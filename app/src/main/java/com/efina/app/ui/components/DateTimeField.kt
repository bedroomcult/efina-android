package com.efina.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efina.app.ui.theme.EfinaCorners
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Date and time are chosen with the platform pickers, not typed.
 *
 * Free-text fields let a user submit "2026-13-45" or "25:99", which the backend
 * rejects with a validation error; the pickers cannot produce an invalid value, and
 * they are the control users expect on Android.
 *
 * The widget works in the string form the API wants (`YYYY-MM-DD`, `HH:mm:ss`) and
 * converts at the boundary, so callers keep the same state they had before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    date: String,
    time: String,
    onDate: (String) -> Unit,
    onTime: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showDate = true },
            shape = EfinaCorners.menu,
            modifier = Modifier.weight(1.4f)
        ) { Text(friendlyDate(date)) }

        OutlinedButton(
            onClick = { showTime = true },
            shape = EfinaCorners.menu,
            modifier = Modifier.weight(1f)
        ) { Text(time.take(5).ifBlank { "00:00" }) }
    }

    if (showDate) {
        val initial = parseDate(date)?.time ?: System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDate(formatDate(it)) }
                        showDate = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val parts = time.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTime(String.format(Locale.US, "%02d:%02d:00", state.hour, state.minute))
                        showTime = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            title = { Text("Time") },
            text = { TimePicker(state = state) }
        )
    }
}

private fun parseDate(value: String): Date? = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)
}.getOrNull()

private fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

/** "Tue, 2 Nov 2026" — the picker's own date is already visible, so show it plainly. */
private fun friendlyDate(value: String): String {
    val parsed = parseDate(value) ?: return value.ifBlank { "Pick a date" }
    return SimpleDateFormat("EEE, d MMM yyyy", Locale.US).format(parsed)
}
