package gr.edusuite.pacer.ui

import androidx.compose.runtime.staticCompositionLocalOf
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
