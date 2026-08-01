package com.cineshelf.app.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cineshelf.app.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(application)

    fun getInitialPosition(file: File): Long = repository.getInitialPosition(file)

    fun findSubtitleFiles(file: File): List<File> = repository.findSubtitleFiles(file)

    fun saveProgress(file: File, positionMs: Long, durationMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProgress(file, positionMs, durationMs)
        }
    }
}
