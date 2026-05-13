package de.charlex.settings.datastore.encryption.security

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
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecCopyErrorMessageString
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
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
data class KeychainHelper(
    val service: String = NSBundle.mainBundle.bundleIdentifier
        ?.let { "$it.settings.datastore.keychain" }
        ?: "de.charlex.settings.datastore.keychain"
) {

    fun storePlainText(key: String?, value: String): String? {
        require(value.isNotEmpty()) { "value must not be empty" }

        val status: Int = addOrUpdate(key, value)
        if (status.toUInt() == noErr) {
            return key
        }

        error("Keychain store failed (cause=${
            CFBridgingRelease(
                SecCopyErrorMessageString(status, null)
            ) as? String ?: "Keychain error: $status" })")
    }

    fun loadPlainText(key: String?): String? {
        return loadValue(key)
    }

    fun deletePlainText(key: String?) {
        deleteValue(key)
    }

    fun clearAll() {
        retainedScope(service) { (retainedService) ->
            val query = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
            )

            SecItemDelete(query)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun addOrUpdate(key: String?, plain: String): Int {
        val ns = plain as NSString
        val data = ns.dataUsingEncoding(NSUTF8StringEncoding) ?: return errSecParam

        return retainedScope(service, key, data) { (retainedService, retainedKey, retainedData) ->
            // Versuch: neues Item anlegen
            val addQuery = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedKey,
                kSecValueData to retainedData,
            )

            val addStatus = SecItemAdd(addQuery, null)
            if (addStatus != errSecDuplicateItem) {
                return@retainedScope addStatus
            }

            val updateQuery = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedKey,
            )
            val attributesToUpdate = queryWithBridgingScope(
                kSecValueData to retainedData,
            )

            SecItemUpdate(updateQuery, attributesToUpdate)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun loadValue(key: String?): String? = memScoped {
        return retainedScope(service, key) { (retainedService, retainedKey) ->
            val query = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedKey,
                kSecReturnData to kCFBooleanTrue,
            )

            val resultRef = alloc<CFTypeRefVar>()
            when (val status = SecItemCopyMatching(query, resultRef.ptr)) {
                errSecSuccess -> {
                    val data =
                        resultRef.value?.let { CFBridgingRelease(it) } ?: return@retainedScope null
                    NSString.create(
                        data = data as NSData,
                        encoding = NSUTF8StringEncoding
                    ) as String
                }

                errSecItemNotFound -> null
                else -> error("Keychain load failed (status=$status)")
            }
        }
    }

    private fun deleteValue(key: String?): Boolean {
        return retainedScope(service, key) { (retainedService, retainedKey) ->
            val query = queryWithBridgingScope(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to retainedService,
                kSecAttrAccount to retainedKey,
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
    }

    private fun <T> retainedScope(
        vararg values: Any?,
        block: BridgingScope.(List<CFTypeRef?>) -> T,
    ): T {
        val retainedValues = values.map(::CFBridgingRetain)
        return try {
            val context = BridgingScope(emptyMap())
            block.invoke(context, retainedValues)
        } finally {
            retainedValues.forEach(::CFBridgingRelease)
        }
    }
}