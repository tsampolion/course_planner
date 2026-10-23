package gr.edusuite.pacer.model

/** Selecting classes moves them here; unselected classes retain their original relative order. */
fun SchoolYear.withCollection(value: ClassCollection): SchoolYear {
    val valid = classes.mapTo(HashSet()) { it.id }
    val selected = value.copy(name = value.name.trim(), classIds = value.classIds.filterTo(linkedSetOf()) { it in valid })
    if (selected.name.isBlank()) return this
    val present = collections.any { it.id == selected.id }
    val next = collections.map { c -> if (c.id == selected.id) selected else c.copy(classIds = c.classIds - selected.classIds) }
    return copy(collections = if (present) next else next + selected)
}

/** Deletes the section only, keeping every class. */
fun SchoolYear.withoutCollection(id: String): SchoolYear = copy(collections = collections.filter { it.id != id })
