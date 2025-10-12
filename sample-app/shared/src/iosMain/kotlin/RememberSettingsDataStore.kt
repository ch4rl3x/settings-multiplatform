import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.create
import de.charlex.settings.datastore.encryption.security.AESSecurity

@Composable
actual fun rememberSettingsDataStore(): State<SettingsDataStore> {
    return remember {
        mutableStateOf(SettingsDataStore.create())
    }
}

@Composable
actual fun rememberEncryptedSettingsDataStore(): State<SettingsDataStore> {
    return remember {
        mutableStateOf(SettingsDataStore.create(
            security = AESSecurity
        ))
    }
}