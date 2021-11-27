package com.example.alexapp

import Jury
import JuryToken
import Performance
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class API : ViewModel() {
  private val login = MutableLiveData("")
  private val token = MutableLiveData("")
  val host = MutableLiveData("")

  val lastLogin: Jury? get() = login.value
  val lastToken: JuryToken? get() = token.value

  fun readPreferences(preferences: SharedPreferences) = with(preferences) {
    login.value = getString("login", null)
    token.value = getString("token", null)
    host.value = getString("host", null)
  }

  fun writePreferences(preferences: SharedPreferences) = with(preferences.edit()) {
    putString("login", lastLogin)
    putString("token", lastToken)
    putString("host", host.value)
    apply()
  }

  suspend fun auth(login: Jury, token: JuryToken) {
    // TODO: try login
    this.login.value = login
    this.token.value = token
  }

  suspend fun getUpdates(since: Int): List<Performance> {
    // TODO: get queue
    return listOf()
  }

  suspend fun grade(performance: Performance, grade: Double, comment: String?) {
    // TODO: grade
  }
}