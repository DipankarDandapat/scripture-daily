package com.scripturedaily.data

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.annotations.SerializedName
import com.scripturedaily.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

data class ScriptureItem(val id: Int, val name: String, val code: String)

data class ReligionItem(val id: Int, val name: String, val code: String, val scriptures: List<ScriptureItem> = emptyList())

data class HealthResponse(val status: String)

data class TokenOut(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class AuthInput(val email: String, val password: String)

data class PreferenceInput(
    @SerializedName("religion_id") val religionId: Int,
    @SerializedName("language_id") val languageId: Int,
    @SerializedName("scripture_ids") val scriptureIds: String,  // comma-separated e.g. "2,3"
    val frequency: String = "daily",
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean = false
)

data class DeviceRegInput(
    @SerializedName("device_token") val deviceToken: String,
    val platform: String = "android",
    @SerializedName("device_model") val deviceModel: String? = null
)

data class FeedbackInput(
    val rating: Int,
    val comment: String?,
    val consent: Boolean
)

data class FavoriteInput(
    @SerializedName("verse_id") val verseId: Int
)

data class ChapterVerse(
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
    fun toVerse() = Verse(id, scripture, religion, book, chapter, verseNumber, language, text, translation, source, license, isDemo)
}

data class PreferenceOut(
    @SerializedName("religion_id") val religionId: Int,
    @SerializedName("language_id") val languageId: Int,
    @SerializedName("scripture_ids") val scriptureIds: String,
    val frequency: String,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean
)

interface ScriptureApi {
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    // Auth — no token needed
    @POST("auth/register")
    suspend fun register(@Body body: AuthInput): Response<TokenOut>

    @POST("auth/login")
    suspend fun login(@Body body: AuthInput): Response<TokenOut>

    // Catalog — public
    @GET("religions") suspend fun religions(): List<ReligionItem>
    @GET("languages") suspend fun languages(): List<CatalogItem>
    @GET("scriptures") suspend fun scriptures(@Query("religion_id") religion: Int): List<CatalogItem>

    // Verses — token auto-attached by interceptor
    @GET("verses/today")
    suspend fun today(): Verse

    @GET("verses/random")
    suspend fun random(
        @Query("religion_id") religion: Int,
        @Query("language_id") language: Int,
        @Query("scripture_id") scripture: Int
    ): Verse

    @GET("verses/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("language_id") languageId: Int? = null,
        @Query("scripture_id") scriptureId: Int? = null
    ): List<Verse>

    @GET("verses/today/history")
    suspend fun todayHistory(@Query("count") count: Int = 7): List<Verse>

    @GET("scriptures/{scripture_id}/chapters")
    suspend fun scriptureChapters(
        @Path("scripture_id") scriptureId: Int,
        @Query("language_id") languageId: Int? = null
    ): List<Int>

    @GET("scriptures/{scripture_id}/chapters/{chapter}/verses")
    suspend fun chapterVerses(
        @Path("scripture_id") scriptureId: Int,
        @Path("chapter") chapter: Int,
        @Query("language_id") languageId: Int? = null
    ): List<Verse>

    // User — token auto-attached by interceptor
    @GET("users/preferences")
    suspend fun getPreferences(): Response<PreferenceOut>

    @PUT("users/preferences")
    suspend fun savePreferences(@Body body: PreferenceInput): Response<PreferenceOut>

    @GET("favorites")
    suspend fun getFavorites(): List<Verse>

    @POST("favorites")
    suspend fun addFavorite(@Body body: FavoriteInput): Response<Unit>

    @DELETE("favorites/{verse_id}")
    suspend fun removeFavorite(@Path("verse_id") verseId: Int): Response<Unit>

    @POST("devices")
    suspend fun registerDevice(@Body body: DeviceRegInput): Response<Unit>

    @POST("feedback")
    suspend fun submitFeedback(@Body body: FeedbackInput): Response<Unit>
}

object ApiClient {
    private var tokenProvider: (() -> String?)? = null

    fun init(context: Context, getToken: () -> String?) {
        tokenProvider = getToken
        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider?.invoke()
            val req = if (token != null)
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            else
                chain.request()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(ChuckerInterceptor.Builder(context).build())
            .build()
        _api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScriptureApi::class.java)
    }

    private lateinit var _api: ScriptureApi
    val api: ScriptureApi get() = _api
}
