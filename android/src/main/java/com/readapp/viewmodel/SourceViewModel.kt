package com.readapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.readapp.data.ReadApiService
import com.readapp.data.ReadRepository
import com.readapp.data.UserPreferences
import com.readapp.data.model.BookSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SourceViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val accessTokenState = MutableStateFlow("")
    private val repository = ReadRepository { endpoint ->
        ReadApiService.create(endpoint) { accessTokenState.value }
    }

    private val _sources = MutableStateFlow<List<BookSource>>(emptyList())
    val sources = _sources.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.accessToken.collect { token ->
                accessTokenState.value = token
            }
        }
        fetchSources()
    }

    fun fetchSources() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // We need to get the latest values from preferences
            val serverUrl = userPreferences.serverUrl.first()
            val publicUrl = userPreferences.publicServerUrl.first().ifBlank { null }
            val token = userPreferences.accessToken.first()

            if (token.isBlank()) {
                _errorMessage.value = "Not logged in"
                _isLoading.value = false
                return@launch
            }

            val result = repository.getBookSources(
                baseUrl = serverUrl,
                publicUrl = publicUrl,
                accessToken = token
            )

            result.onSuccess {
                _sources.value = it
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to load sources"
            }

            _isLoading.value = false
        }
    }

    fun deleteSource(source: BookSource) {
        val currentSources = _sources.value
        _sources.value = currentSources.filter { it.bookSourceUrl != source.bookSourceUrl }

        viewModelScope.launch {
            val serverUrl = userPreferences.serverUrl.first()
            val publicUrl = userPreferences.publicServerUrl.first().ifBlank { null }
            val token = userPreferences.accessToken.first()

            val result = repository.deleteBookSource(serverUrl, publicUrl, token, source.bookSourceUrl)
            if (result.isFailure) {
                _sources.value = currentSources // Revert
                _errorMessage.value = result.exceptionOrNull()?.message ?: "删除失败"
            }
        }
    }

    fun toggleSource(source: BookSource) {
        val currentSources = _sources.value
        val newSources = currentSources.map {
            if (it.bookSourceUrl == source.bookSourceUrl) it.copy(enabled = !it.enabled) else it
        }
        _sources.value = newSources

        viewModelScope.launch {
            val serverUrl = userPreferences.serverUrl.first()
            val publicUrl = userPreferences.publicServerUrl.first().ifBlank { null }
            val token = userPreferences.accessToken.first()

            val result = repository.toggleBookSource(serverUrl, publicUrl, token, source.bookSourceUrl, !source.enabled)
            if (result.isFailure) {
                _sources.value = currentSources // Revert
                _errorMessage.value = result.exceptionOrNull()?.message ?: "操作失败"
            }
        }
    }

    suspend fun getSourceDetail(id: String): String? {
        val serverUrl = userPreferences.serverUrl.first()
        val publicUrl = userPreferences.publicServerUrl.first().ifBlank { null }
        val token = userPreferences.accessToken.first()

        return repository.getBookSourceDetail(serverUrl, publicUrl, token, id).getOrNull()
    }

    suspend fun saveSource(jsonContent: String): Result<Any> {
        val serverUrl = userPreferences.serverUrl.first()
        val publicUrl = userPreferences.publicServerUrl.first().ifBlank { null }
        val token = userPreferences.accessToken.first()

        return repository.saveBookSource(serverUrl, publicUrl, token, jsonContent)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SourceViewModel(application)
            }
        }
    }
}
