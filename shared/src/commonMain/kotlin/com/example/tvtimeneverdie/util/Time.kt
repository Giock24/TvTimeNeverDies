package com.example.tvtimeneverdie.util

expect fun currentTimeMillis(): Long

private const val MILLIS_PER_DAY = 86_400_000L

private val WEEKDAY_NAMES = listOf(
    "Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato",
)

private fun floorDiv(a: Long, b: Long): Long {
    val q = a / b
    return if ((a % b != 0L) && ((a < 0) != (b < 0))) q - 1 else q
}

private fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b

/** Numero di giorni trascorsi dall'epoch (1970-01-01), secondo l'algoritmo di Howard Hinnant. */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = floorDiv(if (y >= 0) y else y - 399, 400)
    val yoe = y - era * 400
    val mp = ((month + 9) % 12).toLong()
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** Converte una data ISO "yyyy-MM-dd" nel numero di giorni dall'epoch, o null se non parsabile. */
fun isoDateToEpochDay(date: String): Long? {
    val parts = date.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return daysFromCivil(year, month, day)
}

fun todayEpochDay(): Long = floorDiv(currentTimeMillis(), MILLIS_PER_DAY)

private fun weekdayName(epochDay: Long): String {
    val index = floorMod(epochDay + 4, 7).toInt()
    return WEEKDAY_NAMES[index]
}

/** Etichetta relativa a oggi per un giorno (IERI/OGGI/DOMANI/nome del giorno) usata nella lista "In Arrivo". */
fun dateBucketLabel(epochDay: Long, todayEpochDay: Long): String = when (epochDay - todayEpochDay) {
    -1L -> "IERI"
    0L -> "OGGI"
    1L -> "DOMANI"
    else -> weekdayName(epochDay).uppercase()
}
