package com.cineshelf.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cineshelf.app.data.LibraryRepository
import com.cineshelf.app.data.SeasonGroup
import com.cineshelf.app.data.ShowItem
import com.cineshelf.app.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ShowDetailUiState(
    val show: ShowItem? = null,
    val isLoading: Boolean = true
)

class ShowDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(application)

    private val _uiState = MutableStateFlow(ShowDetailUiState())
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    fun load(folderPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val show = withContext(Dispatchers.IO) { repository.scanShow(File(folderPath)) }
            _uiState.value = ShowDetailUiState(show = show, isLoading = false)
        }
    }

    fun toggleWatched(item: VideoItem) {
        val newWatched = !item.watched
        // Optimistic local update for snappy UI
        updateItemLocally(item.id) { it.copy(watched = newWatched, positionMs = if (newWatched) 0 else it.positionMs) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.setWatched(item, newWatched)
        }
    }

    /** Permanently deletes the file from disk. Caller is responsible for the undo window. */
    fun commitDelete(item: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVideo(item)
        }
    }

    private fun updateItemLocally(id: String, transform: (VideoItem) -> VideoItem) {
        val current = _uiState.value.show ?: return
        val newSeasons = current.seasons.map { group: SeasonGroup ->
            group.copy(episodes = group.episodes.map { if (it.id == id) transform(it) else it })
        }
        val newStandalone = current.standalone.map { if (it.id == id) transform(it) else it }
        _uiState.value = _uiState.value.copy(show = current.copy(seasons = newSeasons, standalone = newStandalone))
    }
}
