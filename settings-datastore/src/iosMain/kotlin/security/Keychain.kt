package de.charlex.settings.datastore.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
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

    fun store(key: String?, value: ByteArray): String? {
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

    fun load(key: String?): ByteArray? {
        return loadValue(key)
    }

    fun delete(key: String?) {
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

    @OptIn(ExperimentalForeignApi::class)
    fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun addOrUpdate(key: String?, value: ByteArray): Int {
        val data = value.toNSData()

        return retainedScope(service, key, data, appGroup) { (retainedService, retainedKey, retainedData, retainedAccessGroup) ->
            // Versuch: neues Item anlegen
            val addArgs = mutableListOf<Pair<CFStringRef?, CFTypeRef?>>()
            addArgs.add(kSecClass to kSecClassGenericPassword)
            addArgs.add(kSecAttrService to retainedService)
            retainedAccessGroup?.let {
                addArgs.add(kSecAttrAccessGroup to it)
            }
            addArgs.add(kSecAttrAccount to retainedKey)
            addArgs.add(kSecValueData to retainedData)
            val addQuery = queryWithBridgingScope(*addArgs.toTypedArray())

            val addStatus = SecItemAdd(addQuery, null)
            if (addStatus != errSecDuplicateItem) {
                return@retainedScope addStatus
            }

            val updateArgs = mutableListOf<Pair<CFStringRef?, CFTypeRef?>>()
            updateArgs.add(kSecClass to kSecClassGenericPassword)
            updateArgs.add(kSecAttrService to retainedService)
            retainedAccessGroup?.let {
                updateArgs.add(kSecAttrAccessGroup to it)
            }
            updateArgs.add(kSecAttrAccount to retainedKey)

            val updateQuery = queryWithBridgingScope(*updateArgs.toTypedArray())

            val attributesToUpdate = queryWithBridgingScope(
                kSecValueData to retainedData,
            )

            SecItemUpdate(updateQuery, attributesToUpdate)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun loadValue(key: String?): ByteArray? = memScoped {
        //FIXME ByteArray

        return retainedScope(service, key, appGroup) { (retainedService, retainedKey, retainedAccessGroup) ->
            val args = mutableListOf<Pair<CFStringRef?, CFTypeRef?>>()
            args.add(kSecClass to kSecClassGenericPassword)
            args.add(kSecAttrService to retainedService)
            retainedAccessGroup?.let {
                args.add(kSecAttrAccessGroup to it)
            }
            args.add(kSecAttrAccount to retainedKey)
            args.add(kSecReturnData to kCFBooleanTrue)

            val query = queryWithBridgingScope(*args.toTypedArray())

            val resultRef = alloc<CFTypeRefVar>()
            when (val status = SecItemCopyMatching(query, resultRef.ptr)) {
                errSecSuccess -> {
                    val data =
                        resultRef.value?.let { CFBridgingRelease(it) } ?: return@retainedScope null
                    (data as NSData).toByteArray()
                }

                errSecItemNotFound -> null
                else -> error("Keychain load failed (status=$status)")
            }
        }
    }

    private fun deleteValue(key: String?): Boolean {
        return retainedScope(service, key, appGroup) { (retainedService, retainedKey, retainedAccessGroup) ->
            val args = mutableListOf<Pair<CFStringRef?, CFTypeRef?>>()
            args.add(kSecClass to kSecClassGenericPassword)
            args.add(kSecAttrService to retainedService)
            retainedAccessGroup?.let {
                args.add(kSecAttrAccessGroup to it)
            }
            args.add(kSecAttrAccount to retainedKey)

            val query = queryWithBridgingScope(*args.toTypedArray())

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