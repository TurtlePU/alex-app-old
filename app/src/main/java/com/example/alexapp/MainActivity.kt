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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alexapp.components.AuthState
import com.example.alexapp.components.History
import com.example.alexapp.components.NetworkState
import com.example.alexapp.components.PerformanceQueue
import com.example.alexapp.ui.theme.AlexAppTheme
import kotlinx.coroutines.delay

@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
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
    val queue: PerformanceQueue = viewModel()
    val networkState: NetworkState = viewModel()

    val transientHistory by rememberSaveable { mutableStateOf(history.deserialized) }
    var selectedPerformance by rememberSaveable { mutableStateOf(null as Performance?) }
    val scaffoldState = rememberScaffoldState()

    networkState.hostState = scaffoldState.snackbarHostState

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

    Scaffold(
      scaffoldState = scaffoldState,
      drawerContent = { authState.AuthDrawer(networkState::auth) },
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