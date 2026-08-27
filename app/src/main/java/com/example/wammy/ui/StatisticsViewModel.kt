package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatisticsViewModel : ViewModel() {
    private val dao = AppContainer.database.statisticsDao()

    val inLibraryCount: StateFlow<Int> = dao.getInLibraryCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val completedCount: StateFlow<Int> = dao.getCompletedCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val startedCount: StateFlow<Int> = dao.getStartedCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    val totalChaptersCount: StateFlow<Int> = dao.getTotalChaptersCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val readChaptersCount: StateFlow<Int> = dao.getReadChaptersCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    val trackedEntriesCount: StateFlow<Int> = dao.getTrackedEntriesCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val meanScore: StateFlow<Float?> = dao.getMeanScore().stateIn(viewModelScope, SharingStarted.Lazily, 0f)
    val usedTrackersCount: StateFlow<Int> = dao.getUsedTrackersCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
}
