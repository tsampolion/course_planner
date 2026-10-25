package gr.edusuite.pacer.model

import java.util.Locale

/** What a book is for. The abbreviation shown on lesson rows, Stats and printouts comes from the type, never the name. */
enum class BookType(val abbr: String) {
    STUDENT("SB"),
    WORKBOOK("WB"),
    GRAMMAR("GB"),
    WRITING("WrB"),
    SKILLS("SkB"),
    READER("R"),
    SUPPLEMENTARY("Supp"),
    /** Not chosen yet: the book keeps the initials worked out from its name. */
    UNCLASSIFIED(""),
}

/** The type a book's name makes clear, else [BookType.UNCLASSIFIED]. Used once, when an older file is read. */
fun guessBookType(name: String): BookType {
    val n = name.lowercase(Locale.ROOT)
    return when {
        "student" in n -> BookType.STUDENT
        "workbook" in n || "work book" in n -> BookType.WORKBOOK
        "grammar" in n -> BookType.GRAMMAR
        "writing" in n -> BookType.WRITING
        "skills" in n -> BookType.SKILLS
        "reader" in n -> BookType.READER
        else -> BookType.UNCLASSIFIED
    }
}

/**
 * The type to give a book read from a file. A stored [raw] type wins (an unknown one reads as unclassified); files from
 * before schema 15 have none, so a supplementary book gets that type and any other is matched by [guessBookType].
 */
fun readBookType(raw: String?, name: String, supplementary: Boolean): BookType {
    val stored = raw?.takeIf { it.isNotBlank() }
    val type = if (stored == null) (if (supplementary) BookType.SUPPLEMENTARY else guessBookType(name))
    else BookType.entries.firstOrNull { it.name == stored } ?: BookType.UNCLASSIFIED
    return if (supplementary) BookType.SUPPLEMENTARY else type
}

/** The book with [type] set; the supplementary flag follows it, and `include` and `countInLesson` stay. */
fun Book.withType(type: BookType): Book = copy(type = type, supplementary = type == BookType.SUPPLEMENTARY)

/** The book agreeing with itself: a supplementary one has that type, and that type means supplementary. */
fun Book.consistent(): Book = when {
    supplementary && type != BookType.SUPPLEMENTARY -> copy(type = BookType.SUPPLEMENTARY)
    !supplementary && type == BookType.SUPPLEMENTARY -> copy(supplementary = true)
    else -> this
}
