package com.example.data

import com.example.BuildConfig
import com.example.domain.SolarFlowCandidate
import com.example.domain.SolarFlowInput
import com.example.domain.SolarFlowResult
import com.example.domain.WeatherForecastDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlin.math.roundToInt

data class AuthSessionResult(
    val userId: String,
    val name: String,
    val email: String,
    val mobile: String = "",
    val role: String = "farmer",
    val approved: Boolean = true,
    val accessToken: String = "",
    val provider: String = "server" // server | supabase | local | dev
)

data class ServerMetaResult(
    val ok: Boolean,
    val authRequired: Boolean,
    val provider: String,
    val signupEnabled: Boolean,
    val detail: String
)

data class LiveWeatherSnapshot(
    val locationName: String,
    val currentTempC: Double,
    val currentRh: Int,
    val currentRainMm: Double,
    val forecast: List<WeatherForecastDay>,
    val fetchedAtIso: String
)

object GrowthTwinApiClient {

    private fun isValidConfig(raw: String): Boolean {
        val v = raw.trim()
        return v.isNotBlank() &&
            !v.equals("null", ignoreCase = true) &&
            !v.startsWith("#") &&
            !v.startsWith("YOUR_", ignoreCase = true)
    }

    fun hasSupabaseConfig(): Boolean =
        isValidConfig(BuildConfig.SUPABASE_URL) &&
            BuildConfig.SUPABASE_URL.startsWith("http") &&
            isValidConfig(BuildConfig.SUPABASE_ANON_KEY)

    fun hasGroqConfig(): Boolean =
        isValidConfig(BuildConfig.GROQ_API_KEY)

    fun hasSensorToken(): Boolean =
        isValidConfig(BuildConfig.SENSOR_INGEST_TOKEN)

    private fun cleanBaseUrl(raw: String): String =
        raw.trim().trimEnd('/')

