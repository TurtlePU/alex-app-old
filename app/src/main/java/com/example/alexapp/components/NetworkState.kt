package com.example.alexapp.components

import GetQueue
import Performance
import PostAuth
import PostGrade
import Protocol
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class NetworkState : ViewModel() {
  lateinit var hostState: SnackbarHostState
  private var snapshot: Snapshot? by mutableStateOf(null)

  data class Snapshot(val host: String, val login: String, val token: String)

  private val client: HttpClient = HttpClient(CIO) {
    install(JsonFeature) {
      serializer = KotlinxSerializer(Protocol.json)
    }
  }

  suspend fun auth(snapshot: Snapshot): Boolean {
    return try {
      val response: HttpResponse = client.post("${snapshot.host}/auth") {
        contentType(ContentType.Application.Json)
        body = PostAuth(snapshot.login, snapshot.token)
      }
      assert(response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created)
      this.snapshot = snapshot
      true
    } catch (e : Throwable) {
      hostState.showSnackbar(e.localizedMessage ?: "AUTH: Unknown error")
      false
    }
  }

  suspend fun refresh(since: Int): Sequence<Performance> {
    return try {
      val (host, _, _) = snapshot!!
      client.get<Array<Performance>>("$host/queue") {
        contentType(ContentType.Application.Json)
        body = GetQueue(since)
      }.asSequence()
    } catch (e : Throwable) {
      hostState.showSnackbar(e.localizedMessage ?: "REFRESH: Unknown error")
      emptySequence()
    }
  }

  suspend fun grade(performance: Performance, grade: Double, comment: String?) {
    try {
      val (host, jury, token) = snapshot!!
      val response: HttpResponse = client.post("$host/grade") {
        contentType(ContentType.Application.Json)
        body = PostGrade(jury, token, performance, grade, comment)
      }
      assert(response.status == HttpStatusCode.OK)
    } catch (e : Throwable) {
      hostState.showSnackbar(e.localizedMessage ?: "GRADE: Unknown error")
    }
  }

  fun closeClient() {
    client.close()
  }
}