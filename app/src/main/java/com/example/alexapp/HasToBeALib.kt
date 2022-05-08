package com.example.alexapp

import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class Pref<T>(val value: T, val setter: suspend (T) -> Unit)

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