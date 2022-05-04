package com.example.alexapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alexapp.*
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlin.random.Random

data class AuthState(
  private val hostPref: Pref<String>,
  private val loginPref: Pref<String>,
  private val tokenPref: Pref<String>
) {
  @Composable
  fun AuthDrawer(scope: CoroutineScope, tryAuth: suspend (NetworkState.Snapshot) -> Boolean) {
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
    val (checkCredentials, isChecking) = makeCancelable(scope) {
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