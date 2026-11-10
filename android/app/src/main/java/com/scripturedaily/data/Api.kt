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
    @SerializedName("is_demo") val isDemo: Boolean,
    val topics: List<String>? = null,
<<<<<<< HEAD
    @SerializedName("explanation_available") val explanationAvailable: Boolean? = null
=======
    @SerializedName("explanation_available") val explanationAvailable: Boolean? = null,
    @SerializedName("scripture_id") val scriptureId: Int? = null,
    @SerializedName("language_id") val languageId: Int? = null
>>>>>>> settings_redesign
) {
    fun reference(): String = "${book?.let { "$it · " } ?: ""}Chapter $chapter · Verse $verseNumber"

    /** Scripture + reference without repeating the name when the book is the scripture itself (e.g. Bhagavad Gita). */
    fun citation(): String = if (book != null && book.equals(scripture, ignoreCase = true)) reference() else "$scripture · ${reference()}"
}

data class CatalogItem(val id: Int, val name: String, val code: String, val religion: String? = null, @SerializedName("native_script") val nativeScript: String = "")

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
<<<<<<< HEAD
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean = false
=======
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean = false,
    @SerializedName("wallpaper_mode") val wallpaperMode: String = "verse_and_image"
>>>>>>> settings_redesign
)

data class DeviceRegInput(
    @SerializedName("device_token") val deviceToken: String,
    val platform: String = "android",
<<<<<<< HEAD
    @SerializedName("device_model") val deviceModel: String? = null
=======
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("fcm_token") val fcmToken: String? = null
)

data class FcmTokenInput(
    @SerializedName("fcm_token") val fcmToken: String
>>>>>>> settings_redesign
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

<<<<<<< HEAD
=======
data class ActiveVerseData(
    @SerializedName("verse_id") val verseId: Int,
    @SerializedName("scripture_id") val scriptureId: Int,
    @SerializedName("tradition_id") val traditionId: Int,
    @SerializedName("language_id") val languageId: Int,
    @SerializedName("selected_at") val selectedAt: String,
    @SerializedName("next_update_at") val nextUpdateAt: String,
    @SerializedName("selection_mode") val selectionMode: String,
    val verse: Verse
)
data class ActiveVerseResponse(val success: Boolean, val data: ActiveVerseData)

>>>>>>> settings_redesign
data class PreferenceOut(
    @SerializedName("religion_id") val religionId: Int,
    @SerializedName("language_id") val languageId: Int,
    @SerializedName("scripture_ids") val scriptureIds: String,
    val frequency: String,
<<<<<<< HEAD
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean
)

=======
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerializedName("wallpaper_mode") val wallpaperMode: String = "verse_and_image"
)

// ── Journal Reflection ───────────────────────────────────────────────────────
data class ReflectionDto(
    val id: Int, @SerializedName("verse_id") val verseId: Int?, @SerializedName("question_id") val questionId: Int?,
    @SerializedName("reflection_date") val reflectionDate: String, val answer: String, val question: String,
    @SerializedName("verse_text") val verseText: String, @SerializedName("verse_reference") val verseReference: String,
    val scripture: String, val language: String, @SerializedName("created_at") val createdAt: String, @SerializedName("updated_at") val updatedAt: String
)
data class TodayReflectionDto(
    @SerializedName("reflection_date") val reflectionDate: String, @SerializedName("verse_id") val verseId: Int,
    @SerializedName("verse_text") val verseText: String, @SerializedName("verse_reference") val verseReference: String,
    val scripture: String, val language: String, @SerializedName("question_id") val questionId: Int?, val question: String,
    @SerializedName("is_fallback") val isFallback: Boolean = false, @SerializedName("max_length") val maxLength: Int = 500,
    val existing: ReflectionDto? = null
)
data class ReflectionListDto(val items: List<ReflectionDto>, val total: Int, val limit: Int, val offset: Int, @SerializedName("has_more") val hasMore: Boolean)
data class ReflectionSaveBody(@SerializedName("verse_id") val verseId: Int, val answer: String, @SerializedName("reflection_date") val reflectionDate: String? = null)
data class ReflectionUpdateBody(val answer: String)
data class AppConfigResponse(val version: String, @SerializedName("updated_at") val updatedAt: String?, val config: com.google.gson.JsonObject)

>>>>>>> settings_redesign
// ── Feature models ───────────────────────────────────────────────────────────
data class MoodItem(val code: String, val label: String, val emoji: String, val message: String)
data class MoodVerse(val mood: String, val message: String, val verse: Verse)
data class MoodCheckinInput(val mood: String, @SerializedName("verse_id") val verseId: Int? = null)
data class MoodCheckin(val id: Int, val mood: String, @SerializedName("verse_id") val verseId: Int?, @SerializedName("created_at") val createdAt: String)
data class MoodSummary(val days: Int, val total: Int, val counts: Map<String, Int>, @SerializedName("top_mood") val topMood: String?, val recent: List<MoodCheckin>)
data class Explanation(@SerializedName("verse_id") val verseId: Int, val level: String, val language: String, val text: String)

