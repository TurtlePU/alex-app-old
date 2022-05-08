package com.example.alexapp

import Performance
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alexapp.components.AuthState
import com.example.alexapp.components.History
import com.example.alexapp.components.NetworkState
import com.example.alexapp.components.PerformanceQueue
import com.example.alexapp.ui.theme.AlexAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
  private val dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

  @Composable
  private fun stringPref(key: String, default: String = ""): Pref<String> {
    val pref = stringPreferencesKey(key)
    val value by remember { dataStore.data.map { it[pref] ?: default } }.collectAsState(default)
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
    val networkState: NetworkState = viewModel()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
      composable("auth") {
        val scaffoldState = rememberScaffoldState()
        networkState.showError = { scaffoldState.snackbarHostState.showSnackbar(it) }
        Scaffold(scaffoldState = scaffoldState) { padding ->
          authState.AuthDrawer(
            tryAuth = networkState::auth,
            onSuccess = { navController.navigate("scaffold") },
            modifier = Modifier.padding(padding),
          )
        }
      }
      composable("scaffold") {
        val queue: PerformanceQueue = viewModel()

        val transientHistory by rememberUpdatedState(history.deserialized)
        var selectedPerformance by rememberSaveable { mutableStateOf(null as Performance?) }
        val scaffoldState = rememberScaffoldState()

        LaunchedEffect(Unit) {
          while (true) {
            delay(3 * 60 * 1000)
            history.serialize(transientHistory)
          }
        }
        DisposableEffect(Unit) {
          onDispose {
            networkState.closeClient()
          }
        }

        networkState.showError = { scaffoldState.snackbarHostState.showSnackbar(it) }

        Scaffold(
          scaffoldState = scaffoldState,
          floatingActionButton = { queue.RefreshButton(networkState::refresh) },
        ) { padding ->
          queue.Performances(
            Modifier.padding(padding),
            isNew = { !transientHistory.contains(it) },
            isSelected = { selectedPerformance == it },
            select = { selectedPerformance = it },
          )
        }
        selectedPerformance?.let {
          (transientHistory[it] ?: History.Entry()).GradingPopup(it,
            onDismiss = { selectedPerformance = null },
            onGrade = { grade, comment ->
              transientHistory[it] = History.Entry(grade, comment)
              networkState.grade(it, grade, comment)
            })
        }
      }
    }
  }
}