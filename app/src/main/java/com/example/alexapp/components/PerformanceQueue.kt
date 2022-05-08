package com.example.alexapp.components

import Performance
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.alexapp.makeCancelable

class PerformanceQueue : ViewModel() {
  private val map = mutableStateMapOf<String, MutableList<Performance>>()
  private val size: Int get() = map.values.sumOf { it.size }

  private fun addAll(entries: Sequence<Performance>) {
    for (performance in entries) {
      map.computeIfAbsent(performance.participant.category) {
        mutableStateListOf()
      }.add(performance)
    }
  }

  @ExperimentalFoundationApi
  @Composable
  fun Performances(
    modifier: Modifier = Modifier,
    isNew: (Performance) -> Boolean,
    isSelected: (Performance) -> Boolean,
    select: (Performance) -> Unit,
  ) {
    LazyColumn(modifier) {
      val entries = map.entries.sortedBy { it.key }
      entries.forEach { (category, performances) ->
        stickyHeader {
          Box(
            Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colors.background)
          ) {
            Text(
              text = category,
              fontSize = 20.sp,
              modifier = Modifier.padding(15.dp)
            )
          }
        }
        var isFirst = true
        items(performances, key = { Pair(it.participantName, it.repertoire) }) {
          if (isFirst) Divider()
          isFirst = false
          Box(
            modifier = Modifier
              .selectable(selected = isSelected(it), onClick = { select(it) })
              .fillMaxWidth()
              .run {
                if (isNew(it)) background(color = MaterialTheme.colors.secondary)
                else this
              }
          ) {
            Column(modifier = Modifier.padding(15.dp)) {
              Text(
                text = it.participantName,
                fontSize = 20.sp,
              )
              Text(
                text = it.repertoire,
                fontSize = 20.sp,
              )
            }
          }
          Divider()
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