data class TruthTopic(val code: String, val name: String, val icon: String, val description: String,
                      @SerializedName("religion_count") val religionCount: Int, @SerializedName("verse_count") val verseCount: Int)
data class TruthCard(
    @SerializedName("religion_id") val religionId: Int, val religion: String,
    @SerializedName("religion_code") val religionCode: String,
    @SerializedName("language_fallback") val languageFallback: Boolean, val verse: Verse
)
data class UniversalTruth(val topic: TruthTopic, val language: String, val cards: List<TruthCard>)

data class ChallengeProgress(
    val joined: Boolean = false,
    @SerializedName("completed_days") val completedDays: List<Int> = emptyList(),
    @SerializedName("next_day") val nextDay: Int? = 1,
    val completed: Boolean = false,
    @SerializedName("badge_earned") val badgeEarned: Boolean = false,
    @SerializedName("available_tomorrow") val availableTomorrow: Boolean? = false,
    @SerializedName("completed_count") val completedCount: Int = 0,
    @SerializedName("remaining_days") val remainingDays: Int = 0,
    val percent: Int = 0,
    @SerializedName("last_completed_on") val lastCompletedOn: String? = null,
    @SerializedName("missed_days") val missedDays: Int = 0
)
data class Challenge(
    val id: Int, val code: String, val title: String, val subtitle: String, val description: String,
    @SerializedName("total_days") val totalDays: Int, val icon: String, val color: String,
    @SerializedName("badge_name") val badgeName: String, @SerializedName("badge_icon") val badgeIcon: String,
    val progress: ChallengeProgress,
    @SerializedName("is_universal") val isUniversal: Boolean = false,
    val language: String = "en",
    @SerializedName("language_fallback") val languageFallback: Boolean = false
)
data class ChallengeDay(
    @SerializedName("day_number") val dayNumber: Int, val title: String, val reflection: String, val action: String,
    val topic: String, val status: String, val verse: Verse?
)
data class ChallengeDetail(
    val id: Int, val code: String, val title: String, val subtitle: String, val description: String,
    @SerializedName("total_days") val totalDays: Int, val icon: String, val color: String,
    @SerializedName("badge_name") val badgeName: String, @SerializedName("badge_icon") val badgeIcon: String,
    val progress: ChallengeProgress, val days: List<ChallengeDay>,
    @SerializedName("is_universal") val isUniversal: Boolean = false,
    val language: String = "en",
    @SerializedName("language_fallback") val languageFallback: Boolean = false
)
data class CompleteDayBody(val note: String? = null)
data class CompleteDayResult(
    @SerializedName("challenge_id") val challengeId: Int, @SerializedName("day_number") val dayNumber: Int,
    val progress: ChallengeProgress, @SerializedName("badge_earned_now") val badgeEarnedNow: Boolean,
    @SerializedName("badge_name") val badgeName: String?, @SerializedName("badge_icon") val badgeIcon: String?
)
data class Badge(
    @SerializedName("challenge_id") val challengeId: Int, @SerializedName("challenge_code") val challengeCode: String,
    @SerializedName("challenge_title") val challengeTitle: String, @SerializedName("badge_name") val badgeName: String,
    @SerializedName("badge_icon") val badgeIcon: String, val color: String, @SerializedName("earned_at") val earnedAt: String
)

data class StorySummary(
    val id: Int, val code: String, val emoji: String, val palette: String, val values: List<String>, val title: String,
    val lesson: String, val language: String, @SerializedName("language_fallback") val languageFallback: Boolean,
    val scripture: String, val reference: String, @SerializedName("verse_id") val verseId: Int?,
    @SerializedName("scene_count") val sceneCount: Int, val summary: String = "",
    @SerializedName("religion_code") val religionCode: String = ""
)
data class StoryScene(@SerializedName("scene_number") val sceneNumber: Int, val art: String, val text: String)
data class StoryDetail(
    val id: Int, val code: String, val emoji: String, val palette: String, val values: List<String>, val title: String,
    val lesson: String, val language: String, @SerializedName("language_fallback") val languageFallback: Boolean,
    val scripture: String, val reference: String, @SerializedName("verse_id") val verseId: Int?,
    @SerializedName("scene_count") val sceneCount: Int, val scenes: List<StoryScene>, val verse: Verse?,
    val summary: String = "", @SerializedName("religion_code") val religionCode: String = ""
)

