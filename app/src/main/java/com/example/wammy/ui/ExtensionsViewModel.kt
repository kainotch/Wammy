// Created by Notch
package com.example.wammy.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.remote.extensions.Extension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExtensionsViewModel : ViewModel() {

    private val _extensions = MutableStateFlow<List<Extension>>(emptyList())
    val extensions: StateFlow<List<Extension>> = _extensions.asStateFlow()

    private val _installedPackageNames = MutableStateFlow<Set<String>>(emptySet())
    val installedPackageNames: StateFlow<Set<String>> = _installedPackageNames.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Track install progress per extension
    private val _installing = MutableStateFlow<Set<String>>(emptySet())
    val installing: StateFlow<Set<String>> = _installing.asStateFlow()

    init {
        fetchExtensions()
        loadInstalledExtensions()
    }

    private fun fetchExtensions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val repo = AppContainer.extensionApi.getExtensions("https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json")
                _extensions.value = repo.extensionList.extensions
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadInstalledExtensions() {
        viewModelScope.launch {
            val installed = AppContainer.extensionManager.getInstalledPackageNames()
            _installedPackageNames.value = installed
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun installExtension(extension: Extension, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _installing.value = _installing.value + extension.packageName
            val success = AppContainer.extensionManager.downloadAndInstallExtension(extension)
            if (success) {
                _installedPackageNames.value = _installedPackageNames.value + extension.packageName
            }
            _installing.value = _installing.value - extension.packageName
            onResult(success)
        }
    }

    fun uninstallExtension(packageName: String) {
        viewModelScope.launch {
            AppContainer.extensionManager.uninstallExtension(packageName)
            _installedPackageNames.value = _installedPackageNames.value - packageName
        }
    }

    fun getFilteredExtensions(showInstalled: Boolean): List<Extension> {
        val query = _searchQuery.value
        val all = if (showInstalled) {
            _extensions.value.filter { _installedPackageNames.value.contains(it.packageName) }
        } else {
            _extensions.value.filter { !_installedPackageNames.value.contains(it.packageName) }
        }
        if (query.isEmpty()) return all
        return all.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.sources.any { source -> source.language.contains(query, ignoreCase = true) }
        }
    }
}
