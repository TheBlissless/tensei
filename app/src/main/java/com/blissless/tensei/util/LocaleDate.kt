package com.blissless.tensei.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Locale-aware date helpers used across the app.
 *
 * The output style depends on the active app language:
 *  - German: `8. Oktober 2026`
 *  - English: `8th October, 2026`
 *
 * The locale should be read from the current resources
 * (`context.resources.configuration.locales[0]`) so the per-app language set in
 * [AppLocale] is respected rather than the device default.
 */
object LocaleDate {

    fun localeOf(context: Context): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    fun isGerman(locale: Locale): Boolean = locale.language == "de"

    /** English ordinal suffix: 1 -> "1st", 2 -> "2nd", 11 -> "11th". */
    fun ordinal(day: Int): String {
        val suffix = when {
            day % 100 in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }

    /** German "8." / English "8th". */
    fun dayOrdinal(day: Int, locale: Locale): String =
        if (isGerman(locale)) "$day." else ordinal(day)

    /** German "8. Oktober 2026" / English "8th October, 2026". */
    fun formatLong(millis: Long, locale: Locale): String {
        val date = Date(millis)
        val day = SimpleDateFormat("d", locale).format(date).toInt()
        val month = SimpleDateFormat("MMMM", locale).format(date)
        val year = SimpleDateFormat("yyyy", locale).format(date)
        return if (isGerman(locale)) "$day. $month $year" else "${ordinal(day)} $month, $year"
    }

    /** German "8. Oktober 2026, 14:30" / English "8th October, 2026 - 14:30". */
    fun formatLongWithTime(millis: Long, locale: Locale): String {
        val time = SimpleDateFormat("HH:mm", locale).format(Date(millis))
        return if (isGerman(locale)) "${formatLong(millis, locale)}, $time"
        else "${formatLong(millis, locale)} - $time"
    }

    fun formatMonthAbbrev(millis: Long, locale: Locale): String =
        SimpleDateFormat("MMM", locale).format(Date(millis))

    fun formatDayNumber(millis: Long, locale: Locale): Int =
        SimpleDateFormat("d", locale).format(Date(millis)).toInt()

    /** Localized full day names, index 0 = Sunday .. 6 = Saturday. */
    fun dayNames(locale: Locale): List<String> = dayNamesFor("EEEE", locale)

    /** Localized short day names, index 0 = Sunday .. 6 = Saturday. */
    fun dayAbbreviations(locale: Locale): List<String> = dayNamesFor("EEE", locale)

    private fun dayNamesFor(pattern: String, locale: Locale): List<String> {
        val format = SimpleDateFormat(pattern, locale)
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.SUNDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        return (0..6).map {
            val name = format.format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            name
        }
    }

    /**
     * Formats an ISO `yyyy-MM-dd` string (e.g. "2026-10-08") using the given
     * locale. Returns the input unchanged when it cannot be parsed.
     */
    fun formatIsoDate(dateStr: String, locale: Locale): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return dateStr
            val cal = Calendar.getInstance()
            cal.clear()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            formatLong(cal.timeInMillis, locale)
        } catch (_: Exception) {
            dateStr
        }
    }
}
