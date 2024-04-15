import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.stringPreference
import kotlinx.coroutines.launch

val namePref = stringPreference("name", "NoName")

@Composable
expect fun rememberSettingsDataStore(): State<SettingsDataStore>

@Composable
fun Screen() {
    val dataStore by rememberSettingsDataStore()
    
    val coroutineScope =  rememberCoroutineScope()
    val nameForCompose by dataStore.get(namePref).collectAsState(namePref.defaultValue)

    LazyColumn {
        item {
            Text("Name: $nameForCompose")
        }

        item {
            Button({
                coroutineScope.launch {
                    dataStore.put(namePref, "FakeName")
                }
            }) {
                Text("Set Name to FakeName")
            }
            Button({
                coroutineScope.launch {
                    dataStore.put(namePref, "UnknownName")
                }
            }) {
                Text("Set Name to UnknownName")
            }

            Button({
                coroutineScope.launch {
                    dataStore.clear()
                }
            }) {
                Text("Clear DataStore")
            }
        }
    }
}