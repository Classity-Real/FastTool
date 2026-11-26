package dev.fasttool.fastboot

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Runs the bundled fastboot binary.
 *
 * Place a static arm64 (and optionally armeabi-v7a) fastboot binary at:
 *   app/src/main/assets/fastboot
 *
 * On first use it is extracted to the app's private files dir and marked executable.
 */
class FastbootRunner(private val context: Context) {

    private val tag = "FastbootRunner"

    private val binaryFile: File
        get() = File(context.filesDir, "fastboot")

    /**
     * Ensure the fastboot binary is extracted and executable.
     * Returns the absolute path or throws if missing from assets.
     */
    suspend fun ensureBinary(): File = withContext(Dispatchers.IO) {
        if (binaryFile.exists() && binaryFile.canExecute() && binaryFile.length() > 0) {
            return@withContext binaryFile
        }

        // Prefer ABI-specific asset if present, else plain "fastboot"
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val assetNames = listOf(
            "fastboot-$abi",
            "fastboot-arm64-v8a",
            "fastboot"
        )

        var extracted = false
        for (name in assetNames) {
            try {
                context.assets.open(name).use { input ->
                    FileOutputStream(binaryFile).use { output ->
                        input.copyTo(output)
                    }
                }
                extracted = true
                Log.i(tag, "Extracted $name → ${binaryFile.absolutePath}")
                break
            } catch (_: Exception) {
                // try next name
            }
        }

        if (!extracted || !binaryFile.exists() || binaryFile.length() == 0L) {
            throw IllegalStateException(
                "fastboot binary not found in assets. " +
                "Add a static binary as app/src/main/assets/fastboot (arm64)."
            )
        }

        binaryFile.setReadable(true)
        binaryFile.setExecutable(true, false)
        binaryFile
    }

    /**
     * Copy a content Uri to a temporary file the binary can read.
     */
    suspend fun materializeUri(uri: Uri, preferredName: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "flash_images").also { it.mkdirs() }
        val out = File(dir, preferredName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot open $uri")
        out
    }

    data class ExecResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    /**
     * Run: fastboot <args...>
     */
    suspend fun run(vararg args: String): ExecResult = withContext(Dispatchers.IO) {
        val bin = ensureBinary()
        val cmd = listOf(bin.absolutePath) + args.toList()
        Log.i(tag, "exec: ${cmd.joinToString(" ")}")

        val process = ProcessBuilder(cmd)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val code = process.waitFor()

        ExecResult(code, stdout.trim(), stderr.trim())
    }

    suspend fun flash(partition: String, imageFile: File): ExecResult {
        return run("flash", partition, imageFile.absolutePath)
    }

    suspend fun erase(partition: String): ExecResult {
        return run("erase", partition)
    }

    suspend fun devices(): ExecResult {
        return run("devices")
    }

    suspend fun getvar(name: String): ExecResult {
        return run("getvar", name)
    }

    suspend fun reboot(): ExecResult {
        return run("reboot")
    }
}
