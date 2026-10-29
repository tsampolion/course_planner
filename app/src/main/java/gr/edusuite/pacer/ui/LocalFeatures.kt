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
