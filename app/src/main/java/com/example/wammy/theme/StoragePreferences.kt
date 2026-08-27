package com.example.wammy.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StoragePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("storage_prefs", Context.MODE_PRIVATE)

    private val _storageUri = MutableStateFlow(prefs.getString("storage_uri", null))
    val storageUri: StateFlow<String?> = _storageUri.asStateFlow()

    fun setStorageUri(uri: String) {
        prefs.edit().putString("storage_uri", uri).apply()
        _storageUri.value = uri
    }
}
