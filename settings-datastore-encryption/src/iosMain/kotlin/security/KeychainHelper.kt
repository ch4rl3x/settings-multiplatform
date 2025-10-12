package de.charlex.settings.datastore.encryption.security

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFAutorelease
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecCopyErrorMessageString
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecParam
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.noErr

@OptIn(ExperimentalForeignApi::class)
internal object KeychainHelper {
    private const val valueServiceString = "de.charlex.settings.datastore.keychain"
    internal const val aliasPrefix = "KC$"

    // ---- Public API ----

    /** Generate and store a plaintext value, returning the alias stored in the DataStore. */
    fun storePlainText(existingAlias: String?, value: String): String {
        require(value.isNotEmpty()) { "value must not be empty" }

        repeat(5) { attempt ->
            val newAlias = generateAlias()
            val status: Int = addOrUpdate(existingAlias, newAlias, value)
            when {
                status.toUInt() == noErr -> return newAlias
                status == errSecDuplicateItem -> { /* retry with new alias */ }
                else -> error("Keychain add failed (cause=${
                    CFBridgingRelease(
                        SecCopyErrorMessageString(status, null)
                    ) as? String ?: "Keychain error: $status" }, attempt=$attempt)")
            }
        }
        error("Unable to generate unique keychain alias after retries")
    }

    /** Load plaintext for a previously stored alias; returns null if alias invalid or item not found. */
    fun loadPlainText(alias: String): String? {
        if (!isAlias(alias)) return null
        return loadValue(alias)
    }

    /** Delete previously stored alias (no error if missing / invalid). */
    fun deletePlainText(alias: String) {
        if (!isAlias(alias)) return
        deleteValue(alias)
    }

    fun clearAll() {
        retainedScope(valueServiceString) { (retainedService) ->
            val query = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
            )

            SecItemDelete(query)
        }
    }

    // ---- Helpers ----

    private fun isAlias(value: String): Boolean = value.startsWith(aliasPrefix) && value.length > aliasPrefix.length
    private fun generateAlias(): String = aliasPrefix + NSUUID().UUIDString.lowercase()

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun addOrUpdate(existingAlias: String?, alias: String, plain: String): Int {
        val ns = plain as NSString
        val data = ns.dataUsingEncoding(NSUTF8StringEncoding) ?: return errSecParam

        return retainedScope(valueServiceString, alias, data) { (retainedService, retainedAlias, retainedData) ->
            if(existingAlias != null && existingAlias != alias && isAlias(existingAlias)) {
                // Vorherigen Eintrag löschen, wenn Alias geändert
                deleteValue(existingAlias)
            }

            // Versuch: neues Item anlegen
            val addQuery = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedAlias,
                kSecValueData to retainedData,
            )

            SecItemAdd(addQuery, null)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun loadValue(alias: String): String? = memScoped {
        return retainedScope(valueServiceString, alias) { (retainedService, retainedAlias) ->
            val query = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedAlias,
                kSecReturnData to kCFBooleanTrue,
            )

            val resultRef = alloc<CFTypeRefVar>()
            when (val status = SecItemCopyMatching(query, resultRef.ptr)) {
                errSecSuccess -> {
                    val data =
                        resultRef.value?.let { CFBridgingRelease(it) } ?: return@retainedScope null
                    NSString.Companion.create(
                        data = data as NSData,
                        encoding = NSUTF8StringEncoding
                    ) as String
                }

                errSecItemNotFound -> null
                else -> error("Keychain load failed (status=$status)")
            }
        }
    }

    private fun deleteValue(alias: String): Boolean {
        return retainedScope(valueServiceString, alias) { (retainedService, retainedAlias) ->
            val query = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedAlias,
            )

            val status = SecItemDelete(query)
            return@retainedScope status.toUInt() == noErr
        }
    }

    private class BridgingScope(
        val refs: Map<CFStringRef?, CFTypeRef?>,
    ) {
        fun queryWithBridgingScope(
            vararg pairs: Pair<CFStringRef?, CFTypeRef?>,
        ) = run {
            val finalPairs = refs.entries
                .map { it.toPair() }
                .toTypedArray() + pairs
            createQuery(*finalPairs)
        }
    }

    private fun createQuery(
        vararg pairs: Pair<CFStringRef?, CFTypeRef?>,
    ): CFDictionaryRef? {
        val map = mapOf(*pairs)
        val dict = CFDictionaryCreateMutable(
            allocator = null,
            capacity = map.size.convert(),
            null,
            null,
        )
        map.entries.forEach {
            CFDictionaryAddValue(dict, it.key, it.value)
        }
        CFAutorelease(dict)
        return dict
    }

    private fun <T> retainedScope(
        vararg values: Any?,
        block: BridgingScope.(List<CFTypeRef?>) -> T,
    ): T {
        val retainedValues = values.map(::CFBridgingRetain)
        return try {
            val context = BridgingScope(emptyMap<CFStringRef?, COpaquePointer?>())
            block.invoke(context, retainedValues)
        } finally {
            retainedValues.forEach(::CFBridgingRelease)
        }
    }
}