package uz.choyxona.app.ui.util

/**
 * Ordering rules for room/place names, mirroring `room_ordering.py` on the server.
 *
 * Every XONA comes before every SO'RI, and inside a type the numbers are ordered
 * numerically (1, 2, ... 10, 11) instead of lexicographically. The apostrophe in
 * SO'RI is typed with several different characters, so names are normalized
 * before the type is detected.
 */

// Straight quote, curly quotes, Uzbek okina/tortoq, backtick, acute accent.
private val APOSTROPHES = setOf('\'', '‘', '’', 'ʻ', 'ʼ', '`', '´')

private const val TYPE_XONA = 0
private const val TYPE_SORI = 1
private const val TYPE_UNKNOWN = 2

// Rooms without a number go after the numbered ones of the same type.
private const val UNNUMBERED = Int.MAX_VALUE

private val NUMBER_REGEX = Regex("\\d+")

fun normalizeRoomName(name: String?): String =
    (name ?: "").uppercase().filterNot { it in APOSTROPHES }.trim()

private fun roomTypePriority(normalizedName: String): Int = when {
    normalizedName.contains("XONA") -> TYPE_XONA
    normalizedName.contains("SORI") -> TYPE_SORI
    else -> TYPE_UNKNOWN
}

private fun roomNumber(normalizedName: String): Int =
    NUMBER_REGEX.find(normalizedName)?.value?.toIntOrNull() ?: UNNUMBERED

/** Comparator over anything that can expose a room name. */
fun <T> roomNameComparator(nameOf: (T) -> String?): Comparator<T> =
    compareBy(
        { roomTypePriority(normalizeRoomName(nameOf(it))) },
        { roomNumber(normalizeRoomName(nameOf(it))) },
        { normalizeRoomName(nameOf(it)) }
    )
