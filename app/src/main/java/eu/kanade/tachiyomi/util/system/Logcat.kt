package eu.kanade.tachiyomi.util.system

enum class LogPriority { DEBUG, INFO, WARN, ERROR }

fun logcat(priority: LogPriority = LogPriority.INFO, e: Throwable? = null, message: () -> String) {
    val msg = message()
    when (priority) {
        LogPriority.DEBUG -> android.util.Log.d("QuickJS", msg, e)
        LogPriority.INFO -> android.util.Log.i("QuickJS", msg, e)
        LogPriority.WARN -> android.util.Log.w("QuickJS", msg, e)
        LogPriority.ERROR -> android.util.Log.e("QuickJS", msg, e)
    }
}
