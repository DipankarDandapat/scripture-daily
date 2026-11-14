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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    val isDemo: Boolean
) {
    fun reference() = "${book?.let { "$it · " } ?: ""}Chapter $chapter · Verse $verseNumber"
    fun toVerse() = Verse(id, scripture, religion, book, chapter, verseNumber, language, text, translation, source, "", isDemo, scriptureId = scriptureId, languageId = languageId)
}

fun Verse.toCached(scriptureId: Int, languageId: Int) = CachedVerse(
    id, this.scriptureId ?: scriptureId, this.languageId ?: languageId, scripture, religion, book, chapter, verseNumber, language, text, translation, source, isDemo
)

@Dao
interface VerseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(verse: CachedVerse)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(verses: List<CachedVerse>)
    @Query("SELECT * FROM cached_verses WHERE id = :id LIMIT 1") suspend fun find(id: Int): CachedVerse?
    @Query("SELECT * FROM cached_verses WHERE scriptureId = :scriptureId AND languageId = :languageId ORDER BY id DESC LIMIT 1") suspend fun matching(scriptureId: Int, languageId: Int): CachedVerse?
    @Query("SELECT * FROM cached_verses WHERE scriptureId = :scriptureId AND languageId = :languageId ORDER BY id") suspend fun forCombo(scriptureId: Int, languageId: Int): List<CachedVerse>
}

// ── Catalog cache (religions + languages, TTL 24 h) ───────────────────────────
// type: "religion" | "language" | "scripture"
@Entity(tableName = "cached_catalog")
data class CachedCatalogItem(
    @PrimaryKey val key: String,   // e.g. "religion_1", "language_2"
    val type: String,
    val itemId: Int,
    val name: String,
    val code: String,
    val religion: String?,         // non-null for scriptures
    val scripturesJson: String?,   // JSON array for religions: "[{id,name,code},…]"
    val fetchedAt: Long = System.currentTimeMillis()
)

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(items: List<CachedCatalogItem>)
    @Query("SELECT * FROM cached_catalog WHERE type = :type ORDER BY itemId") suspend fun byType(type: String): List<CachedCatalogItem>
    @Query("SELECT MIN(fetchedAt) FROM cached_catalog WHERE type = :type") suspend fun oldestFetch(type: String): Long?
    @Query("DELETE FROM cached_catalog WHERE type = :type") suspend fun deleteByType(type: String)
}

@Database(entities = [CachedVerse::class, CachedCatalogItem::class], version = 6, exportSchema = false)
abstract class ScriptureDatabase : RoomDatabase() {
    abstract fun verses(): VerseDao
    abstract fun catalog(): CatalogDao
    companion object {
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop favorite/read tracking columns — favorites are now backend-only
                db.execSQL("""
                    CREATE TABLE cached_verses_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        scriptureId INTEGER NOT NULL,
                        languageId INTEGER NOT NULL,
                        scripture TEXT NOT NULL,
                        religion TEXT NOT NULL,
                        book TEXT,
                        chapter INTEGER NOT NULL,
                        verseNumber INTEGER NOT NULL,
                        language TEXT NOT NULL,
                        text TEXT NOT NULL,
                        translation TEXT,
                        source TEXT NOT NULL,
                        isDemo INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO cached_verses_new (id,scriptureId,languageId,scripture,religion,book,chapter,verseNumber,language,text,translation,source,isDemo) SELECT id,scriptureId,languageId,scripture,religion,book,chapter,verseNumber,language,text,translation,source,isDemo FROM cached_verses")
                db.execSQL("DROP TABLE cached_verses")
                db.execSQL("ALTER TABLE cached_verses_new RENAME TO cached_verses")
            }
        }

        @Volatile private var instance: ScriptureDatabase? = null
        fun get(context: Context): ScriptureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ScriptureDatabase::class.java, "scripture_daily.db")
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
