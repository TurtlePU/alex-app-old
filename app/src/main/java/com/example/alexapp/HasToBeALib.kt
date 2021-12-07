package com.example.alexapp

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class Pref<T>(val value: T, val setter: suspend (T) -> Unit)

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