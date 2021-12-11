package com.example.alexapp.components

import Performance
import Protocol
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Popup
import com.example.alexapp.Pref
import com.example.alexapp.makeCancelable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class History(private val pref: Pref<String>) {
  @Serializable
  data class Entry(private val grade: Double?, private val comment: String?) {
    constructor() : this(null, null)

    @Composable
    fun GradingPopup(onDismiss: () -> Unit, onGrade: suspend (Double, String?) -> Unit) {
      Popup(onDismissRequest = onDismiss) {
        var grade: Double? by rememberSaveable { mutableStateOf(grade) }
        var comment: String? by rememberSaveable { mutableStateOf(comment) }

        val refreshGrade = { it: String -> grade = it.toDoubleOrNull() }
        val refreshComment = { it: String -> comment = it }
        val (onClick, isRunning) = makeCancelable {
          grade?.let { onGrade(it, comment); onDismiss() }
        }

        val imageVector = if (isRunning) Icons.Filled.Cancel else Icons.Filled.StarRate
        TextField(value = grade?.toString() ?: "0", onValueChange = refreshGrade)
        TextField(value = comment ?: "", onValueChange = refreshComment)
        Button(onClick) { Icon(imageVector = imageVector, contentDescription = null) }
      }
    }
  }

  companion object {
    const val minimalJson: String = "[]"
  }

  val deserialized: MutableMap<Performance, Entry>
    get() = Protocol.json.decodeFromString(pref.value)

  suspend fun serialize(value: MutableMap<Performance, Entry>) {
    pref.setter(Protocol.json.encodeToString(value))
  }
}