// Created by Notch
package com.example.wammy.data.remote.extensions

data class KeiyoushiRepo(
    val extensionList: ExtensionListContainer
)

data class ExtensionListContainer(
    val extensions: List<Extension>
)

data class Extension(
    val name: String,
    val packageName: String,
    val resources: ExtensionResources,
    val extensionLib: String,
    val versionCode: String,
    val versionName: String,
    val contentWarning: String,
    val sources: List<ExtensionSource>
)

data class ExtensionResources(
    val apkUrl: String,
    val iconUrl: String,
    val jarUrl: String
)

data class ExtensionSource(
    val id: String,
    val name: String,
    val language: String,
    val homeUrl: String
)
