package gr.edusuite.pacer.ui

import androidx.compose.runtime.staticCompositionLocalOf
import gr.edusuite.pacer.R
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.inUse

/** The teacher's modules plus the bricks the open year's data uses, so data already saved always shows. */
fun featuresOf(settings: Settings, data: AppData): Features {
    val years = listOfNotNull(data.year)
    return Features(settings.features().on, Brick.entries.filterTo(mutableSetOf()) { it.inUse(years) })
}

/** What the screens may show. Previews and tests outside [PacerApp] see every brick. */
val LocalFeatures = staticCompositionLocalOf { Features.ALL }

/** Group home's Due soon belongs to Work: it shows with Work, or while tests or homework are saved. */
fun dueSoonShown(f: Features): Boolean = f.has(Brick.HOMEWORK) || f.has(Brick.TESTS)

/** Print or save PDF on Class and School year belongs to Output, like the group ⋮'s Print. */
fun printShown(f: Features): Boolean = f.has(Brick.PRINT)

/** Stats' Pace needed opens My own pace, a Planning tool. */
fun ownPaceShown(f: Features): Boolean = f.has(Brick.OWN_PACE)

/* Hints and warnings never name a tool the teacher has off (FEATURE_MODULES_BRIEF, Fringe cases). */

/** Plan's one-time hint: "or plan it" only with the plan editor. */
fun lessonMenuHint(f: Features): Int = if (f.has(Brick.PLAN_EDITOR)) R.string.hint_lesson_menu else R.string.hint_lesson_menu_basic

/** Group settings' books hint: leaving books out and adding books need Book tools. */
fun groupBooksHint(f: Features): Int =
    if (f.has(Brick.OPTIONAL_BOOKS) || f.has(Brick.GROUP_BOOK_RULES)) R.string.group_settings_books_hint else R.string.group_settings_books_hint_basic

/** Own pace's "not enough lessons": setting pages as homework needs Work. */
fun infeasibleWarning(f: Features): Int = if (f.has(Brick.HOMEWORK)) R.string.warn_infeasible else R.string.warn_infeasible_basic
