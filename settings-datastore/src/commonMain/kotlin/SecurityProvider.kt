package de.charlex.settings.datastore

import de.charlex.settings.datastore.security.Security

interface SecurityProvider {
    val security: Security
}
