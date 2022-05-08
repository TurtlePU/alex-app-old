package com.example.alexapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.alexapp.Pref
import com.example.alexapp.makeCancelable
import java.util.*
import kotlin.random.Random

data class AuthState(
  private val hostPref: Pref<String>,
  private val loginPref: Pref<String>,
  private val tokenPref: Pref<String>
) {
  companion object {
    private fun generateToken(
      login: String,
      time: Date = Calendar.getInstance().time,
      salt: Int = Random.nextInt(),
    ) = (login.hashCode() xor time.hashCode() xor salt).toString(32)
  }

  @Composable
  fun AuthDrawer(
    tryAuth: suspend (NetworkState.Snapshot) -> Boolean,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
  ) {
    var dirtyHost: String? by rememberSaveable { mutableStateOf(null) }
    var dirtyLogin: String? by rememberSaveable { mutableStateOf(null) }
    var dirtyToken: String? by rememberSaveable { mutableStateOf(null) }

    val refreshToken: () -> Unit = {
      dirtyToken = dirtyLogin?.let(::generateToken)
    }
    val resetLocals: () -> Unit = {
      dirtyHost = hostPref.value
      dirtyLogin = loginPref.value
      dirtyToken = tokenPref.value
    }
    val (checkCredentials, isChecking) = makeCancelable {
      val tryHost = dirtyHost ?: return@makeCancelable
      hostPref.setter(tryHost)
      val tryLogin = dirtyLogin ?: return@makeCancelable
      val tryToken = dirtyToken ?: return@makeCancelable
      if (tryAuth(NetworkState.Snapshot(tryHost, tryLogin, tryToken))) {
        loginPref.setter(tryLogin)
        tokenPref.setter(tryToken)
        onSuccess()
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = modifier
        .fillMaxSize()
        .padding(32.dp),
    ) {
      TextField(
        value = dirtyHost ?: "",
        onValueChange = { dirtyHost = it },
        placeholder = { Text("Host") },
        singleLine = true,
      )
      TextField(
        value = dirtyLogin ?: "",
        onValueChange = { dirtyLogin = it; refreshToken() },
        placeholder = { Text("Login") },
        singleLine = true,
      )
      Password(
        value = dirtyToken ?: "",
        placeholder = "Token",
      )
      Spacer(modifier = Modifier.padding(4.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Button(onClick = refreshToken) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
          )
        }
        Button(onClick = resetLocals) {
          Icon(
            imageVector = Icons.Filled.RestoreFromTrash,
            contentDescription = null,
          )
        }
        Button(onClick = checkCredentials) {
          Icon(
            imageVector = if (isChecking) Icons.Filled.Cancel else Icons.Filled.Login,
            contentDescription = null,
          )
        }
      }
    }
  }
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
      IconButton(onClick = { hideValue = !hideValue }) {
        Icon(
          imageVector = imageVector,
          contentDescription = hideValueDescription,
        )
      }
    },
  )
}