package com.example.alexapp.components

import Performance
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.example.alexapp.makeCancelable

class PerformanceQueue : ViewModel() {
  private val map: MutableMap<String, MutableList<Performance>> by mutableStateOf(mutableMapOf())
  private val size: Int get() = map.values.sumOf { it.size }

  private fun addAll(entries: Sequence<Performance>) {
    for (performance in entries) {
      map.putIfAbsent(performance.participant.category, mutableListOf())!!.add(performance)
    }
  }

  @ExperimentalFoundationApi
  @Composable
  fun Column(
    modifier: Modifier = Modifier,
    isNew: (Performance) -> Boolean,
    isSelected: (Performance) -> Boolean,
    select: (Performance) -> Unit,
  ) {
    LazyColumn(modifier) {
      map.forEach { (category, performances) ->
        stickyHeader { Text(category) }
        items(performances, key = { Pair(it.participantName, it.repertoire) }) {
          val boxModifier = Modifier
            .selectable(selected = isSelected(it), onClick = { select(it) })
            .run { if (isNew(it)) background(color = MaterialTheme.colors.secondary) else this }
          Box(boxModifier) { Text(it.participantName) }
        }
      }
    }
  }

  @Composable
  fun RefreshButton(refresh: suspend (Int) -> Sequence<Performance>) {
    val (onClick, isRunning) = makeCancelable {
      val since = size
      val updates = refresh(since)
      Log.d(null, "Requesting updates since $since")
      addAll(updates)
    }

    val imageVector = if (isRunning) Icons.Filled.Cancel else Icons.Filled.Refresh
    FloatingActionButton(onClick) { Icon(imageVector = imageVector, contentDescription = null) }
  }
}