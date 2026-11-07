package com.scripturedaily.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesPersistenceTest {
    private lateinit var database: ScriptureDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScriptureDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savedVerseAppearsInFavoritesAndCanBeRemoved() = runBlocking {
        val dao = database.verses()
        val verse = CachedVerse(
            id = 47,
            scriptureId = 1,
            languageId = 1,
            scripture = "Bhagavad Gita",
            religion = "Hinduism",
            book = null,
            chapter = 2,
            verseNumber = 47,
            language = "en",
            text = "Demo verse text",
            translation = null,
            source = "test",
            isDemo = true
        )

        dao.save(verse.copy(favorite = true))
        assertEquals(listOf(47), dao.observeFavorites().first().map { it.id })

        dao.setFavorite(47, false)
        assertEquals(emptyList<Int>(), dao.observeFavorites().first().map { it.id })
    }
}
