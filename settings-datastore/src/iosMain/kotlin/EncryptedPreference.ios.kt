package de.charlex.settings.datastore

import de.charlex.settings.datastore.security.KeychainAccessibility
import de.charlex.settings.datastore.security.KeychainOptions
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFStringRef

/**
 * A list of raw keychain query items. The first value of each pair is a `kSec...` key
 * (e.g. `kSecAttrAccessible`), the second value is the value for that key.
 *
 * Supported value types are [String], [ByteArray], [Boolean] and any already bridged `CFTypeRef`.
 */
@OptIn(ExperimentalForeignApi::class)
typealias KeychainQueryItems = List<Pair<CFStringRef?, Any?>>

/** Items that identify a keychain item and are therefore used by *every* operation. */
typealias KeychainBaseQueryItems = KeychainQueryItems

/** Item attributes that are applied on `SecItemAdd` **and** on `SecItemUpdate`. */
typealias KeychainItemAttributes = KeychainQueryItems

/** Items that are only applied to `SecItemAdd`. */
typealias KeychainAddQueryItems = KeychainQueryItems

/** Items that are only applied to the *search* dictionary of `SecItemUpdate`. */
typealias KeychainUpdateQueryItems = KeychainQueryItems

/** Items that are only applied to the *attributesToUpdate* dictionary of `SecItemUpdate`. */
typealias KeychainUpdateAttributes = KeychainQueryItems

/** Items that are only applied to `SecItemCopyMatching`. */
typealias KeychainReadQueryItems = KeychainQueryItems

/** Items that are only applied to `SecItemDelete`. */
typealias KeychainDeleteQueryItems = KeychainQueryItems

/**
 * iOS specific options for an encrypted preference.
 *
 * The keychain distinguishes between
 * * **search attributes** – used to *find* an item (`kSecClass`, `kSecAttrService`, `kSecAttrAccount`, …)
 * * **item attributes** – describing *how* an item is stored (`kSecAttrAccessible`, `kSecAttrLabel`, …)
 *
 * Item attributes must be supplied on `SecItemAdd` **and** on `SecItemUpdate`, otherwise an already
 * existing item silently keeps its old protection class (e.g. the default
 * `kSecAttrAccessibleWhenUnlocked`). Passing them as *search* attributes on the other hand makes the
 * lookup fail. [KeychainOptions] takes care of routing every item into the correct dictionary.
 *
 * Simplest usage – set the protection class for a preference:
 * ```kotlin
 * val token = encryptedStringPreference(
 *     name = "token",
 *     defaultValue = "",
 *     options = SystemOptions(accessibility = KeychainAccessibility.AfterFirstUnlockThisDeviceOnly)
 * )
 * ```
 */
actual class SystemOptions {

    /** The fully resolved keychain configuration used for every keychain operation. */
    @OptIn(ExperimentalForeignApi::class)
    val keychain: KeychainOptions

    @OptIn(ExperimentalForeignApi::class)
    constructor(keychain: KeychainOptions) {
        this.keychain = keychain
    }

    @OptIn(ExperimentalForeignApi::class)
    constructor(
        keychainBaseQueryItems: KeychainBaseQueryItems = emptyList(),
        keychainAddQueryItems: KeychainAddQueryItems = emptyList(),
        keychainReadQueryItems: KeychainReadQueryItems = emptyList(),
        keychainItemAttributes: KeychainItemAttributes = emptyList(),
        keychainUpdateQueryItems: KeychainUpdateQueryItems = emptyList(),
        keychainUpdateAttributes: KeychainUpdateAttributes = emptyList(),
        keychainDeleteQueryItems: KeychainDeleteQueryItems = emptyList(),
        accessibility: KeychainAccessibility? = null,
        applyUpdatableAttributesOnUpdate: Boolean = true,
    ) : this(
        KeychainOptions(
            baseQueryItems = keychainBaseQueryItems,
            itemAttributes = keychainItemAttributes,
            addQueryItems = keychainAddQueryItems,
            updateQueryItems = keychainUpdateQueryItems,
            updateAttributes = keychainUpdateAttributes,
            readQueryItems = keychainReadQueryItems,
            deleteQueryItems = keychainDeleteQueryItems,
            accessibility = accessibility,
            applyUpdatableAttributesOnUpdate = applyUpdatableAttributesOnUpdate,
        )
    )

    @OptIn(ExperimentalForeignApi::class)
    val keychainBaseQueryItems: KeychainBaseQueryItems get() = keychain.baseQueryItems

    @OptIn(ExperimentalForeignApi::class)
    val keychainItemAttributes: KeychainItemAttributes get() = keychain.itemAttributes

    @OptIn(ExperimentalForeignApi::class)
    val keychainAddQueryItems: KeychainAddQueryItems get() = keychain.addQueryItems

    @OptIn(ExperimentalForeignApi::class)
    val keychainUpdateQueryItems: KeychainUpdateQueryItems get() = keychain.updateQueryItems

    @OptIn(ExperimentalForeignApi::class)
    val keychainUpdateAttributes: KeychainUpdateAttributes get() = keychain.updateAttributes

    @OptIn(ExperimentalForeignApi::class)
    val keychainReadQueryItems: KeychainReadQueryItems get() = keychain.readQueryItems

    @OptIn(ExperimentalForeignApi::class)
    val keychainDeleteQueryItems: KeychainDeleteQueryItems get() = keychain.deleteQueryItems

    @OptIn(ExperimentalForeignApi::class)
    val accessibility: KeychainAccessibility? get() = keychain.accessibility

    @OptIn(ExperimentalForeignApi::class)
    fun copy(keychain: KeychainOptions = this.keychain): SystemOptions = SystemOptions(keychain)

    @OptIn(ExperimentalForeignApi::class)
    override fun equals(other: Any?): Boolean = other is SystemOptions && other.keychain == keychain

    @OptIn(ExperimentalForeignApi::class)
    override fun hashCode(): Int = keychain.hashCode()

    @OptIn(ExperimentalForeignApi::class)
    override fun toString(): String = "SystemOptions(keychain=$keychain)"
}
