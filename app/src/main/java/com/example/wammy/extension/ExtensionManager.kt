// Created by Notch
package com.example.wammy.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import dalvik.system.DexFile
import android.content.SharedPreferences
import com.example.wammy.data.remote.extensions.Extension
import dalvik.system.PathClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class ExtensionInfo(val iconUrl: String?, val versionName: String?)

class ExtensionManager(private val context: Context) {
    private val client = OkHttpClient()
    private val extensionsDir = File(context.filesDir, "extensions").apply { mkdirs() }
    private val prefs: SharedPreferences = context.getSharedPreferences("installed_extensions", Context.MODE_PRIVATE)
    private val infoPrefs: SharedPreferences = context.getSharedPreferences("extension_info", Context.MODE_PRIVATE)

    // Maps package name to the loaded ClassLoader
    private val loadedExtensions = mutableMapOf<String, PathClassLoader>()
    val activeSources = mutableListOf<eu.kanade.tachiyomi.source.Source>()

    init {
        // Auto-load all previously installed extensions on startup
        getInstalledPackageNames().forEach { pkg -> loadExtension(pkg) }
    }

    fun getExtensionInfo(packageName: String): ExtensionInfo? {
        val iconUrl = infoPrefs.getString("${packageName}_icon", null)
        val version = infoPrefs.getString("${packageName}_version", null)
        return if (iconUrl != null) ExtensionInfo(iconUrl, version) else null
    }

    private fun saveExtensionInfo(packageName: String, iconUrl: String?, versionName: String?) {
        infoPrefs.edit()
            .putString("${packageName}_icon", iconUrl)
            .putString("${packageName}_version", versionName)
            .apply()
    }

    fun getInstalledPackageNames(): Set<String> {
        return prefs.getStringSet("installed", emptySet()) ?: emptySet()
    }

    private fun saveInstalled(packageNames: Set<String>) {
        prefs.edit().putStringSet("installed", packageNames).apply()
    }

    suspend fun downloadAndInstallExtension(extension: Extension): Boolean = withContext(Dispatchers.IO) {
        try {
            val apkUrl = extension.resources.apkUrl
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                android.util.Log.e("ExtensionManager", "Download failed: HTTP ${response.code} for $apkUrl")
                return@withContext false
            }

            val apkFile = File(extensionsDir, "${extension.packageName}.apk")
            response.body!!.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Mark as installed in prefs
            val installed = getInstalledPackageNames().toMutableSet()
            installed.add(extension.packageName)
            saveInstalled(installed)

            // Save extension metadata (CDN icon, version) for later use by Sources tab
            saveExtensionInfo(extension.packageName, extension.resources.iconUrl, extension.versionName)

            loadExtension(extension.packageName)
            android.util.Log.d("ExtensionManager", "Successfully installed: ${extension.packageName}")
            true
        } catch (e: Throwable) {
            android.util.Log.e("ExtensionManager", "Install error: ${e.message}", e)
            false
        }
    }

    fun uninstallExtension(packageName: String) {
        val apkFile = File(extensionsDir, "$packageName.apk")
        if (apkFile.exists()) apkFile.delete()
        loadedExtensions.remove(packageName)
        activeSources.removeAll { src -> 
            src.javaClass.name.startsWith(packageName)
        }
        val installed = getInstalledPackageNames().toMutableSet()
        installed.remove(packageName)
        saveInstalled(installed)
    }

    fun getPackageNameForSource(source: eu.kanade.tachiyomi.source.Source): String? {
        val className = source.javaClass.name
        return loadedExtensions.keys.find { className.startsWith(it) }
    }

    private fun loadExtension(packageName: String) {
        // Prevent duplicates if an extension is re-downloaded or updated
        activeSources.removeAll { it.javaClass.name.startsWith(packageName) }
        
        val apkFile = File(extensionsDir, "$packageName.apk")
        if (!apkFile.exists()) return
        apkFile.setReadOnly()
        
        // Extract icon
        try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (packageInfo != null) {
                packageInfo.applicationInfo!!.sourceDir = apkFile.absolutePath
                packageInfo.applicationInfo!!.publicSourceDir = apkFile.absolutePath
                val iconDrawable = packageInfo.applicationInfo!!.loadIcon(context.packageManager)
                val iconFile = File(context.cacheDir, "${packageName}_icon.png")
                if (!iconFile.exists()) {
                    val bitmap = if (iconDrawable is BitmapDrawable) {
                        iconDrawable.bitmap
                    } else {
                        val bmp = Bitmap.createBitmap(iconDrawable.intrinsicWidth.takeIf { it > 0 } ?: 96,
                            iconDrawable.intrinsicHeight.takeIf { it > 0 } ?: 96, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        iconDrawable.draw(canvas)
                        bmp
                    }
                    FileOutputStream(iconFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val classLoader = PathClassLoader(apkFile.absolutePath, context.classLoader)
            loadedExtensions[packageName] = classLoader

            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                android.content.pm.PackageManager.GET_META_DATA
            )
            var className = packageInfo?.applicationInfo?.metaData?.getString("tachiyomi.extension.class")
            if (className == null) {
                android.util.Log.w("ExtensionManager", "MetaData was null for $packageName. Guessing class name.")
            }

            // List of class names to try if the exact one from metadata fails or is null
            val classNamesToTry = mutableListOf<String>()
            if (className != null) {
                classNamesToTry.add(if (className.startsWith(".")) packageName + className else className)
            }
            
            // Bulletproof fallback: Scan the DEX file directly for the Source or SourceFactory class!
            try {
                val dexFile = DexFile(apkFile)
                val entries = dexFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.startsWith(packageName) && !entry.contains("$")) {
                        classNamesToTry.add(entry)
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("ExtensionManager", "DexFile scan failed: ${e.message}")
            }
            
            // Also add the standard guesses just in case DexFile failed
            val parts = packageName.split(".")
            val lastPart = parts.last().replaceFirstChar { it.uppercase() }
            classNamesToTry.add("$packageName.$lastPart")
            classNamesToTry.add("$packageName.${lastPart}Factory")

            var loaded = false
            for (finalClassName in classNamesToTry) {
                try {
                    val sourceClass = classLoader.loadClass(finalClassName)
                    val instance = sourceClass.newInstance()
                    if (instance is eu.kanade.tachiyomi.source.Source) {
                        activeSources.add(instance)
                        android.util.Log.d("ExtensionManager", "Loaded source: ${instance.name}")
                        loaded = true
                        break
                    } else if (instance is eu.kanade.tachiyomi.source.SourceFactory) {
                        val allSources = instance.createSources()
                        
                        // Filter out duplicates by only keeping English or universal languages
                        val filteredSources = allSources.filter { 
                            it.lang == "en" || it.lang == "all" || it.lang.isEmpty() 
                        }
                        
                        val sourcesToKeep = if (filteredSources.isNotEmpty()) filteredSources else listOf(allSources.first())

                        activeSources.addAll(sourcesToKeep)
                        sourcesToKeep.forEach { 
                            android.util.Log.d("ExtensionManager", "Loaded factory source: ${it.name} (${it.lang})")
                        }
                        loaded = true
                        break
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("ExtensionManager", "Could not instantiate $finalClassName: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("ExtensionManager", "Error loading extension $packageName: ${e.message}", e)
        }
    }
}