data class StudioTemplateDto(
    val code: String, val name: String, val kind: String, val tradition: String?, val motif: String,
    @SerializedName("color_top") val colorTop: String, @SerializedName("color_bottom") val colorBottom: String,
    @SerializedName("color_text") val colorText: String, @SerializedName("color_accent") val colorAccent: String,
    @SerializedName("color_muted") val colorMuted: String, @SerializedName("is_light") val isLight: Boolean,
    val emoji: String = "", @SerializedName("sort_order") val sortOrder: Int = 0
)

data class TreeMilestoneDto(val stage: String, val label: String, val level: Int,
                            @SerializedName("first_day") val firstDay: Int, @SerializedName("last_day") val lastDay: Int, val unlocked: Boolean)
data class TreeNextDto(val stage: String, val label: String, val level: Int,
                       @SerializedName("at_days") val atDays: Int, @SerializedName("days_to_go") val daysToGo: Int)
<<<<<<< HEAD
=======
data class TreeActivityDto(val code: String, val title: String, val description: String, val metric: String,
                           val target: Int, val progress: Int, val completed: Boolean)
data class TreeActivitiesDto(val level: Int, val title: String, val subtitle: String, val days: String, val items: List<TreeActivityDto>,
                             @SerializedName("completed_count") val completedCount: Int, @SerializedName("total_count") val totalCount: Int,
                             @SerializedName("all_completed") val allCompleted: Boolean)
data class TreeLevelDto(val level: Int, val title: String, val days: String, val completed: Int, val total: Int, val status: String)
>>>>>>> settings_redesign
data class TreeDto(
    @SerializedName("total_days") val totalDays: Int, val level: Int, val stage: String,
    @SerializedName("stage_label") val stageLabel: String, val status: String,
    @SerializedName("current_streak") val currentStreak: Int, @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("read_today") val readToday: Boolean, @SerializedName("last_read") val lastRead: String?,
<<<<<<< HEAD
    @SerializedName("next_milestone") val nextMilestone: TreeNextDto?, val milestones: List<TreeMilestoneDto>, val history: List<String>
)
=======
    @SerializedName("next_milestone") val nextMilestone: TreeNextDto?, val milestones: List<TreeMilestoneDto>, val history: List<String>,
    val enabled: Boolean? = null, @SerializedName("started_on") val startedOn: String? = null,
    @SerializedName("reset_count") val resetCount: Int? = null, val activities: TreeActivitiesDto? = null,
    @SerializedName("activity_box_visible") val activityBoxVisible: Boolean? = null, val levels: List<TreeLevelDto>? = null
)
data class TreeSettingsBody(val enabled: Boolean)
data class TreeActivityBody(val event: String)
>>>>>>> settings_redesign
data class TreeReadBody(val dates: List<String> = emptyList(), @SerializedName("mark_today") val markToday: Boolean = true)

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
    @GET("languages") suspend fun languages(@Query("religion_id") religionId: Int? = null): List<CatalogItem>
    @GET("scriptures") suspend fun scriptures(@Query("religion_id") religion: Int): List<CatalogItem>

    // Verses — token auto-attached by interceptor
    @GET("verses/today")
    suspend fun today(
        @Query("religion_id") religion: Int? = null,
        @Query("language_id") language: Int? = null,
        @Query("client_date") clientDate: String? = null
    ): Verse

    /** Draws from the whole tradition (omit [scripture] to span all of its scriptures). */
    @GET("verses/random")
    suspend fun random(
        @Query("religion_id") religion: Int,
        @Query("language_id") language: Int,
        @Query("scripture_id") scripture: Int? = null,
        @Query("exclude_id") excludeId: Int? = null
    ): Verse

<<<<<<< HEAD
=======
    @GET("verses/active")
    suspend fun activeVerse(): ActiveVerseResponse

    /** Refresh the screen from the server without advancing an already selected verse. */
    @GET("verses/active/current")
    suspend fun currentActiveVerse(): ActiveVerseResponse

    @POST("verses/discover")
    suspend fun discoverVerse(): ActiveVerseResponse

    @POST("verses/active/refresh")
    suspend fun refreshActiveVerse(): ActiveVerseResponse

>>>>>>> settings_redesign
    @GET("studio/templates")
    suspend fun studioTemplates(@Query("kind") kind: String = "all", @Query("religion_id") religionId: Int? = null): List<StudioTemplateDto>

    @GET("tree") suspend fun tree(@Query("client_date") clientDate: String? = null): TreeDto

