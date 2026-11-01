package gr.edusuite.pacer.model

/**
 * The starting sets Welcome offers on a fresh install (`docs/FEATURE_MODULES_BRIEF.md`, Presets). A preset is only a
 * set of modules; each module still turns on by itself later, and nothing turns one off.
 */
enum class Preset(val modules: Set<Module>) {
    JUST_PACING(emptySet()),
    PACING_AND_WORK(setOf(Module.WORK)),
    EVERYTHING(Module.entries.toSet());

    companion object {
        /** The preset whose modules are exactly [modules], or null for a custom choice. */
        fun of(modules: Set<Module>): Preset? = entries.firstOrNull { it.modules == modules }
    }
}
