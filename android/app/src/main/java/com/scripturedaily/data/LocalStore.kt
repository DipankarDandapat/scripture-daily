package com.scripturedaily.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_verses")
data class CachedVerse(
    @PrimaryKey val id: Int,
    val scriptureId: Int,
    val languageId: Int,
    val scripture: String,
    val religion: String,
    val book: String?,
    val chapter: Int,
    val verseNumber: Int,
    val language: String,
    val text: String,
    val translation: String?,
    val source: String,
    val isDemo: Boolean,
    val savedAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false
) {
    fun reference() = "${book?.let { "$it · " } ?: ""}Chapter $chapter · Verse $verseNumber"
    fun toVerse() = Verse(id, scripture, religion, book, chapter, verseNumber, language, text, translation, source, "", isDemo)
}

fun Verse.toCached(scriptureId: Int, languageId: Int, favorite: Boolean = false, savedAt: Long = System.currentTimeMillis()) = CachedVerse(
    id, scriptureId, languageId, scripture, religion, book, chapter, verseNumber, language, text, translation, source, isDemo, savedAt, favorite
)

@Dao
interface VerseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(verse: CachedVerse)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(verses: List<CachedVerse>)
    @Query("SELECT * FROM cached_verses WHERE id = :id LIMIT 1") suspend fun find(id: Int): CachedVerse?
    @Query("SELECT * FROM cached_verses WHERE favorite = 1 ORDER BY savedAt DESC") fun observeFavorites(): Flow<List<CachedVerse>>
    @Query("SELECT * FROM cached_verses WHERE favorite = 1 ORDER BY savedAt DESC") suspend fun favorites(): List<CachedVerse>
    @Query("SELECT * FROM cached_verses WHERE scriptureId = :scriptureId AND languageId = :languageId ORDER BY savedAt DESC LIMIT 1") suspend fun matching(scriptureId: Int, languageId: Int): CachedVerse?
    @Query("UPDATE cached_verses SET favorite = :favorite WHERE id = :id") suspend fun setFavorite(id: Int, favorite: Boolean)
}

@Database(entities = [CachedVerse::class], version = 1, exportSchema = false)
abstract class ScriptureDatabase : RoomDatabase() {
    abstract fun verses(): VerseDao
    companion object {
        @Volatile private var instance: ScriptureDatabase? = null
        fun get(context: Context): ScriptureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ScriptureDatabase::class.java, "scripture_daily.db").fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
