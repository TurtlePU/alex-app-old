package com.example.alexapp

import Performance
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.alexapp.ui.theme.AlexAppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
  private val state: ContestState by viewModels()
  private val credentials: VolatileCredentials by viewModels()
  private val api: API by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    api.readPreferences(preferences)
    setContent {
      AlexAppTheme {
        Surface(color = MaterialTheme.colors.background) {
          Scaffold(
            drawerContent = {
              EditData(api.host)
              EditData(credentials.loginData)
              Password(credentials.tokenData)
              Button(onClick = credentials::refreshToken) {
                Text("Refresh token")
              }
              Button(onClick = ::resetVolatile) {
                Text("Reset to OK")
              }
              Cancelable(::tryAuth) { onClick, isRunning ->
                Button(onClick) {
                  Text(if (isRunning) "Cancel" else "Check credentials")
                }
              }
            },
            floatingActionButton = {
              Cancelable({ state.updateUsing(api::getUpdates) }) { onClick, isRunning ->
                FloatingActionButton(onClick) {
                  Icon(
                    imageVector = if (isRunning) Icons.Filled.Cancel else Icons.Filled.Refresh,
                    contentDescription = null
                  )
                }
              }
            }
          ) { padding ->
            val categories: Map<String, List<Performance>>
                by state.performanceCategories.observeAsState(mapOf())
            LazyColumn(modifier = Modifier.padding(padding)) {
              categories.forEach { (category, performances) ->
                stickyHeader { Text(category) }
                items(performances, key = { Pair(it.participantName, it.repertoire) }) {
                  Text(it.participantName)
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onDestroy() {
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    api.writePreferences(preferences)
    super.onDestroy()
  }

  private suspend fun tryAuth() {
    api.auth(
      credentials.login ?: return,
      credentials.token ?: return,
    )
  }

  private fun resetVolatile() {
    credentials.login = api.lastLogin
    credentials.token = api.lastToken
  }
}

@Composable
fun EditData(data: MutableLiveData<String>) {
  val text: String by data.observeAsState("")
  TextField(value = text, onValueChange = { data.value = it })
}

@Composable
fun Password(
  data: LiveData<String>,
  onValueChange: (String) -> Unit = { },
  readOnly: Boolean = true,
  hideValueDescription: String? = null,
) {
  val text by data.observeAsState("")
  var hideValue by remember { mutableStateOf(true) }
  val visualTransformation =
    if (hideValue) PasswordVisualTransformation() else VisualTransformation.None
  val imageVector =
    if (hideValue) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
  val flipHide: () -> Unit =
    { hideValue = !hideValue }
  TextField(
    value = text,
    onValueChange = onValueChange,
    readOnly = readOnly,
    visualTransformation = visualTransformation,
    trailingIcon = {
      IconButton(onClick = flipHide) {
        Icon(
          imageVector = imageVector,
          contentDescription = hideValueDescription,
        )
      }
    },
  )
}

@Composable
fun Cancelable(
  f: suspend () -> Unit,
  content: @Composable (clb: () -> Unit, isRunning: Boolean) -> Unit
) {
  val scope = rememberCoroutineScope()
  val state = remember { mutableStateOf(null as Job?) }
  var job by state
  val clb: () -> Unit = {
    job = job?.let { it.cancel(); null }
      ?: scope.launch { f(); job = null }
  }
  val isRunning = job != null
  content(clb, isRunning)
}