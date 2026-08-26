package de.charlex.settings.datastore.security

import de.charlex.settings.datastore.KeychainAddQueryItems
import de.charlex.settings.datastore.KeychainBaseQueryItems
import de.charlex.settings.datastore.KeychainQueryItems
import de.charlex.settings.datastore.KeychainReadQueryItems
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFAutorelease
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Security.SecCopyErrorMessageString
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessGroup
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * Thin, correctness focused wrapper around the iOS Security framework for generic password items.
 *
 * All operations are configured through [KeychainOptions], which makes sure that
 * * identity attributes only ever end up in *search* dictionaries and
 * * item attributes such as `kSecAttrAccessible` are written on `SecItemAdd` **and** `SecItemUpdate`.
 *
 * @param appGroup Optional keychain access group (`kSecAttrAccessGroup`) for app group sharing.
 * @param service The `kSecAttrService` all items of this keychain belong to.
 * @param defaultOptions Options applied to every operation. Per call options are merged on top of
 * these, so a store wide protection class can be configured once.
 */
@OptIn(ExperimentalForeignApi::class)
data class Keychain(
    val appGroup: String? = null,
    val service: String,
    val defaultOptions: KeychainOptions = KeychainOptions.Default,
) {

    /**
     * Creates the item if it does not exist yet, otherwise updates its value **and** its item
     * attributes (protection class, label, …).
     */
    fun store(
        key: String,
        value: ByteArray,
        options: KeychainOptions = KeychainOptions.Default,
    ): String {
        require(value.isNotEmpty()) { "value must not be empty" }

        val resolved = defaultOptions.merge(options)
        val status = addOrUpdate(key = key, value = value, options = resolved)
        if (status == errSecSuccess) {
            return key
        }
        error("Keychain store failed for '$key' (${describe(status)})")
    }

    @Deprecated(
        "Item attributes such as kSecAttrAccessible must also be applied on update. Use the KeychainOptions based overload.",
        ReplaceWith(
            "store(key, value, KeychainOptions(baseQueryItems = keychainBaseQueryItems, addQueryItems = keychainAddQueryItems))",
            "de.charlex.settings.datastore.security.KeychainOptions",
        ),
    )
    fun store(
        key: String,
        value: ByteArray,
        keychainBaseQueryItems: KeychainBaseQueryItems,
        keychainAddQueryItems: KeychainAddQueryItems = emptyList(),
    ): String = store(
        key = key,
        value = value,
        options = KeychainOptions(
            baseQueryItems = keychainBaseQueryItems,
            addQueryItems = keychainAddQueryItems,
        ),
    )

    /** Reads the raw value of [key] or `null` when the item does not exist. */
    fun load(
        key: String,
        options: KeychainOptions = KeychainOptions.Default,
    ): ByteArray? = loadValue(key = key, options = defaultOptions.merge(options))

    @Deprecated(
        "Use the KeychainOptions based overload.",
        ReplaceWith(
            "load(key, KeychainOptions(baseQueryItems = keychainBaseQueryItems, readQueryItems = keychainReadQueryItems))",
            "de.charlex.settings.datastore.security.KeychainOptions",
        ),
    )
    fun load(
        key: String,
        keychainBaseQueryItems: KeychainBaseQueryItems,
        keychainReadQueryItems: KeychainReadQueryItems = emptyList(),
    ): ByteArray? = load(
        key = key,
        options = KeychainOptions(
            baseQueryItems = keychainBaseQueryItems,
            readQueryItems = keychainReadQueryItems,
        ),
    )

    /** Deletes [key]. Returns `true` when the item was removed or did not exist. */
    fun delete(
        key: String,
        options: KeychainOptions = KeychainOptions.Default,
    ): Boolean {
        val resolved = defaultOptions.merge(options)
        val query = createQuery(identityItems(key) + resolved.resolvedDeleteItems)
        val status = SecItemDelete(query)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    @Deprecated(
        "Use the KeychainOptions based overload.",
        ReplaceWith(
            "delete(key, KeychainOptions(baseQueryItems = keychainBaseQueryItems))",
            "de.charlex.settings.datastore.security.KeychainOptions",
        ),
    )
    fun delete(
        key: String,
        keychainBaseQueryItems: KeychainBaseQueryItems,
    ): Boolean = delete(key = key, options = KeychainOptions(baseQueryItems = keychainBaseQueryItems))

    /** Deletes every item of this [service] (and [appGroup]). */
    fun clearAll(options: KeychainOptions = KeychainOptions.Default): Boolean {
        val resolved = defaultOptions.merge(options)
        val query = createQuery(serviceItems() + resolved.resolvedDeleteItems)
        val status = SecItemDelete(query)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    @OptIn(ExperimentalForeignApi::class)
    fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = this@toNSData.size.toULong()
        )
    }

    // region internals

    /** `kSecClass` + `kSecAttrService` (+ `kSecAttrAccessGroup`) – identifies this keychain. */
    private fun serviceItems(): KeychainQueryItems = buildList {
        add(kSecClass to kSecClassGenericPassword)
        add(kSecAttrService to service)
        appGroup?.let { add(kSecAttrAccessGroup to it) }
    }

    /** [serviceItems] + `kSecAttrAccount` – identifies a single item. */
    private fun identityItems(key: String): KeychainQueryItems =
        serviceItems() + listOf(kSecAttrAccount to key)

    private fun addOrUpdate(
        key: String,
        value: ByteArray,
        options: KeychainOptions,
    ): Int {
        // 1) try to create the item including all item attributes (protection class, label, ...)
        val addQuery = createQuery(
            identityItems(key) + options.resolvedAddItems + listOf(kSecValueData to value)
        )

        val addStatus = SecItemAdd(addQuery, null)
        if (addStatus != errSecDuplicateItem) {
            return addStatus
        }

        // 2) the item already exists -> the *search* dictionary must only contain identity items,
        //    while the item attributes have to be written through `attributesToUpdate`. Otherwise
        //    an existing item would keep its previous protection class forever.
        val updateQuery = createQuery(identityItems(key) + options.resolvedUpdateQueryItems)
        val attributesToUpdate = createQuery(
            options.resolvedUpdateAttributes + listOf(kSecValueData to value)
        )

        return SecItemUpdate(updateQuery, attributesToUpdate)
    }

    @OptIn(BetaInteropApi::class)
    private fun loadValue(
        key: String,
        options: KeychainOptions,
    ): ByteArray? = autoreleasepool {
        memScoped {
            val query = createQuery(
                identityItems(key) + options.resolvedReadItems + listOf(
                    kSecReturnData to kCFBooleanTrue,
                    kSecMatchLimit to kSecMatchLimitOne,
                )
            )

            val resultRef = alloc<CFTypeRefVar>()
            return when (val status = SecItemCopyMatching(query, resultRef.ptr)) {
                errSecSuccess -> {
                    val data = resultRef.value?.let { CFBridgingRelease(it) } ?: return null
                    (data as NSData).toByteArray()
                }

                errSecItemNotFound -> null
                else -> error("Keychain load failed for '$key' (${describe(status)})")
            }
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun describe(status: Int): String {
        val message = (CFBridgingRelease(SecCopyErrorMessageString(status, null)) as? NSString)?.toString()
        return if (message != null) "status=$status, cause=$message" else "status=$status"
    }

    /**
     * Builds a `CFDictionary`. `null` keys/values are dropped and duplicate keys are collapsed
     * (the last occurrence wins) – `CFDictionaryCreate` does not tolerate duplicate keys.
     */
    @Suppress("UNCHECKED_CAST")
    private fun createQuery(items: KeychainQueryItems): CFDictionaryRef? {
        val validPairs = items
            .mapNotNull { (queryKey, queryValue) ->
                if (queryKey != null && queryValue != null) queryKey to queryValue else null
            }
            .toMap()
            .toList()
        val size = validPairs.size

        return memScoped {
            val keys = allocArray<CFTypeRefVar>(size)
            val values = allocArray<CFTypeRefVar>(size)

            validPairs.forEachIndexed { index, pair ->
                keys[index] = pair.first
                values[index] = pair.second.toCFTypeRef()
            }

            val dict = CFDictionaryCreate(
                allocator = null,
                keys = keys.reinterpret(),
                values = values.reinterpret(),
                numValues = size.convert(),
                keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
                valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr
            )

            CFAutorelease(dict) as CFDictionaryRef?
        }
    }

    @OptIn(BetaInteropApi::class)
    private fun Any?.toCFTypeRef(): CFTypeRef? {
        if (this == null) return null

        if (this is kotlinx.cinterop.CPointer<*>) {
            return this
        }

        if (this is Boolean) {
            return if (this) kCFBooleanTrue else kCFBooleanFalse
        }

        val objCObject = when (this) {
            is String -> NSString.create(string = this)
            is ByteArray -> this.toNSData()
            else -> this
        }

        val retained = CFBridgingRetain(objCObject)
        CFAutorelease(retained)
        return retained
    }

    // endregion
}




