package com.scripturedaily.data

import com.google.gson.annotations.SerializedName
import com.scripturedaily.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

data class Verse(
    val id: Int,
    val scripture: String,
    val religion: String,
    val book: String?,
    val chapter: Int,
    @SerializedName("verse_number") val verseNumber: Int,
    val language: String,
    val text: String,
    val translation: String?,
    val source: String,
    val license: String,
    @SerializedName("is_demo") val isDemo: Boolean
) {
    fun reference(): String = "${book?.let { "$it · " } ?: ""}Chapter $chapter · Verse $verseNumber"
}

data class CatalogItem(val id: Int, val name: String, val code: String, val religion: String? = null)

interface ScriptureApi {
    @GET("verses/today") suspend fun today(@Query("religion_id") religion: Int, @Query("language_id") language: Int, @Query("scripture_id") scripture: Int): Verse
    @GET("verses/random") suspend fun random(@Query("religion_id") religion: Int, @Query("language_id") language: Int, @Query("scripture_id") scripture: Int): Verse
    @GET("verses/search") suspend fun search(@Query("q") query: String, @Query("language_id") language: Int? = null): List<Verse>
    @GET("religions") suspend fun religions(): List<CatalogItem>
    @GET("languages") suspend fun languages(): List<CatalogItem>
    @GET("scriptures") suspend fun scriptures(@Query("religion_id") religion: Int): List<CatalogItem>
}

object ApiClient {
    val api: ScriptureApi by lazy {
        Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(ScriptureApi::class.java)
    }
}
