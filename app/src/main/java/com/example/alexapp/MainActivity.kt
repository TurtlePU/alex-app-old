package com.example.alexapp

import Performance
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.alexapp.components.AuthState
import com.example.alexapp.components.History
import com.example.alexapp.components.PerformanceQueue
import com.example.alexapp.ui.theme.AlexAppTheme
import kotlinx.coroutines.flow.map

@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
  private val dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

  @Composable
  private fun stringPref(key: String, default: String = ""): Pref<String> {
    val pref = stringPreferencesKey(key)
    val value by dataStore.data.map { it[pref] ?: default }.collectAsState(default)
    val setter: suspend (String) -> Unit = { dataStore.edit { settings -> settings[pref] = it } }
    return Pref(value, setter)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AlexAppTheme {
        Surface(color = MaterialTheme.colors.background) {
          AlexScaffold(
            AuthState(
              stringPref("host"),
              stringPref("login"),
              stringPref("token"),
            ),
            History(
              stringPref("history", default = History.minimalJson),
            ),
          )
        }
      }
    }
  }

  @Composable
  private fun AlexScaffold(authState: AuthState, history: History) {
    val queue by rememberSaveable { mutableStateOf(PerformanceQueue()) }
    val transientHistory by rememberSaveable { mutableStateOf(history.deserialized) }
    var selectedPerformance by rememberSaveable { mutableStateOf(null as Performance?) }

    Scaffold(
      drawerContent = { authState.AuthDrawer { /* TODO */ } },
      floatingActionButton = { queue.RefreshButton { emptySequence() /* TODO */ } },
    ) { padding ->
      queue.Column(
        Modifier.padding(padding),
        isNew = { !transientHistory.contains(it) },
        isSelected = { selectedPerformance == it },
        select = { selectedPerformance = it },
      )
    }
    selectedPerformance?.let {
      (transientHistory[it] ?: History.Entry()).GradingPopup(
        onDismiss = { selectedPerformance = null },
        onGrade = { grade, comment ->
          transientHistory[it] = History.Entry(grade, comment)
          /* TODO */
        })
    }
    history.DumpEffect(transientHistory)
  }
}