package com.example.alexapp.components

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.alexapp.Password
import com.example.alexapp.Pref
import com.example.alexapp.makeCancelable
import com.example.alexapp.makeRace
import java.util.*

data class AuthState(
  private val hostPref: Pref<String>,
  private val loginPref: Pref<String>,
  private val tokenPref: Pref<String>
) {
  data class AuthSnap(val host: String, val login: String, val token: String)

  @Composable
  fun AuthDrawer(tryAuth: suspend (AuthSnap) -> Unit) {
    var dirtyLogin: String? by rememberSaveable { mutableStateOf(null) }
    var dirtyToken: String? by rememberSaveable { mutableStateOf(null) }

    val refreshHost = makeRace(hostPref.setter)
    val refreshLogin = { it: String -> dirtyLogin = it }
    val refreshToken: () -> Unit = {
      dirtyToken = dirtyLogin?.let {
        val time = Calendar.getInstance().time
        Pair(it, time).hashCode().toString(16).take(8)
      }
    }
    val resetLocals: () -> Unit = {
      dirtyLogin = loginPref.value
      dirtyToken = tokenPref.value
    }
    val (onClick, isRunning) = makeCancelable {
      val tryLogin = dirtyLogin ?: return@makeCancelable
      val tryToken = dirtyToken ?: return@makeCancelable
      tryAuth(AuthSnap(hostPref.value, tryLogin, tryToken))
      loginPref.setter(tryLogin)
      tokenPref.setter(tryToken)
    }

    TextField(value = hostPref.value, onValueChange = refreshHost)
    TextField(value = dirtyLogin ?: "", onValueChange = refreshLogin)
    Password(value = dirtyToken ?: "")
    Button(onClick = refreshToken) { Text("Refresh token") }
    Button(onClick = resetLocals) { Text("Reset to OK") }
    Button(onClick) { Text(if (isRunning) "Cancel" else "Check credentials") }
  }
}