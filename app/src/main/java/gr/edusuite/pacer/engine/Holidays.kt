package gr.edusuite.pacer.engine

import java.time.LocalDate

/** Orthodox Easter (Meeus' Julian algorithm), moved to the Gregorian calendar. Valid 1900-2099. */
fun orthodoxEaster(year: Int): LocalDate {
    val a = year % 4
    val b = year % 7
    val c = year % 19
    val d = (19 * c + 15) % 30
    val e = (2 * a + 4 * b - d + 34) % 7
    val month = (d + e + 114) / 31
    val day = (d + e + 114) % 31 + 1
    return LocalDate.of(year, month, day).plusDays(13)
}

/** Greek public holidays that fall between [from] and [to], inclusive, sorted by date. */
fun greekHolidays(from: LocalDate, to: LocalDate): List<Pair<LocalDate, String>> {
    val out = ArrayList<Pair<LocalDate, String>>()
    for (y in from.year..to.year) {
        val easter = orthodoxEaster(y)
        out += LocalDate.of(y, 1, 1) to "New Year's Day"
        out += LocalDate.of(y, 1, 6) to "Epiphany"
        out += easter.minusDays(48) to "Clean Monday"
        out += LocalDate.of(y, 3, 25) to "Independence Day"
        out += easter.minusDays(2) to "Good Friday"
        out += easter.plusDays(1) to "Easter Monday"
        out += LocalDate.of(y, 5, 1) to "Labour Day"
        out += easter.plusDays(50) to "Holy Spirit Monday"
        out += LocalDate.of(y, 10, 28) to "Ohi Day"
        out += LocalDate.of(y, 11, 17) to "Polytechnic Day"
        out += LocalDate.of(y, 12, 25) to "Christmas Day"
    }
    return out.filter { !it.first.isBefore(from) && !it.first.isAfter(to) }.sortedBy { it.first }
}
