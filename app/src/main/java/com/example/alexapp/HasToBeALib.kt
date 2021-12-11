package com.example.alexapp

import android.content.Context
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Pref<T>(val value: T, val setter: suspend (T) -> Unit)

@Composable
fun Context.stringPref(key: String, default: String = ""): Pref<String> {
  val pref = stringPreferencesKey(key)
  val value by dataStore.data.map { it[pref] ?: default }.collectAsState(default)
  val setter: suspend (String) -> Unit = { dataStore.edit { settings -> settings[pref] = it } }
  return Pref(value, setter)
}

@Composable
fun Password(
  value: String,
  onValueChange: (String) -> Unit = { },
  readOnly: Boolean = false,
  placeholder: String = "password",
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
    placeholder = { Text(placeholder) },
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