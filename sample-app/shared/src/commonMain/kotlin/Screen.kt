import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.encryption.encryptedStringPreference
import de.charlex.settings.datastore.encryption.get
import de.charlex.settings.datastore.encryption.put
import de.charlex.settings.datastore.stringPreference
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

val namePref = stringPreference("name", "")
val encryptedPref = encryptedStringPreference("encrypted", "")

@Composable
expect fun rememberSettingsDataStore(): State<SettingsDataStore>

@Composable
expect fun rememberEncryptedSettingsDataStore(): State<SettingsDataStore>

@Composable
fun Screen() {
    val dataStore by rememberSettingsDataStore()
    val encryptedDatastore by rememberEncryptedSettingsDataStore()

    val coroutineScope =  rememberCoroutineScope()
    val nameForCompose by dataStore.get(namePref).collectAsState(namePref.defaultValue)
    val encryptedForCompose by dataStore.get(encryptedPref).collectAsState(encryptedPref.defaultValue)

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        verticalArrangement = spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DataStore"
                    )
                    Button({
                        coroutineScope.launch {
                            val value = dataStore.get(namePref).firstOrNull()
                            dataStore.put(namePref, "$value+" )
                        }
                    }) {
                        Text("Add more and more +")
                    }

                    Text("Name: $nameForCompose")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Encrypted DataStore"
                    )
                    Button({
                        coroutineScope.launch {
                            val value = encryptedDatastore.get(encryptedPref).firstOrNull()
                            encryptedDatastore.put(encryptedPref, "$value+" )
                        }
                    }) {
                        Text("Add more and more +")
                    }

                    Text("Encrypted: $encryptedForCompose")
                }
            }
        }

        item {
            Button({
                coroutineScope.launch {
                    dataStore.clear()
                    encryptedDatastore.clear()
                }
            }) {
                Text("Clear both DataStore")
            }
        }
    }
}