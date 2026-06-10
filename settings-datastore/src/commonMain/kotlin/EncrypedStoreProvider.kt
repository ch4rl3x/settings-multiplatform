package de.charlex.settings.datastore

import de.charlex.settings.datastore.security.EncryptedStore

interface EncrypedStoreProvider {
    val encryptedStore: EncryptedStore
}
