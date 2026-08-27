// Created by Notch
package com.example.wammy.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupPreference(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val value: PreferenceValue,
)

@Serializable
data class BackupSourcePreferences(
    @ProtoNumber(1) val sourceKey: String,
    @ProtoNumber(2) val prefs: List<BackupPreference>,
)

@Serializable
sealed class PreferenceValue

@Serializable
@kotlinx.serialization.SerialName("eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue")
data class IntPreferenceValue(val value: Int) : PreferenceValue()

@Serializable
@kotlinx.serialization.SerialName("eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue")
data class LongPreferenceValue(val value: Long) : PreferenceValue()

@Serializable
@kotlinx.serialization.SerialName("eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue")
data class FloatPreferenceValue(val value: Float) : PreferenceValue()

@Serializable
@kotlinx.serialization.SerialName("eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue")
data class StringPreferenceValue(val value: String) : PreferenceValue()

@Serializable
@kotlinx.serialization.SerialName("eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue")
data class BooleanPreferenceValue(val value: Boolean) : PreferenceValue()

@Serializable
@kotlinx.serialization.SerialName("eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue")
data class StringSetPreferenceValue(val value: Set<String>) : PreferenceValue()
