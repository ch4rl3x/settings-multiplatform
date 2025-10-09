import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.create

@Composable
actual fun rememberSettingsDataStore(): State<SettingsDataStore> {
    return remember {
        mutableStateOf(SettingsDataStore.create())
    }
    
}