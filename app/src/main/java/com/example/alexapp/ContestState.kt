package com.example.alexapp

import Performance
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class ContestState : ViewModel() {
    private val _performanceCategories =
        MutableLiveData<MutableMap<String, MutableList<Performance>>>()

    val performanceCategories: LiveData<MutableMap<String, MutableList<Performance>>>
        get() = _performanceCategories

    suspend fun updateUsing(request: suspend (Int) -> List<Performance>) {
        val categories = _performanceCategories.value ?: TreeMap()
        val performancesTotal = categories.values.sumOf { it.size }
        val newPerformances = request(performancesTotal)
        for (performance in newPerformances) {
            categories.computeIfAbsent(performance.participant.category) {
                mutableListOf()
            }.add(performance)
        }
        _performanceCategories.value = categories
    }
}