package com.efina.app.util

import android.content.Context
import android.os.Build
import com.efina.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ships uncaught exceptions to the backend's public error endpoint.
 *
 * The web client already posts to `POST /api/public/debug` (functions/api/public/debug.js),
 * which stores into `error_logs` with a 7-day retention trigger. The Android app
 * had no equivalent, so a crash on a tester's device produced nothing anyone
 * could read — the same blind spot that made the earlier Capacitor blank screen
 * take several rounds to diagnose.
 *
 * Request contract, read from the backend:
 *   body   { message: String (<=1000), stack?: String (<=5000), url?: String (<=500), level?: 'error'|'warn'|'info'|'debug' }
 *   response 200 { ok: true }
 *
 * Deliberately fire-and-forget on a background dispatcher: a reporter that can
 * itself fail the launch is worse than no reporter.
 */
object CrashReporter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Rate limit: one report per unique top stack frame per process. */
    private val seen = mutableSetOf<String>()

    fun install(context: Context?) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { report(error, thread) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun report(error: Throwable, thread: Thread? = null) {
        val frame = error.stackTrace.firstOrNull()?.toString() ?: error.javaClass.name
        synchronized(seen) {
            if (!seen.add(frame)) return
            if (seen.size > 20) seen.clear()
        }

        val payload = buildJson(error, thread)
        scope.launch {
            runCatching { post(payload) }
        }
    }

    private fun buildJson(error: Throwable, thread: Thread?): String {
        val device = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val header = "thread=${thread?.name ?: "?"} device=$device version=${BuildConfig.VERSION_NAME}"

        return buildString {
            append('{')
            append("\"message\":").append(quote(truncate("${error.javaClass.simpleName}: ${error.message}", 1000)))
            append(',')
            append("\"stack\":").append(
                quote(truncate(header + "\n" + error.stackTraceToString(), 4900))
            )
            append(',')
            append("\"url\":").append(quote(truncate("android://${BuildConfig.APPLICATION_ID}", 500)))
            append(',')
            append("\"level\":\"error\"")
            append('}')
        }
    }

    private fun post(json: String) {
        val url = URL("${BuildConfig.API_BASE_URL}public/debug")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(json) }
            connection.responseCode // force the request to complete
        } finally {
            connection.disconnect()
        }
    }

    private fun truncate(value: String?, max: Int): String {
        val v = value ?: ""
        return if (v.length <= max) v else v.substring(0, max)
    }

    /** Minimal JSON string escaping; the payload is built by hand to avoid a dependency. */
    private fun quote(value: String): String = buildString {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }
}
