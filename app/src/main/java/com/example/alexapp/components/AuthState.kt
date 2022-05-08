package com.example.alexapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alexapp.Password
import com.example.alexapp.Pref
import com.example.alexapp.makeCancelable
import java.util.*
import kotlin.random.Random

data class AuthState(
  private val hostPref: Pref<String>,
  private val loginPref: Pref<String>,
  private val tokenPref: Pref<String>
) {
  @Composable
  fun AuthDrawer(tryAuth: suspend (NetworkState.Snapshot) -> Boolean) {
    var dirtyHost: String by rememberSaveable { mutableStateOf(hostPref.value) }
    var dirtyLogin: String? by rememberSaveable { mutableStateOf(null) }
    var dirtyToken: String? by rememberSaveable { mutableStateOf(null) }

    val refreshToken: () -> Unit = {
      dirtyToken = dirtyLogin?.let {
        val time = Calendar.getInstance().time
        val salt = Random.nextInt()
        (it.hashCode() xor time.hashCode() xor salt).toString(32)
      }
    }
    val resetLocals: () -> Unit = {
      dirtyHost = hostPref.value
      dirtyLogin = loginPref.value
      dirtyToken = tokenPref.value
    }
    val (checkCredentials, isChecking) = makeCancelable {
      hostPref.setter(dirtyHost)
      val tryLogin = dirtyLogin ?: return@makeCancelable
      val tryToken = dirtyToken ?: return@makeCancelable
      if (tryAuth(NetworkState.Snapshot(dirtyHost, tryLogin, tryToken))) {
        loginPref.setter(tryLogin)
        tokenPref.setter(tryToken)
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
    ) {
      TextField(
        value = dirtyHost,
        onValueChange = { dirtyHost = it },
        placeholder = { Text("Host") },
        singleLine = true,
      )
      TextField(
        value = dirtyLogin ?: "",
        onValueChange = { dirtyLogin = it },
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
        for ((onClick, imageVector) in listOf(
          refreshToken to Icons.Filled.Refresh,
          resetLocals to Icons.Filled.RestoreFromTrash,
          checkCredentials to (if (isChecking) Icons.Filled.Cancel else Icons.Filled.Login)
        )) {
          Button(onClick = onClick) {
            Icon(
              imageVector = imageVector,
              contentDescription = null,
            )
          }
        }
      }
    }
  }
}