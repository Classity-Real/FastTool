package dev.fasttool

import android.content.Context

enum class RunMethod { Root, Normal }

class SetupPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("fasttool_setup", Context.MODE_PRIVATE)

    var completed: Boolean
        get() = prefs.getBoolean("completed", false)
        set(value) = prefs.edit().putBoolean("completed", value).apply()

    var method: RunMethod
        get() = if (prefs.getString("method", "normal") == "root") RunMethod.Root else RunMethod.Normal
        set(value) = prefs.edit().putString("method", if (value == RunMethod.Root) "root" else "normal").apply()

    var workDir: String
        get() = prefs.getString("work_dir", "") ?: ""
        set(value) = prefs.edit().putString("work_dir", value).apply()

    fun reset() {
        prefs.edit().clear().apply()
    }
}
