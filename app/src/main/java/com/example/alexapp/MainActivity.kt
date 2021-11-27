package com.example.alexapp

import Performance
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Popup
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.alexapp.ui.theme.AlexAppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Pref<T>(val value: T, val setter: suspend (T) -> Unit)

@Composable
fun Context.stringPref(key: String): Pref<String> {
  val pref = stringPreferencesKey(key)
  val value by dataStore.data.map { it[pref] ?: "" }.collectAsState("")
  val setter: suspend (String) -> Unit = { dataStore.edit { settings -> settings[pref] = it } }
  return Pref(value, setter)
}

data class AuthState(
  private val hostPref: Pref<String>,
  private val loginPref: Pref<String>,
  private val tokenPref: Pref<String>
) {
  val host: String get() = hostPref.value
  val login: String get() = loginPref.value
  val token: String get() = tokenPref.value

  suspend fun writeHost(host: String) {
    hostPref.setter(host)
  }

  suspend fun writeLogin(login: String) {
    loginPref.setter(login)
  }

  suspend fun writeToken(token: String) {
    tokenPref.setter(token)
  }
}

@Composable
fun Context.authState() = AuthState(stringPref("host"), stringPref("login"), stringPref("token"))

@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AlexAppTheme {
        Surface(color = MaterialTheme.colors.background) {
          val authState = authState()
          val queue by rememberSaveable { mutableStateOf(PerformanceQueue()) }
          var selectedPerformance: Performance? by rememberSaveable { mutableStateOf(null) }

          Scaffold(
            drawerContent = { authState.AuthDrawer() },
            floatingActionButton = { queue.RefreshButton() },
          ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
              queue.map.forEach { (category, performances) ->
                stickyHeader { Text(category) }
                items(performances, key = { Pair(it.participantName, it.repertoire) }) {
                  Text(it.participantName, modifier = Modifier.selectable(
                    selected = selectedPerformance == it,
                    onClick = { selectedPerformance = it }
                  ))
                }
              }
            }
            selectedPerformance?.let { _ ->
              GradingPopup(onDismiss = { selectedPerformance = null }) { _, _ ->
                // TODO: grade by performance, grade and comment
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AuthState.AuthDrawer() {
  var dirtyLogin: String? by rememberSaveable { mutableStateOf(null) }
  var dirtyToken: String? by rememberSaveable { mutableStateOf(null) }

  val refreshHost = makeRace(::writeHost)
  val refreshLogin = { it: String -> dirtyLogin = it }
  val refreshToken: () -> Unit = {
    dirtyToken = dirtyLogin?.let {
      val time = Calendar.getInstance().time
      Pair(it, time).hashCode().toString(16).take(8)
    }
  }
  val resetLocals: () -> Unit = {
    dirtyLogin = login
    dirtyToken = token
  }
  val (onClick, isRunning) = makeCancelable {
    val tryLogin = dirtyLogin ?: return@makeCancelable
    val tryToken = dirtyToken ?: return@makeCancelable
    // TODO: try auth
    writeLogin(tryLogin)
    writeToken(tryToken)
  }

  TextField(value = host, onValueChange = refreshHost)
  TextField(value = dirtyLogin ?: "", onValueChange = refreshLogin)
  Password(value = dirtyToken ?: "")
  Button(onClick = refreshToken) {
    Text("Refresh token")
  }
  Button(onClick = resetLocals) {
    Text("Reset to OK")
  }
  Button(onClick) { Text(if (isRunning) "Cancel" else "Check credentials") }
}

@Composable
fun Password(
  value: String,
  onValueChange: (String) -> Unit = { },
  readOnly: Boolean = true,
  hideValueDescription: String? = null,
) {
  var hideValue by rememberSaveable { mutableStateOf(true) }

  val flipHide: () -> Unit = { hideValue = !hideValue }

  val (visualTransformation, imageVector) =
    if (hideValue) Pair(PasswordVisualTransformation(), Icons.Filled.VisibilityOff)
    else Pair(VisualTransformation.None, Icons.Filled.Visibility)

  TextField(
    value = value,
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
fun PerformanceQueue.RefreshButton() {
  val (onClick, isRunning) = makeCancelable {
    val since = size
    // TODO: request updates from server
    Log.d(null, "Requesting updates since $since")
    addAll(emptySequence())
  }

  FloatingActionButton(onClick) {
    Icon(
      imageVector = if (isRunning) Icons.Filled.Cancel else Icons.Filled.Refresh,
      contentDescription = null
    )
  }
}

@Composable
fun GradingPopup(onDismiss: () -> Unit, onGrade: suspend (Double, String?) -> Unit) {
  Popup(onDismissRequest = onDismiss) {
    var grade: Double? by rememberSaveable { mutableStateOf(null) }
    var comment: String? by rememberSaveable { mutableStateOf(null) }

    val refreshGrade = { it: String -> grade = it.toDoubleOrNull() }
    val refreshComment = { it: String -> comment = it }
    val (onClick, isRunning) = makeCancelable {
      grade?.let {
        onGrade(it, comment)
        onDismiss()
      }
    }

    TextField(value = grade?.toString() ?: "0", onValueChange = refreshGrade)
    TextField(value = comment ?: "", onValueChange = refreshComment)
    Button(onClick) { Text(if (isRunning) "Cancel" else "Grade") }
  }
}

@Composable
fun <T> makeRace(action: suspend (T) -> Unit): (T) -> Unit {
  val scope = rememberCoroutineScope()
  var job: Job? by remember { mutableStateOf(null) }
  return {
    job?.cancel()
    job = scope.launch { action(it); job = null }
  }
}

@Composable
fun makeCancelable(action: suspend () -> Unit): Pair<() -> Unit, Boolean> {
  val scope = rememberCoroutineScope()
  var job: Job? by remember { mutableStateOf(null) }
  val cancelable: () -> Unit = {
    job = job?.let { it.cancel(); null }
      ?: scope.launch { action(); job = null }
  }
  val isRunning = job != null
  return Pair(cancelable, isRunning)
}

class PerformanceQueue(val map: MutableMap<String, MutableList<Performance>> = mutableMapOf()) {
  val size: Int get() = map.values.sumOf { it.size }

  fun addAll(entries: Sequence<Performance>) {
    for (performance in entries) {
      map.putIfAbsent(performance.participant.category, mutableListOf())!!.add(performance)
    }
  }
}