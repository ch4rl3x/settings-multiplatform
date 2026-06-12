package de.charlex.settings.datastore

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFStringRef

@OptIn(ExperimentalForeignApi::class)
typealias KeychainBaseQueryItems = List<Pair<CFStringRef?, Any?>>
@OptIn(ExperimentalForeignApi::class)
typealias KeychainAddQueryItems = List<Pair<CFStringRef?, Any?>>
@OptIn(ExperimentalForeignApi::class)
typealias KeychainReadQueryItems = List<Pair<CFStringRef?, Any?>>

actual class SystemOptions @OptIn(ExperimentalForeignApi::class) constructor(
    val keychainBaseQueryItems: KeychainBaseQueryItems,
    val keychainAddQueryItems: KeychainAddQueryItems,
    val keychainReadQueryItems: KeychainReadQueryItems
)