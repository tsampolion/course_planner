package gr.edusuite.pacer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gr.edusuite.pacer.R

/** Money, dates, counts and page numbers use the system monospace font, so figures line up (§3). */
val Mono: FontFamily = FontFamily.Monospace

// Font variation settings are still marked experimental in Compose; they're backed by the platform since API 26.
@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: FontWeight) =
    Font(R.font.outfit, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/**
 * Outfit (SIL Open Font License 1.1, third_party/outfit/OFL.txt), bundled as its variable font: one 110 KB file gives the
 * three weights the guide uses, smaller than three static files. minSdk 26 supports font variations.
 */
val Outfit = FontFamily(outfit(FontWeight.Normal), outfit(FontWeight.Medium), outfit(FontWeight.SemiBold))

private val Base = Typography()

private fun TextStyle.outfit(weight: FontWeight, size: Int, line: Int) =
    copy(fontFamily = Outfit, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun TextStyle.outfit(weight: FontWeight) = copy(fontFamily = Outfit, fontWeight = weight)

/** The EduSuite type scale (§3). */
internal val PacerTypography = Typography(
    displayLarge = Base.displayLarge.outfit(FontWeight.SemiBold),
    displayMedium = Base.displayMedium.outfit(FontWeight.SemiBold),
    displaySmall = Base.displaySmall.outfit(FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.outfit(FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.outfit(FontWeight.SemiBold, 28, 36),
    headlineSmall = Base.headlineSmall.outfit(FontWeight.Medium, 20, 26),
    titleLarge = Base.titleLarge.outfit(FontWeight.SemiBold, 22, 28),
    titleMedium = Base.titleMedium.outfit(FontWeight.Medium, 16, 22),
    titleSmall = Base.titleSmall.outfit(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.outfit(FontWeight.Normal, 16, 24),
    bodyMedium = Base.bodyMedium.outfit(FontWeight.Normal, 14, 20),
    bodySmall = Base.bodySmall.outfit(FontWeight.Normal, 12, 16),
    labelLarge = Base.labelLarge.outfit(FontWeight.Medium, 14, 20),
    labelMedium = Base.labelMedium.outfit(FontWeight.Medium, 12, 16),
    labelSmall = Base.labelSmall.outfit(FontWeight.Medium, 11, 16),
)
