package dev.fasttool.fastboot

import android.content.Context
import android.net.Uri
import android.os.Build
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Runs the bundled fastboot binary.
 *
 * Preferred locations (in order):
 * 1. nativeLibraryDir/libfastboot.so  (from jniLibs — always executable)
 * 2. Extracted copy in codeCacheDir with chmod 755
 * 3. Extracted copy in filesDir with chmod 755
 */
class FastbootRunner(private val context: Context) {

    private val tag = "FastbootRunner"

    suspend fun ensureBinary(): File = withContext(Dispatchers.IO) {
        // 1) System-installed native lib path (best)
        val nativeDir = context.applicationInfo.nativeLibraryDir
        if (nativeDir != null) {
            val fromJni = File(nativeDir, "libfastboot.so")
            if (fromJni.exists() && fromJni.canExecute()) {
                Log.i(tag, "Using jniLibs binary: ${fromJni.absolutePath}")
                return@withContext fromJni
            }
            // Also try plain name if someone put it there
            val plain = File(nativeDir, "fastboot")
            if (plain.exists() && plain.canExecute()) {
                return@withContext plain
            }
        }

        // 2) Extract from assets into codeCacheDir (often executable)
        val cacheBin = File(context.codeCacheDir, "fastboot")
        extractAssetIfNeeded(cacheBin)
        if (tryMakeExecutable(cacheBin)) {
            return@withContext cacheBin
        }

        // 3) filesDir fallback
        val filesBin = File(context.filesDir, "fastboot")
        extractAssetIfNeeded(filesBin)
        if (tryMakeExecutable(filesBin)) {
            return@withContext filesBin
        }

        throw IllegalStateException(
            "Cannot execute fastboot binary (Permission denied). " +
            "Rebuild with jniLibs arm64-v8a/libfastboot.so."
        )
    }

    private fun extractAssetIfNeeded(dest: File) {
        if (dest.exists() && dest.length() > 0) return

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val names = listOf(
            "libfastboot.so",
            "fastboot",
            "fastboot-$abi",
            "fastboot-arm64-v8a"
        )
        for (name in names) {
            try {
                context.assets.open(name).use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                Log.i(tag, "Extracted asset $name → ${dest.absolutePath}")
                return
            } catch (_: Exception) {
            }
        }
        // Also try copying from nativeLibraryDir if present but not executable flag
        val nativeDir = context.applicationInfo.nativeLibraryDir
        if (nativeDir != null) {
            val src = File(nativeDir, "libfastboot.so")
            if (src.exists()) {
                src.copyTo(dest, overwrite = true)
            }
        }
    }

    private fun tryMakeExecutable(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        try {
            file.setReadable(true, false)
            file.setExecutable(true, false)
            file.setWritable(true, true)
            try {
                Os.chmod(file.absolutePath, 493) // 0755
            } catch (_: Exception) {
            }
            if (file.canExecute()) return true
            // Probe by trying to run --help briefly is too heavy; trust canExecute
            return file.canExecute()
        } catch (e: Exception) {
            Log.w(tag, "chmod failed for ${file.absolutePath}", e)
            return false
        }
    }

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

    suspend fun run(vararg args: String): ExecResult = withContext(Dispatchers.IO) {
        val bin = ensureBinary()
        val cmd = listOf(bin.absolutePath) + args.toList()
        Log.i(tag, "exec: ${cmd.joinToString(" ")}")

        val pb = ProcessBuilder(cmd)
            .redirectErrorStream(false)
            .directory(context.cacheDir)
        pb.environment()["HOME"] = context.filesDir.absolutePath
        pb.environment()["TMPDIR"] = context.cacheDir.absolutePath

        val process = pb.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val code = process.waitFor()
        ExecResult(code, stdout.trim(), stderr.trim())
    }

    suspend fun flash(partition: String, imageFile: File): ExecResult {
        return run("flash", partition, imageFile.absolutePath)
    }

    suspend fun devices(): ExecResult = run("devices")

    suspend fun reboot(): ExecResult = run("reboot")
}
