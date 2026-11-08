package com.efina.app.util

import android.content.Context
import android.os.Build
import com.efina.app.BuildConfig
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
 * Posts are blocking with a short timeout on the uncaught-exception thread so the
 * report is sent before the process terminates. A reporter that blocks the crash
 * thread for a few seconds is acceptable; a reporter that silently drops reports
 * defeats its purpose.
 */
object CrashReporter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Rate limit: one report per unique top stack frame per process. */
    private val seen = mutableSetOf<String>()

    fun install(context: Context?) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            report(error, thread)
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
        post(payload)
    }

    private fun buildJson(error: Throwable, thread: Thread?): String {
        val device = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val header = "thread=${thread?.name ?: "?"} device=$device version=${BuildConfig.VERSION_NAME}"

        // Sanitize user input: error.message can contain transaction descriptions,
        // category names or other PII. Use only the exception class name plus a
        // truncated, sanitized message.
        val safeMessage = sanitize(error.message ?: "")
        val safeStack = sanitize(error.stackTraceToString())

        return buildString {
            append('{')
            append("\"message\":").append(quote(truncate("${error.javaClass.simpleName}: $safeMessage", 1000)))
            append(',')
            append("\"stack\":").append(
                quote(truncate(header + "\n" + safeStack, 2000))
            )
            append(',')
            append("\"url\":").append(quote(truncate("android://${BuildConfig.APPLICATION_ID}", 500)))
            append(',')
            append("\"level\":\"error\"")
            append('}')
        }
    }

    /** Strips potential user input from error messages/stack traces before sending. */
    private fun sanitize(value: String): String {
        return value
            .replace(Regex("""["'`]"""), "") // strip quotes that could inject JSON
            .take(500) // aggressive truncation to limit any residual user input
    }

    private fun post(json: String) {
        val url = URL("${BuildConfig.API_BASE_URL}public/debug")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 5_000
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

    private fun truncate(value: String, max: Int): String {
        return if (value.length <= max) value else value.substring(0, max)
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
