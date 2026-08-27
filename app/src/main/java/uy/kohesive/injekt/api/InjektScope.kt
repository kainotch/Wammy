// Created by Notch
package uy.kohesive.injekt.api

import kotlinx.serialization.json.Json
import eu.kanade.tachiyomi.network.NetworkHelper
import java.lang.reflect.Type

class InjektScope : InjektFactory {
    inline fun <reified T> get(): T {
        return get(T::class.java)
    }
    
    fun <T> get(clazz: Class<T>): T {
        return getInternal(clazz.name) as T
    }

    fun <T> get(type: FullTypeReference<T>): T {
        return getInternal(type.type.toString()) as T
    }

    override fun getInstance(type: Type): Any {
        return getInternal(type.toString())
    }

    private fun getInternal(name: String): Any {
        if (name.contains("Json")) {
            return Json { 
                ignoreUnknownKeys = true
                isLenient = true 
            }
        }
        if (name.contains("eu.kanade.tachiyomi.network.NetworkHelper")) {
            return NetworkHelper
        }
        if (name.contains("android.app.Application") || name.contains("android.content.Context")) {
            return com.example.wammy.AppContainer.appContext
        }
        if (name.contains("SharedPreferences") || name.contains("PreferencesHelper")) {
            return com.example.wammy.AppContainer.appContext.getSharedPreferences("tachiyomi_extensions", android.content.Context.MODE_PRIVATE)
        }
        if (name.contains("OkHttpClient")) {
            return NetworkHelper.client
        }
        // Fallback: return app context for unknown types instead of crashing
        // Many extensions request obscure types that can use app context
        android.util.Log.w("Injekt", "Unknown type requested: $name, returning appContext")
        return com.example.wammy.AppContainer.appContext
    }
}
