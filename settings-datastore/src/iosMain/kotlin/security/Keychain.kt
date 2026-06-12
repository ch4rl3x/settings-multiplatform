package de.charlex.settings.datastore.security

import de.charlex.settings.datastore.KeychainAddQueryItems
import de.charlex.settings.datastore.KeychainBaseQueryItems
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
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
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
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.noErr
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
data class Keychain(
    val appGroup: String? = null,
    val service: String
) {

    fun store(
        key: String,
        value: ByteArray,
        keychainBaseQueryItems: KeychainBaseQueryItems = emptyList(),
        keychainAddQueryItems: KeychainAddQueryItems = emptyList()
    ): String {
        require(value.isNotEmpty()) { "value must not be empty" }

        val status: Int = addOrUpdate(
            key = key,
            value = value,
            keychainBaseQueryItems = keychainBaseQueryItems,
            keychainAddQueryItems = keychainAddQueryItems
        )
        if (status.toUInt() == noErr) {
            return key
        }

        error("Keychain store failed (cause=${
            (CFBridgingRelease(SecCopyErrorMessageString(status, null)) as? NSString)?.toString() ?: "Keychain error: $status" })")
    }

    fun load(
        key: String,
        keychainBaseQueryItems: KeychainBaseQueryItems = emptyList(),
        keychainReadQueryItems: KeychainReadQueryItems = emptyList(),
    ): ByteArray? {
        return loadValue(
            key = key,
            keychainBaseQueryItems = keychainBaseQueryItems,
            keychainReadQueryItems = keychainReadQueryItems
        )
    }

    fun delete(
        key: String,
        keychainBaseQueryItems: KeychainBaseQueryItems = emptyList()
    ): Boolean {
        return deleteValue(
            key = key,
            keychainBaseQueryItems = keychainBaseQueryItems
        )
    }

    fun clearAll(): Boolean {
        val queryArgs = mutableListOf<Pair<CFStringRef?, Any?>>()
        queryArgs.add(kSecClass to kSecClassGenericPassword)
        queryArgs.add(kSecAttrService to service)
        appGroup?.let {
            queryArgs.add(kSecAttrAccessGroup to it)
        }
        val query = createQuery(*queryArgs.toTypedArray())

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

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun addOrUpdate(
        key: String,
        value: ByteArray,
        keychainBaseQueryItems: KeychainBaseQueryItems,
        keychainAddQueryItems: KeychainAddQueryItems
    ): Int {
        val data = value.toNSData()

        // Versuch: neues Item anlegen
        val addArgs = mutableListOf<Pair<CFStringRef?, Any?>>()
        addArgs.add(kSecClass to kSecClassGenericPassword)
        addArgs.add(kSecAttrService to service)
        appGroup?.let {
            addArgs.add(kSecAttrAccessGroup to it)
        }
        addArgs.add(kSecAttrAccount to key)
        addArgs.add(kSecValueData to value)
        addArgs.addAll(keychainBaseQueryItems)
        addArgs.addAll(keychainAddQueryItems)
        val addQuery = createQuery(*addArgs.toTypedArray())

        val addStatus = SecItemAdd(addQuery, null)
        if (addStatus != errSecDuplicateItem) {
            return addStatus
        }

        val updateArgs = mutableListOf<Pair<CFStringRef?, Any?>>()
        updateArgs.add(kSecClass to kSecClassGenericPassword)
        updateArgs.add(kSecAttrService to service)
        appGroup?.let {
            updateArgs.add(kSecAttrAccessGroup to it)
        }
        updateArgs.add(kSecAttrAccount to key)
        updateArgs.addAll(keychainBaseQueryItems)

        val updateQuery = createQuery(*updateArgs.toTypedArray())

        val attributesToUpdate = createQuery(
            kSecValueData to value,
        )

        return SecItemUpdate(updateQuery, attributesToUpdate)
    }

    @OptIn(BetaInteropApi::class)
    private fun loadValue(
        key: String,
        keychainBaseQueryItems: KeychainBaseQueryItems,
        keychainReadQueryItems: KeychainReadQueryItems
    ): ByteArray? = autoreleasepool {
        memScoped {
            val args = mutableListOf<Pair<CFStringRef?, Any?>>()
            args.add(kSecClass to kSecClassGenericPassword)
            args.add(kSecAttrService to service)
            appGroup?.let {
                args.add(kSecAttrAccessGroup to it)
            }
            args.add(kSecAttrAccount to key)
            args.add(kSecReturnData to kCFBooleanTrue)
            args.addAll(keychainBaseQueryItems)
            args.addAll(keychainReadQueryItems)

            val query = createQuery(*args.toTypedArray())

            val resultRef = alloc<CFTypeRefVar>()
            return when (val status = SecItemCopyMatching(query, resultRef.ptr)) {
                errSecSuccess -> {
                    val data =
                        resultRef.value?.let { CFBridgingRelease(it) } ?: return null
                    (data as NSData).toByteArray()
                }

                errSecItemNotFound -> null
                else -> error("Keychain load failed (status=$status)")
            }
        }
    }

    private fun deleteValue(
        key: String,
        keychainBaseQueryItems: KeychainBaseQueryItems
    ): Boolean {
        val args = mutableListOf<Pair<CFStringRef?, Any?>>()
        args.add(kSecClass to kSecClassGenericPassword)
        args.add(kSecAttrService to service)
        appGroup?.let {
            args.add(kSecAttrAccessGroup to it)
        }
        args.add(kSecAttrAccount to key)
        args.addAll(keychainBaseQueryItems)

        val query = createQuery(*args.toTypedArray())

        val status = SecItemDelete(query)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    @Suppress("UNCHECKED_CAST")
    private fun createQuery(
        vararg pairs: Pair<CFStringRef?, Any?>
    ): CFDictionaryRef? {
        val validPairs = pairs.filter { it.first != null && it.second != null }
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

        val objCObject = when (this) {
            is String -> NSString.create(string = this)
            is ByteArray -> this.toNSData()
            else -> this
        }

        val retained = CFBridgingRetain(objCObject)
        CFAutorelease(retained)
        return retained
    }
}