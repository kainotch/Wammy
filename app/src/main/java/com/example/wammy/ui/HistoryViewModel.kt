// Created by Notch
package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel : ViewModel() {

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val history: StateFlow<List<HistoryEntity>> = kotlinx.coroutines.flow.combine(
        AppContainer.database.historyDao().getAllHistory(),
        _searchQuery
    ) { hist, query ->
        if (query.isEmpty()) hist else hist.filter { it.mangaTitle.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            AppContainer.database.historyDao().deleteHistory(id)
        }
    }
    
    fun clearAllHistory() {
        viewModelScope.launch {
            AppContainer.database.historyDao().clearHistory()
        }
    }

    fun formatRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val days = diff / (1000 * 60 * 60 * 24)
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            else -> {
                val format = SimpleDateFormat("M/d/yy", Locale.getDefault())
                format.format(Date(timestamp))
            }
        }
    }
    
    fun formatTime(timestamp: Long): String {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}