    private suspend fun requestJson(
        urlStr: String,
        method: String = "GET",
        body: JSONObject? = null,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 25000
    ): Result<Pair<Int, String>> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> if (v.isNotBlank()) setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
            }.orEmpty()
            Result.success(code to text)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractErrorDetail(raw: String, code: Int): String {
        return try {
            val j = JSONObject(raw)
            j.optString("detail").ifBlank {
                j.optString("error_description").ifBlank {
                    j.optString("msg").ifBlank {
                        j.optString("message").ifBlank { "HTTP $code" }
                    }
                }
            }
        } catch (_: Exception) {
            raw.take(160).ifBlank { "HTTP $code" }
        }
    }

    // ---- 1. GrowthTwin FastAPI Backend (https://growth-twin.onrender.com) ----

    suspend fun pingServer(baseUrl: String): Result<ServerMetaResult> {
        val base = cleanBaseUrl(baseUrl)
        if (base.isBlank()) return Result.failure(IllegalArgumentException("Server URL is empty"))
        val cfgRes = requestJson("$base/auth/config")
        val healthRes = requestJson("$base/health")
        return cfgRes.fold(
            onSuccess = { (code, text) ->
                if (code in 200..299) {
                    val j = runCatching { JSONObject(text) }.getOrNull() ?: JSONObject()
                    val hJson = healthRes.getOrNull()?.takeIf { it.first in 200..299 }?.second
                        ?.let { runCatching { JSONObject(it) }.getOrNull() }
                    val storeName = hJson?.optString("store", "").orEmpty().ifBlank { "remote" }
                    Result.success(
                        ServerMetaResult(
                            ok = hJson?.optBoolean("ok", true) ?: true,
                            authRequired = j.optBoolean("auth_required", true),
                            provider = storeName,
                            signupEnabled = j.optBoolean("signup_enabled", true),
                            detail = "Connected ($code)"
                        )
                    )
                } else {
                    Result.failure(IllegalStateException(extractErrorDetail(text, code)))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun serverLogin(baseUrl: String, email: String, password: String): Result<AuthSessionResult> {
        val base = cleanBaseUrl(baseUrl)
        if (base.isBlank()) return Result.failure(IllegalArgumentException("No server URL configured"))
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
        val res = requestJson("$base/auth/login", "POST", payload)
        return res.fold(
            onSuccess = { (code, text) ->
                if (code in 200..299) {
                    val j = JSONObject(text)
                    val user = j.optJSONObject("user") ?: j.optJSONObject("profile") ?: j
                    val token = j.optString("access_token").ifBlank { j.optString("token") }
                    val displayName = user.optString("full_name")
                        .ifBlank { user.optString("name") }
                        .ifBlank { email.substringBefore("@") }
                    Result.success(
                        AuthSessionResult(
                            userId = user.optString("id").ifBlank { user.optString("user_id", email.substringBefore("@")) },
                            name = displayName,
                            email = user.optString("email", email),
                            mobile = user.optString("phone").ifBlank { user.optString("mobile", "") },
                            role = user.optString("role", "farmer"),
                            approved = user.optBoolean("approved", true),
                            accessToken = token,
                            provider = "server"
                        )
                    )
                } else {
                    Result.failure(IllegalStateException(extractErrorDetail(text, code)))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun serverRegister(
        baseUrl: String,
        name: String,
        mobile: String,
        email: String,
        password: String,
        role: String
    ): Result<AuthSessionResult> {
        val base = cleanBaseUrl(baseUrl)
        if (base.isBlank()) return Result.failure(IllegalArgumentException("No server URL configured"))
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("full_name", name.trim())
            if (mobile.isNotBlank()) put("phone", mobile.trim())
            put("role_request", role)
        }
        val res = requestJson("$base/auth/register", "POST", payload)
        return res.fold(
            onSuccess = { (code, text) ->
                if (code in 200..299) {
                    val j = JSONObject(text)
                    val user = j.optJSONObject("user") ?: j.optJSONObject("profile") ?: j
                    val token = j.optString("access_token").ifBlank { j.optString("token") }
                    val displayName = user.optString("full_name")
                        .ifBlank { user.optString("name") }
                        .ifBlank { name.trim() }
                    Result.success(
                        AuthSessionResult(
                            userId = user.optString("id").ifBlank { user.optString("user_id", email.substringBefore("@")) },
                            name = displayName,
                            email = user.optString("email", email),
                            mobile = user.optString("phone").ifBlank { user.optString("mobile", mobile) },
                            role = user.optString("role", role),
                            approved = user.optBoolean("approved", role == "farmer"),
                            accessToken = token,
                            provider = "server"
                        )
                    )
                } else {
                    Result.failure(IllegalStateException(extractErrorDetail(text, code)))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun serverLogout(baseUrl: String, token: String) {
        val base = cleanBaseUrl(baseUrl)
        if (base.isBlank() || token.isBlank()) return
        runCatching {
            requestJson("$base/auth/logout", "POST", null, authHeaders(token), timeoutMs = 6000)
        }
    }

    private fun authHeaders(token: String, userId: String = "", role: String = ""): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (token.isNotBlank()) map["Authorization"] = "Bearer $token"
        if (userId.isNotBlank()) map["X-User-Id"] = userId
        if (role.isNotBlank()) map["X-User-Role"] = role
        return map
    }

    suspend fun serverAdvisorAsk(
        baseUrl: String,
        token: String,
        userId: String,
        role: String,
        prompt: String
    ): Result<JSONObject> {
        val base = cleanBaseUrl(baseUrl)
        val payload = JSONObject().apply { put("prompt", prompt) }
        return requestJson("$base/advisor/analyze", "POST", payload, authHeaders(token, userId, role)).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            JSONObject(text)
        }
    }

    suspend fun serverFairMarketAsk(
        baseUrl: String,
        token: String,
        question: String
    ): Result<JSONObject> {
        val base = cleanBaseUrl(baseUrl)
        val payload = JSONObject().apply {
            put("question", question)
            put("history", JSONArray())
        }
        return requestJson("$base/fair-market/ask", "POST", payload, authHeaders(token)).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            JSONObject(text)
        }
    }

    suspend fun serverSolarFlowRecommend(
        baseUrl: String,
        token: String,
        input: SolarFlowInput
    ): Result<SolarFlowResult> {
        val base = cleanBaseUrl(baseUrl)
        val payload = JSONObject().apply {
            put("soil_moisture_pct", input.soilMoisturePct)
            put("target_moisture_pct", input.targetMoisturePct)
            put("area_m2", input.areaM2)
            put("root_zone_available_mm", input.rootZoneAvailableMm)
            put("daily_crop_water_mm", input.dailyCropWaterMm)
            put("forecast_rain_mm", input.forecastRainMm)
            put("rain_probability", input.rainProbability)
            put("solar_power_w", input.solarPowerW)
            put("pump_power_w", input.pumpPowerW)
            put("battery_capacity_wh", input.batteryCapacityWh)
            put("battery_pct", input.batteryPct)
            put("battery_reserve_pct", input.batteryReservePct)
            put("tank_water_l", input.tankWaterL)
            put("pump_flow_l_min", input.pumpFlowLMin)
            put("horizon_hours", input.horizonHours)
        }
        return requestJson("$base/solarflow/plan", "POST", payload, authHeaders(token)).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            val j = JSONObject(text)
            val rec = j.getString("recommendation")
            val candsArr = j.optJSONArray("candidates") ?: JSONArray()
            val cands = (0 until candsArr.length()).map { i ->
                val c = candsArr.getJSONObject(i)
                SolarFlowCandidate(
                    action = c.optString("action"),
                    energySource = c.optString("energy_source", "none"),
                    pumpMinutes = c.optDouble("pump_minutes", 0.0),
                    waterL = c.optDouble("water_l", 0.0),
                    energyWh = c.optDouble("energy_wh", 0.0),
                    unmetWaterMm = c.optDouble("unmet_water_mm", 0.0),
                    score = c.optDouble("score", 0.0)
                )
            }
            val reasonsArr = j.optJSONArray("reasons") ?: JSONArray()
            val reasons = (0 until reasonsArr.length()).map { reasonsArr.getString(it) }
            val flagsArr = j.optJSONArray("flags") ?: JSONArray()
            val flags = (0 until flagsArr.length()).map { flagsArr.getString(it) }
            val assArr = j.optJSONArray("assumptions") ?: JSONArray()
            val assumptions = (0 until assArr.length()).map { assArr.getString(it) }
            val band = j.optJSONArray("water_demand_range_mm")
            val rangePair = if (band != null && band.length() >= 2) {
                band.getDouble(0) to band.getDouble(1)
            } else {
                0.0 to j.optDouble("water_demand_mm", 0.0)
            }
            SolarFlowResult(
                recommendation = rec,
                energySource = j.optString("energy_source", cands.firstOrNull { it.action == rec }?.energySource ?: "none"),
                pumpMinutes = j.optDouble("pump_minutes", 0.0),
                estimatedWaterL = j.optDouble("estimated_water_l", 0.0),
                estimatedEnergyWh = j.optDouble("estimated_energy_wh", 0.0),
                waterDemandMm = j.optDouble("water_demand_mm", 0.0),
                waterDemandRangeMm = rangePair,
                estimatedBatteryAfterPct = j.optDouble("estimated_battery_after_pct", input.batteryPct),
                candidates = cands,
                reasons = reasons,
                flags = flags,
                assumptions = assumptions
            )
        }
    }

    suspend fun serverIngestSensor(
        baseUrl: String,
        plotId: String,
        deviceId: String,
        sequence: Int,
        rawAdc: Int,
        temperatureC: Double?,
        humidityPct: Double?
    ): Result<JSONObject> {
        val base = cleanBaseUrl(baseUrl)
        val payload = JSONObject().apply {
            put("plot_id", plotId)
            put("device_id", deviceId)
            put("sequence", sequence)
            put("timestamp", Instant.now().toString())
            put("raw_moisture_adc", rawAdc)
            if (temperatureC != null) put("temperature_c", temperatureC)
            if (humidityPct != null) put("humidity_pct", humidityPct)
        }
        val headers = if (hasSensorToken()) mapOf("X-Sensor-Token" to BuildConfig.SENSOR_INGEST_TOKEN) else emptyMap()
        return requestJson("$base/twin/device/readings", "POST", payload, headers).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            JSONObject(text)
        }
    }

    // ---- 2. Direct Supabase GoTrue Auth (when SUPABASE_URL & SUPABASE_ANON_KEY are in Secrets) ----

    suspend fun supabaseLogin(email: String, password: String): Result<AuthSessionResult> {
        if (!hasSupabaseConfig()) return Result.failure(IllegalStateException("Supabase not configured"))
        val base = cleanBaseUrl(BuildConfig.SUPABASE_URL)
        val headers = mapOf(
            "apikey" to BuildConfig.SUPABASE_ANON_KEY,
            "Authorization" to "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        )
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
        return requestJson("$base/auth/v1/token?grant_type=password", "POST", payload, headers).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            val j = JSONObject(text)
            val user = j.getJSONObject("user")
            val meta = user.optJSONObject("user_metadata") ?: JSONObject()
            AuthSessionResult(
                userId = user.getString("id"),
                name = meta.optString("name", email.substringBefore("@")),
                email = user.optString("email", email),
                mobile = meta.optString("mobile", ""),
                role = meta.optString("role", "farmer"),
                approved = meta.optBoolean("approved", true),
                accessToken = j.getString("access_token"),
                provider = "supabase"
            )
        }
    }

    suspend fun supabaseRegister(
        name: String,
        mobile: String,
        email: String,
        password: String,
        role: String
    ): Result<AuthSessionResult> {
        if (!hasSupabaseConfig()) return Result.failure(IllegalStateException("Supabase not configured"))
        val base = cleanBaseUrl(BuildConfig.SUPABASE_URL)
        val headers = mapOf(
            "apikey" to BuildConfig.SUPABASE_ANON_KEY,
            "Authorization" to "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        )
        val meta = JSONObject().apply {
            put("name", name.trim())
            put("mobile", mobile.trim())
            put("role", role)
            put("approved", role == "farmer")
        }
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("data", meta)
        }
        return requestJson("$base/auth/v1/signup", "POST", payload, headers).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            val j = JSONObject(text)
            val user = j.optJSONObject("user") ?: j
            AuthSessionResult(
                userId = user.optString("id", email.substringBefore("@")),
                name = name.trim(),
                email = email.trim(),
                mobile = mobile.trim(),
                role = role,
                approved = role == "farmer",
                accessToken = j.optString("access_token", ""),
                provider = "supabase"
            )
        }
    }

    // ---- 3. Real Public Open-Meteo Bangladesh Weather API ----

    suspend fun fetchLiveWeather(
        lat: Double = 23.8103,
        lon: Double = 90.4125,
        locationName: String = "Dhaka"
    ): Result<LiveWeatherSnapshot> {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,precipitation" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max" +
            "&timezone=Asia%2FDhaka&forecast_days=7"
        return requestJson(url).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            val j = JSONObject(text)
            val cur = j.getJSONObject("current")
            val daily = j.getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val tmax = daily.getJSONArray("temperature_2m_max")
            val tmin = daily.getJSONArray("temperature_2m_min")
            val rain = daily.getJSONArray("precipitation_sum")
            val prob = daily.getJSONArray("precipitation_probability_max")
            val rhNow = cur.optDouble("relative_humidity_2m", 75.0).roundToInt()

            val forecast = (0 until dates.length()).map { i ->
                val rMm = rain.optDouble(i, 0.0)
                val pPct = prob.optInt(i, if (rMm > 2.0) 60 else 15)
                val maxT = tmax.optDouble(i, 31.0)
                val vs = when {
                    rMm >= 10.0 -> "more"
                    rMm <= 1.0 -> "less"
                    else -> "normal"
                }
                WeatherForecastDay(
                    date = dates.getString(i),
                    leadDays = i,
                    rainMm = rMm,
                    prob = pPct,
                    tmax = maxT,
                    normalMm = 4.0,
                    vsNormal = vs
                )
            }

            LiveWeatherSnapshot(
                locationName = locationName,
                currentTempC = cur.optDouble("temperature_2m", 28.0),
                currentRh = rhNow,
                currentRainMm = cur.optDouble("precipitation", 0.0),
                forecast = forecast,
                fetchedAtIso = Instant.now().toString()
            )
        }
    }

    // ---- 4. Direct Groq LLM Advisory Synthesis (when GROQ_API_KEY is in Secrets) ----

    suspend fun groqAdvisorNarrative(
        prompt: String,
        deterministicSummary: String,
        langCode: String
    ): Result<String> {
        if (!hasGroqConfig()) return Result.failure(IllegalStateException("GROQ_API_KEY not configured"))
        val model = BuildConfig.GROQ_MODEL.ifBlank { "llama-3.3-70b-versatile" }
        val langInstr = if (langCode == "bn") "Respond concisely in Bangla." else "Respond concisely in English."
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", 0.25)
            put("max_tokens", 280)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are GrowthTwin's agriculture assistant for Bangladesh. Never contradict the deterministic facts provided. $langInstr")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Farmer prompt: $prompt\n\nVerified calculations:\n$deterministicSummary")
                    })
                }
            )
        }
        val headers = mapOf("Authorization" to "Bearer ${BuildConfig.GROQ_API_KEY}")
        return requestJson("https://api.groq.com/openai/v1/chat/completions", "POST", payload, headers).mapCatching { (code, text) ->
            if (code !in 200..299) throw IllegalStateException(extractErrorDetail(text, code))
            val j = JSONObject(text)
            j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
        }
    }
}
