package com.cineshelf.app.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cineshelf.app.data.LibraryRepository
import com.cineshelf.app.data.ShowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryUiState(
    val shows: List<ShowItem> = emptyList(),
    val isLoading: Boolean = true
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val shows = withContext(Dispatchers.IO) { repository.scanLibrary() }
            _uiState.value = LibraryUiState(shows = shows, isLoading = false)
        }
    }

    fun createShow(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.createShowFolder(name) }
            refresh()
        }
    }
}
