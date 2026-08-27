package com.example.wammy.ui.reader

/**
 * Singleton in-memory store to share large data between Activities
 * without using Intent extras (which have a 1MB limit).
 */
object NovelSessionStore {
    var chapterPaths: List<String> = emptyList()
    var chapterNames: List<String> = emptyList()
    var currentIndex: Int = 0
    var apkFile: String = ""
    var pkgName: String = ""
    var novelUrl: String = ""
}
