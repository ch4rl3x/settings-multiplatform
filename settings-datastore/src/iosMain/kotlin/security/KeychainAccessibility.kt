package de.charlex.settings.datastore.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFStringRef
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
import platform.Security.kSecAttrAccessibleWhenUnlocked
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly

/**
 * Type safe representation of the `kSecAttrAccessible` keychain attribute (the "protection class").
 *
 * The protection class is an **item attribute**, not a search attribute. It therefore has to be
 * written on `SecItemAdd` *and* on `SecItemUpdate` – otherwise an already existing item silently
 * keeps its previous (possibly more restrictive) protection class.
 *
 * @see KeychainOptions.accessibility
 */
enum class KeychainAccessibility {

    /** `kSecAttrAccessibleWhenUnlocked` – readable only while the device is unlocked. Migrates to a new device via backup. */
    WhenUnlocked,

    /** `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` – like [WhenUnlocked] but never leaves the device. */
    WhenUnlockedThisDeviceOnly,

    /** `kSecAttrAccessibleAfterFirstUnlock` – readable after the first unlock following a reboot (works in background). */
    AfterFirstUnlock,

    /** `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` – like [AfterFirstUnlock] but never leaves the device. */
    AfterFirstUnlockThisDeviceOnly,

    /** `kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly` – requires a device passcode, never leaves the device. */
    WhenPasscodeSetThisDeviceOnly;

    /** The matching `kSecAttrAccessible*` Core Foundation constant. */
    @OptIn(ExperimentalForeignApi::class)
    val value: CFStringRef?
        get() = when (this) {
            WhenUnlocked -> kSecAttrAccessibleWhenUnlocked
            WhenUnlockedThisDeviceOnly -> kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            AfterFirstUnlock -> kSecAttrAccessibleAfterFirstUnlock
            AfterFirstUnlockThisDeviceOnly -> kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            WhenPasscodeSetThisDeviceOnly -> kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
        }
}

