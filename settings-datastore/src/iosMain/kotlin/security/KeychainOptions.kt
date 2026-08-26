package de.charlex.settings.datastore.security

import de.charlex.settings.datastore.KeychainAddQueryItems
import de.charlex.settings.datastore.KeychainBaseQueryItems
import de.charlex.settings.datastore.KeychainDeleteQueryItems
import de.charlex.settings.datastore.KeychainItemAttributes
import de.charlex.settings.datastore.KeychainQueryItems
import de.charlex.settings.datastore.KeychainReadQueryItems
import de.charlex.settings.datastore.KeychainUpdateAttributes
import de.charlex.settings.datastore.KeychainUpdateQueryItems
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFStringRef
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrComment
import platform.Security.kSecAttrCreator
import platform.Security.kSecAttrDescription
import platform.Security.kSecAttrGeneric
import platform.Security.kSecAttrIsInvisible
import platform.Security.kSecAttrIsNegative
import platform.Security.kSecAttrLabel
import platform.Security.kSecAttrType

/**
 * Well known keychain attribute keys grouped by the role they play inside a keychain call.
 */
@OptIn(ExperimentalForeignApi::class)
object KeychainAttributeKeys {

    /**
     * Attributes that describe *how* an item is protected. They must never be part of a search
     * dictionary (they would filter the lookup) and have to be written on add **and** on update.
     */
    val protection: Set<CFStringRef> = setOfNotNull(
        kSecAttrAccessible,
        kSecAttrAccessControl,
    )

    /**
     * Attributes that `SecItemUpdate` accepts inside its `attributesToUpdate` dictionary.
     *
     * Identity attributes (`kSecClass`, `kSecAttrService`, `kSecAttrAccount`, `kSecAttrAccessGroup`)
     * and immutable attributes (e.g. `kSecAttrSynchronizable`) are deliberately **not** part of this
     * set – updating them would either rename/move the item or fail with `errSecParam`.
     */
    val updatable: Set<CFStringRef> = protection + setOfNotNull(
        kSecAttrLabel,
        kSecAttrDescription,
        kSecAttrComment,
        kSecAttrCreator,
        kSecAttrType,
        kSecAttrIsInvisible,
        kSecAttrIsNegative,
        kSecAttrGeneric,
    )
}

/**
 * Fully describes how a single keychain item is looked up, created, updated, read and deleted.
 *
 * ### Why this exists
 * The Security framework uses the same dictionary shape for very different purposes:
 *
 * | Call | Dictionary | Contains |
 * |---|---|---|
 * | `SecItemAdd` | `attributes` | identity **+ item attributes + value** |
 * | `SecItemUpdate` | `query` | identity **only** |
 * | `SecItemUpdate` | `attributesToUpdate` | item attributes **+ value** |
 * | `SecItemCopyMatching` | `query` | identity + return/match options |
 * | `SecItemDelete` | `query` | identity |
 *
 * Putting `kSecAttrAccessible` only into the *add* dictionary (as the previous API forced you to)
 * means an already existing item keeps its old protection class forever, because `SecItemUpdate`
 * never receives it. Putting it into the *base* dictionary is equally wrong, because it then
 * filters every lookup and updates/reads start failing with `errSecItemNotFound`.
 *
 * [KeychainOptions] fixes this by separating **identity** from **item attributes**:
 * * [baseQueryItems] – identity, used by every call
 * * [itemAttributes] / [accessibility] – written on add **and** update
 * * [addQueryItems], [updateQueryItems], [updateAttributes], [readQueryItems], [deleteQueryItems] – call specific escape hatches
 *
 * @property baseQueryItems Identity/search attributes applied to every keychain call.
 * @property itemAttributes Item attributes applied on `SecItemAdd` and on `SecItemUpdate`.
 * @property addQueryItems Additional items only applied to `SecItemAdd`.
 * @property updateQueryItems Additional items only applied to the *search* dictionary of `SecItemUpdate`.
 * @property updateAttributes Additional items only applied to the *attributesToUpdate* dictionary of `SecItemUpdate`.
 * @property readQueryItems Additional items only applied to `SecItemCopyMatching`.
 * @property deleteQueryItems Additional items only applied to `SecItemDelete`.
 * @property accessibility Convenience for `kSecAttrAccessible`; wins over a manually supplied value.
 * @property applyUpdatableAttributesOnUpdate When `true` (default) every *non protection* attribute
 * from [addQueryItems] that `SecItemUpdate` accepts (see [KeychainAttributeKeys.updatable], e.g.
 * `kSecAttrLabel`) is written on update as well. Set to `false` to only update what you configured
 * explicitly. Protection attributes (`kSecAttrAccessible`, `kSecAttrAccessControl`) are **always**
 * routed to the add *and* update dictionaries, no matter in which list they were configured – this
 * is what keeps existing code that put `kSecAttrAccessible` into [addQueryItems] working.
 */