<<<<<<< HEAD
=======
    @PUT("tree/settings")
    suspend fun setTreeEnabled(@Body body: TreeSettingsBody, @Query("client_date") clientDate: String? = null): TreeDto

    @POST("tree/activity")
    suspend fun treeActivity(@Body body: TreeActivityBody, @Query("client_date") clientDate: String? = null): TreeDto

    // ── Journal Reflection ──
    @GET("reflections/today")
    suspend fun todaysReflection(@Query("verse_id") verseId: Int? = null, @Query("client_date") clientDate: String? = null): TodayReflectionDto

    @POST("reflections") suspend fun saveReflection(@Body body: ReflectionSaveBody): Response<ReflectionDto>

    @GET("reflections")
    suspend fun reflections(
        @Query("limit") limit: Int = 20, @Query("offset") offset: Int = 0,
        @Query("month") month: String? = null, @Query("date") date: String? = null
    ): ReflectionListDto

    @GET("reflections/dates") suspend fun reflectionDates(@Query("days") days: Int = 60): List<String>
    @GET("reflections/{id}") suspend fun reflection(@Path("id") id: Int): ReflectionDto
    @PUT("reflections/{id}") suspend fun updateReflection(@Path("id") id: Int, @Body body: ReflectionUpdateBody): Response<ReflectionDto>
    @DELETE("reflections/{id}") suspend fun deleteReflection(@Path("id") id: Int): Response<Unit>

    // ── Centralised configuration (ads, intervals, flags) ──
    @GET("config") suspend fun config(@retrofit2.http.Header("If-None-Match") etag: String? = null): Response<AppConfigResponse>

>>>>>>> settings_redesign
    @POST("tree/read")
    suspend fun recordReading(@Query("client_date") clientDate: String? = null, @Body body: TreeReadBody = TreeReadBody()): TreeDto

    // ── Mood, explanations, universal truths, challenges, stories ──
    @GET("moods") suspend fun moods(): List<MoodItem>

    @GET("mood/verse")
    suspend fun moodVerse(
        @Query("mood") mood: String,
        @Query("religion_id") religionId: Int?,
        @Query("language_id") languageId: Int?,
        @Query("shuffle") shuffle: Int = 0
    ): MoodVerse

    @POST("mood/checkins") suspend fun recordMood(@Body body: MoodCheckinInput): Response<Unit>
    @GET("mood/summary") suspend fun moodSummary(@Query("days") days: Int = 30): MoodSummary

    @GET("verses/{id}/explain") suspend fun explain(@Path("id") id: Int): Response<Explanation>

    @GET("verses/{id}/story")
    suspend fun storyForVerse(@Path("id") id: Int, @Query("language_id") languageId: Int?): Response<StoryDetail>

    @GET("universal-truths/topics") suspend fun truthTopics(): List<TruthTopic>

    @GET("universal-truths/{code}")
    suspend fun universalTruth(
        @Path("code") code: String,
        @Query("language_id") languageId: Int?,
        @Query("seed") seed: Int = 0
    ): UniversalTruth

    @GET("challenges")
    suspend fun challenges(@Query("client_date") clientDate: String? = null, @Query("language_id") languageId: Int? = null): List<Challenge>

    @GET("challenges/{id}")
    suspend fun challenge(
        @Path("id") id: Int,
        @Query("language_id") languageId: Int?,
        @Query("religion_id") religionId: Int?,
        @Query("client_date") clientDate: String? = null
    ): ChallengeDetail

    @POST("challenges/{id}/join") suspend fun joinChallenge(@Path("id") id: Int): Response<ChallengeProgress>

    @POST("challenges/{id}/days/{day}/complete")
    suspend fun completeDay(
        @Path("id") id: Int,
        @Path("day") day: Int,
        @Query("client_date") clientDate: String?,
        @Body body: CompleteDayBody = CompleteDayBody()
    ): Response<CompleteDayResult>

    @DELETE("challenges/{id}/progress") suspend fun restartChallenge(@Path("id") id: Int): Response<Unit>
    @GET("users/badges") suspend fun badges(): List<Badge>

    @GET("stories")
    suspend fun stories(@Query("language_id") languageId: Int?, @Query("religion_id") religionId: Int? = null): List<StorySummary>

    @GET("stories/{code}")
    suspend fun story(@Path("code") code: String, @Query("language_id") languageId: Int?): StoryDetail

    @GET("verses/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("language_id") languageId: Int? = null,
        @Query("scripture_id") scriptureId: Int? = null
    ): List<Verse>

    @GET("verses/today/history")
    suspend fun todayHistory(
        @Query("count") count: Int = 7,
        @Query("religion_id") religion: Int? = null,
        @Query("language_id") language: Int? = null
    ): List<Verse>

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

<<<<<<< HEAD
=======
    @PUT("devices/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenInput): Response<Unit>

>>>>>>> settings_redesign
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

    /** Lets widgets, workers and receivers use the API from a cold process (MainActivity not started). */
    fun ensureInit(context: Context) {
        if (!::_api.isInitialized) {
            val app = context.applicationContext
            init(app) { app.getSharedPreferences("reader", Context.MODE_PRIVATE).getString("bearer_token", null) }
        }
    }
}
