import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.create
import de.charlex.settings.datastore.encryption.security.AESSecurity

@Composable
actual fun rememberSettingsDataStore(): State<SettingsDataStore> {
    val context = LocalContext.current
    return remember {
        mutableStateOf(SettingsDataStore.create(context))
    }
}

@Composable
actual fun rememberEncryptedSettingsDataStore(): State<SettingsDataStore> {
    val context = LocalContext.current
    return remember {
        mutableStateOf(SettingsDataStore.create(
            context = context,
            security = AESSecurity))
    }
}