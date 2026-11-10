package com.scripturedaily.features

import com.scripturedaily.data.Verse

/** Full-screen destinations opened from Home and Discover. */
sealed interface Overlay {
    data object Truths : Overlay
    data object Challenges : Overlay
    data object Journal : Overlay
<<<<<<< HEAD
    data object Widgets : Overlay
=======
    /** Journal opened straight on the Reflections tab. */
    data object Reflections : Overlay
    data object Widgets : Overlay
    /** Debug-only: loads every ad format with Google test units. */
    data object AdsLab : Overlay
>>>>>>> settings_redesign
    data object Stories : Overlay
    /** Studio hub: Share Studio (images, wallpapers) and Video Studio. [video] opens the Video tab first. */
    data class Studio(val verse: Verse, val video: Boolean = false) : Overlay
    data class StoryForVerse(val verseId: Int) : Overlay
}
