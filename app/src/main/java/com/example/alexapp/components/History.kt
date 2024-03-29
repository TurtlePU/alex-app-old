package com.example.alexapp.components

import Performance
import Protocol
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.alexapp.Pref
import com.example.alexapp.makeCancelable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class History : ViewModel() {
  val transient = mutableStateMapOf<Performance, Entry>()
  private lateinit var setter: suspend (String) -> Unit

  companion object {
    const val minimalJson: String = "[]"
  }

  fun setPref(pref: Pref<String>) {
    transient.putAll(Protocol.json.decodeFromString<Map<Performance, Entry>>(pref.value))
    setter = pref.setter
  }

  suspend fun serialize() {
    setter(Protocol.json.encodeToString(transient))
  }

  @Serializable
  data class Entry(private val grade: Double?, private val comment: String?) {
    constructor() : this(null, null)

    @Composable
    fun GradingPopup(
      performance: Performance,
      onDismiss: () -> Unit,
      onGrade: suspend (Double, String?) -> Unit
    ) {
      var comment: String? by rememberSaveable { mutableStateOf(comment) }
      var grade: Double by rememberSaveable { mutableStateOf(grade ?: 5.0) }
      val (onClick, isRunning) = makeCancelable {
        onGrade(grade, comment)
        onDismiss()
      }
      AlertDialog(
        onDismissRequest = onDismiss,
        title = {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = performance.participantName,
              textAlign = TextAlign.Center,
              fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.padding(1.dp))
            Text(
              text = performance.repertoire,
              textAlign = TextAlign.Center,
              fontSize = 20.sp,
            )
          }
        },
        text = {
          TextField(
            value = comment ?: "",
            textStyle = LocalTextStyle.current.merge(TextStyle(fontSize = 20.sp)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(2.dp),
            onValueChange = { comment = it },
          )
        },
        buttons = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              val buttonRow = @Composable { i: Int ->
                Row {
                  Button(
                    modifier = Modifier
                      .padding(1.dp)
                      .weight(1f)
                      .fillMaxWidth(),
                    onClick = { grade = i * 1.0 },
                  ) {
                    Text(text = i.toString())
                  }
                  Button(
                    modifier = Modifier
                      .padding(1.dp)
                      .weight(1f)
                      .fillMaxWidth(),
                    onClick = { grade = i + 0.5 },
                  ) {
                    Text(text = (i + 0.5).toString())
                  }
                }
              }
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
              ) {
                for (i in 5..10 step 2) {
                  buttonRow(i)
                }
              }
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
              ) {
                for (i in 6..9 step 2) {
                  buttonRow(i)
                }
                Button(
                  modifier = Modifier
                    .padding(1.dp)
                    .fillMaxWidth(),
                  onClick = { grade = 10.0 },
                ) {
                  Text(text = "10")
                }
              }
            }
            Spacer(modifier = Modifier.padding(1.dp))
            Button(onClick) {
              Icon(
                imageVector = if (isRunning) Icons.Filled.Cancel else Icons.Filled.StarRate,
                contentDescription = null,
              )
            }
          }
        },
      )
    }
  }
}