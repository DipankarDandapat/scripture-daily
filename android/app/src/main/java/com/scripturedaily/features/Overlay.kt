package com.scripturedaily.features

import com.scripturedaily.data.Verse

/** Full-screen destinations opened from Home and Discover. */
sealed interface Overlay {
    data object Truths : Overlay
    data object Challenges : Overlay
    data object Journal : Overlay
    data object Widgets : Overlay
    data object Stories : Overlay
    data class Studio(val verse: Verse) : Overlay
    data class Media(val verse: Verse) : Overlay
    data class StoryForVerse(val verseId: Int) : Overlay
}
