// Created by Notch
package com.example.wammy.data.remote.extensions

import retrofit2.http.GET
import retrofit2.http.Url

interface ExtensionApi {
    @GET
    suspend fun getExtensions(@Url url: String): KeiyoushiRepo
}
