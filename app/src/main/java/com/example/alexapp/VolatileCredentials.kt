package com.example.alexapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class VolatileCredentials : ViewModel() {
  val loginData = MutableLiveData("")
  val tokenData = MutableLiveData("")

  var login: String?
    get() = loginData.value
    set(login) {
      loginData.value = login
    }

  var token: String?
    get() = tokenData.value
    set(token) {
      tokenData.value = token
    }

  fun refreshToken() {
    val time = Calendar.getInstance().time
    tokenData.value = Pair(loginData, time).hashCode().toString(16).take(8)
  }
}