@OptIn(ExperimentalForeignApi::class)
data class KeychainOptions(
    val baseQueryItems: KeychainBaseQueryItems = emptyList(),
    val itemAttributes: KeychainItemAttributes = emptyList(),
    val addQueryItems: KeychainAddQueryItems = emptyList(),
    val updateQueryItems: KeychainUpdateQueryItems = emptyList(),
    val updateAttributes: KeychainUpdateAttributes = emptyList(),
    val readQueryItems: KeychainReadQueryItems = emptyList(),
    val deleteQueryItems: KeychainDeleteQueryItems = emptyList(),
    val accessibility: KeychainAccessibility? = null,
    val applyUpdatableAttributesOnUpdate: Boolean = true,
) {

    companion object {

        /** Keychain defaults – no extra attributes, protection class stays at the system default. */
        val Default: KeychainOptions = KeychainOptions()

        /** Shorthand for `KeychainOptions(accessibility = accessibility)`. */
        fun accessible(accessibility: KeychainAccessibility): KeychainOptions =
            KeychainOptions(accessibility = accessibility)
    }

    /**
     * Merges [other] into this configuration. List based items are concatenated, [other] wins for
     * duplicate keys and for [accessibility] / [applyUpdatableAttributesOnUpdate].
     */
    fun merge(other: KeychainOptions?): KeychainOptions {
        if (other == null) return this
        return KeychainOptions(
            baseQueryItems = baseQueryItems + other.baseQueryItems,
            itemAttributes = itemAttributes + other.itemAttributes,
            addQueryItems = addQueryItems + other.addQueryItems,
            updateQueryItems = updateQueryItems + other.updateQueryItems,
            updateAttributes = updateAttributes + other.updateAttributes,
            readQueryItems = readQueryItems + other.readQueryItems,
            deleteQueryItems = deleteQueryItems + other.deleteQueryItems,
            accessibility = other.accessibility ?: accessibility,
            applyUpdatableAttributesOnUpdate = other.applyUpdatableAttributesOnUpdate,
        )
    }

    /**
     * Protection attributes that were (incorrectly, but historically) placed inside
     * [baseQueryItems] or [addQueryItems]. They are picked up here so they end up in the right
     * dictionaries no matter how they were configured.
     */
    private val legacyProtectionItems: KeychainQueryItems
        get() = baseQueryItems.protectionItems() + addQueryItems.protectionItems()

    /**
     * Every item attribute that has to be written on add **and** on update.
     *
     * Precedence (last one wins): legacy protection items → [itemAttributes] → [accessibility].
     */
    val resolvedItemAttributes: KeychainQueryItems
        get() = buildList {
            addAll(legacyProtectionItems)
            addAll(itemAttributes)
            accessibility?.let { add(kSecAttrAccessible to it.value) }
        }

    /** Identity items usable inside any search dictionary (protection attributes removed). */
    internal val resolvedSearchItems: KeychainQueryItems
        get() = baseQueryItems.withoutProtectionItems()

    /** Items for the `SecItemAdd` dictionary (without identity and value, those are added by [Keychain]). */
    internal val resolvedAddItems: KeychainQueryItems
        get() = buildList {
            addAll(legacyProtectionItems)
            addAll(itemAttributes)
            addAll(addQueryItems.withoutProtectionItems())
            accessibility?.let { add(kSecAttrAccessible to it.value) }
        }

    /** Items for the *search* dictionary of `SecItemUpdate`. */
    internal val resolvedUpdateQueryItems: KeychainQueryItems
        get() = resolvedSearchItems + updateQueryItems.withoutProtectionItems()

    /**
     * Items for the *attributesToUpdate* dictionary of `SecItemUpdate`.
     *
     * This is the dictionary that used to be missing every item attribute, which made an existing
     * item keep its previous protection class after a value update.
     */
    internal val resolvedUpdateAttributes: KeychainQueryItems
        get() = buildList {
            addAll(legacyProtectionItems)
            addAll(itemAttributes.updatableItems())
            if (applyUpdatableAttributesOnUpdate) {
                addAll(addQueryItems.updatableItems().withoutProtectionItems())
            }
            accessibility?.let { add(kSecAttrAccessible to it.value) }
            addAll(updateAttributes)
        }

    /** Items for the `SecItemCopyMatching` dictionary. */
    internal val resolvedReadItems: KeychainQueryItems
        get() = resolvedSearchItems + readQueryItems.withoutProtectionItems()

    /** Items for the `SecItemDelete` dictionary. */
    internal val resolvedDeleteItems: KeychainQueryItems
        get() = resolvedSearchItems + deleteQueryItems.withoutProtectionItems()
}

@OptIn(ExperimentalForeignApi::class)
private fun KeychainQueryItems.protectionItems(): KeychainQueryItems =
    filter { (key, _) -> key != null && key in KeychainAttributeKeys.protection }

@OptIn(ExperimentalForeignApi::class)
private fun KeychainQueryItems.withoutProtectionItems(): KeychainQueryItems =
    filterNot { (key, _) -> key != null && key in KeychainAttributeKeys.protection }

@OptIn(ExperimentalForeignApi::class)
private fun KeychainQueryItems.updatableItems(): KeychainQueryItems =
    filter { (key, _) -> key != null && key in KeychainAttributeKeys.updatable }





