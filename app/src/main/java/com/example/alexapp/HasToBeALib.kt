package com.example.alexapp

import android.content.Context
import androidx.compose.runtime.*
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
  val value by remember { dataStore.data.map { it[pref] ?: default } }.collectAsState(default)
  val setter: suspend (String) -> Unit = { dataStore.edit { settings -> settings[pref] = it } }
  return Pref(value, setter)
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