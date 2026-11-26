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
 * Runs the bundled fastboot binary shipped as jniLibs/arm64-v8a/libfastboot.so
 *
 * Modern Android often blocks exec from app data dirs (error 13).
 * Strategy:
 * 1. Use extracted nativeLibraryDir/libfastboot.so (legacy packaging)
 * 2. Fall back to running via /system/bin/linker64
 */
class FastbootRunner(private val context: Context) {

    private val tag = "FastbootRunner"

    suspend fun ensureBinary(): File = withContext(Dispatchers.IO) {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        if (nativeDir != null) {
            val so = File(nativeDir, "libfastboot.so")
            if (so.exists() && so.length() > 0) {
                Log.i(tag, "Found native lib: ${so.absolutePath} size=${so.length()}")
                return@withContext so
            }
            Log.w(tag, "nativeLibraryDir has no libfastboot.so: $nativeDir contents=${File(nativeDir).list()?.joinToString()}")
        }

        // Last resort: extract asset into codeCache and hope
        val dest = File(context.codeCacheDir, "libfastboot.so")
        extractFromAssets(dest)
        if (dest.exists() && dest.length() > 0) {
            try {
                Os.chmod(dest.absolutePath, 493) // 0755
            } catch (_: Exception) {
            }
            dest.setExecutable(true, false)
            return@withContext dest
        }

        throw IllegalStateException(
            "libfastboot.so not found. Rebuild APK with jniLibs + useLegacyPackaging."
        )
    }

    private fun extractFromAssets(dest: File) {
        if (dest.exists() && dest.length() > 0) return
        val names = listOf("libfastboot.so", "fastboot", "fastboot-arm64-v8a")
        for (name in names) {
            try {
                context.assets.open(name).use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                Log.i(tag, "Extracted $name → ${dest.absolutePath}")
                return
            } catch (_: Exception) {
            }
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

        // Try direct exec first
        val direct = tryExec(listOf(bin.absolutePath) + args.toList())
        if (direct != null && !isPermissionDenied(direct)) {
            return@withContext direct
        }

        // Fall back: run through dynamic linker (works when noexec on data)
        val linker = findLinker()
        if (linker != null) {
            Log.i(tag, "Retry via linker: $linker")
            val viaLinker = tryExec(listOf(linker, bin.absolutePath) + args.toList())
            if (viaLinker != null) return@withContext viaLinker
        }

        direct ?: ExecResult(
            13,
            "",
            "Permission denied executing ${bin.absolutePath}. " +
                "Install a build that extracts jniLibs (useLegacyPackaging)."
        )
    }

    private fun isPermissionDenied(r: ExecResult): Boolean {
        val msg = (r.stderr + r.stdout).lowercase()
        return r.exitCode == 13 || msg.contains("permission denied") || msg.contains("error=13")
    }

    private fun findLinker(): String? {
        val candidates = listOf(
            "/system/bin/linker64",
            "/system/bin/linker"
        )
        return candidates.firstOrNull { File(it).exists() }
    }

    private fun tryExec(cmd: List<String>): ExecResult? {
        return try {
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
        } catch (e: Exception) {
            Log.w(tag, "exec failed: ${e.message}")
            ExecResult(13, "", e.message ?: "exec failed")
        }
    }

    suspend fun flash(partition: String, imageFile: File): ExecResult {
        return run("flash", partition, imageFile.absolutePath)
    }

    suspend fun devices(): ExecResult = run("devices")

    suspend fun reboot(): ExecResult = run("reboot")
}
