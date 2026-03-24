package com.example.appcleanhouse.models

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CleanerAvailability(
    val cleanerId: String = "",
    val availabilityByDate: Map<String, List<String>> = emptyMap(),
    val updatedAt: Long = 0L
) {
    companion object {
        val DEFAULT_TIME_SLOTS = listOf("09:00 AM", "11:00 AM", "02:00 PM", "04:30 PM")
        private const val STORAGE_DATE_PATTERN = "yyyy-MM-dd"
        private const val DISPLAY_DATE_PATTERN = "MMM dd, yyyy"

        fun dateToStorageKey(date: Date): String {
            return SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.US).format(date)
        }

        fun storageKeyToDisplay(key: String): String {
            val parser = SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.US)
            val formatter = SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.getDefault())
            val parsedDate = parser.parse(key) ?: return key
            return formatter.format(parsedDate)
        }

        fun parseStorageKey(key: String): Date? {
            return SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.US).parse(key)
        }

        fun upcomingBookingDates(count: Int = 7): List<String> {
            val calendar = Calendar.getInstance()
            return buildList {
                repeat(count) {
                    add(dateToStorageKey(calendar.time))
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }

        fun upcomingEmptyAvailabilityMap(count: Int = 7): Map<String, List<String>> {
            return upcomingBookingDates(count).associateWith { emptyList() }
        }

        fun normalizeAvailabilityMap(
            source: Map<String, List<String>>
        ): Map<String, List<String>> {
            return source.mapNotNull { (date, slots) ->
                val normalizedSlots = slots.filter { it in DEFAULT_TIME_SLOTS }.distinct()
                if (date.isBlank()) null else date to normalizedSlots
            }.toMap()
        }

        fun availabilityFromLegacy(
            availableDays: List<String>,
            availableTimeSlots: List<String>,
            count: Int = 7
        ): Map<String, List<String>> {
            val normalizedDays = availableDays.toSet()
            val normalizedTimeSlots = availableTimeSlots
                .filter { it in DEFAULT_TIME_SLOTS }
                .ifEmpty { DEFAULT_TIME_SLOTS }
            val calendar = Calendar.getInstance()

            return buildMap {
                repeat(count) {
                    val dateKey = dateToStorageKey(calendar.time)
                    val dayKey = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "MONDAY"
                        Calendar.TUESDAY -> "TUESDAY"
                        Calendar.WEDNESDAY -> "WEDNESDAY"
                        Calendar.THURSDAY -> "THURSDAY"
                        Calendar.FRIDAY -> "FRIDAY"
                        Calendar.SATURDAY -> "SATURDAY"
                        Calendar.SUNDAY -> "SUNDAY"
                        else -> ""
                    }
                    put(dateKey, if (normalizedDays.isEmpty() || dayKey in normalizedDays) normalizedTimeSlots else emptyList())
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }

        fun mergeWithUpcomingDates(
            source: Map<String, List<String>>,
            count: Int = 7
        ): Map<String, List<String>> {
            val normalized = normalizeAvailabilityMap(source)
            return upcomingBookingDates(count).associateWith { date -> normalized[date].orEmpty() }
        }
    }
}