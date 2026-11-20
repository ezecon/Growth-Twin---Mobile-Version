package com.example.ui.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdvisorSessionEntity
import com.example.data.GrowthTwinApiClient
import com.example.data.GrowthTwinDatabase
import com.example.data.GrowthTwinRepository
import com.example.data.LiveWeatherSnapshot
import com.example.data.PreferencesStore
import com.example.data.TelemetryReadingEntity
import com.example.data.UserPrefs
import com.example.domain.AdvisorEngine
import com.example.domain.AdvisorReport
import com.example.domain.CropCatalog
import com.example.domain.DailyLogEntry
import com.example.domain.FairMarketEngine
import com.example.domain.HarvestRecord
import com.example.domain.IrrigationController
import com.example.domain.IrrigationView
import com.example.domain.MarketPriceAnalysis
import com.example.domain.MoistureCalibration
import com.example.domain.PhotoReviewItem
import com.example.domain.PlotConfig
import com.example.domain.PlotState
import com.example.domain.PlotTwinEngine
import com.example.domain.SentinelEngine
import com.example.domain.SlotValue
import com.example.domain.SolarFlowEngine
import com.example.domain.SolarFlowInput
import com.example.domain.SolarFlowResult
import com.example.domain.TrustAggregator
import com.example.domain.TrustReport
import com.example.domain.TwinActionSpec
import com.example.domain.TwinEvent
import com.example.ui.theme.AppLang
import com.example.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class ExpertPhotoItem(
    val plotId: String,
    val plotName: String,
    val cropId: String,
    val review: PhotoReviewItem
)

data class ExpertAdvisorQueueItem(
    val sessionId: String,
    val prompt: String,
    val headlineBn: String,
    val headlineEn: String,
    val confidence: Double
)

data class ChannelSimMessage(
    val id: String,
    val fromFarmer: Boolean,
    val text: String
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GrowthTwinViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GrowthTwinDatabase.getInstance(application)
    private val prefsStore = PreferencesStore(application)
    val repo = GrowthTwinRepository(db, prefsStore)

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    val prefs: StateFlow<UserPrefs> = prefsStore.prefsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefs()
    )

    val plots: StateFlow<List<PlotState>> = repo.observePlots().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedPlotId = MutableStateFlow<String?>(null)
    val selectedPlotId: StateFlow<String?> = _selectedPlotId.asStateFlow()

    val selectedPlot: StateFlow<PlotState?> = combine(plots, _selectedPlotId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val plotLogs: StateFlow<List<DailyLogEntry>> = selectedPlot.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else repo.observeLogs(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plotEvents: StateFlow<List<TwinEvent>> = selectedPlot.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else repo.observeEvents(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telemetryList: StateFlow<List<TelemetryReadingEntity>> = selectedPlot.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else repo.observeTelemetry(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Irrigation View
    private val _irrigationView = MutableStateFlow<IrrigationView?>(null)
    val irrigationView: StateFlow<IrrigationView?> = _irrigationView.asStateFlow()

    // Section 1 Advisor
    private val _advisorPrompt = MutableStateFlow("")
    val advisorPrompt: StateFlow<String> = _advisorPrompt.asStateFlow()

    private val _advisorSlots = MutableStateFlow<MutableMap<String, SlotValue>>(mutableMapOf())
    private val _advisorAsked = MutableStateFlow<MutableList<String>>(mutableListOf())
    private val _advisorSessionId = MutableStateFlow(UUID.randomUUID().toString().take(8))

    private val _advisorReport = MutableStateFlow<AdvisorReport?>(null)
    val advisorReport: StateFlow<AdvisorReport?> = _advisorReport.asStateFlow()

    // Sentinel simulation selector
    private val _sentinelScenarioId = MutableStateFlow("fresh")
    val sentinelScenarioId: StateFlow<String> = _sentinelScenarioId.asStateFlow()

    // SolarFlow AI
    private val _solarInput = MutableStateFlow(SolarFlowInput())
    val solarInput: StateFlow<SolarFlowInput> = _solarInput.asStateFlow()
    private val _solarResult = MutableStateFlow(SolarFlowEngine.recommend(SolarFlowInput()))
    val solarResult: StateFlow<SolarFlowResult> = _solarResult.asStateFlow()

    // Fair Market
    private val _marketQuery = MutableStateFlow("বগুড়ায় আলু বিক্রি করব, দালাল ১৮ টাকা কেজি বলছে")
    val marketQuery: StateFlow<String> = _marketQuery.asStateFlow()
    private val _marketQty = MutableStateFlow("200")
    val marketQty: StateFlow<String> = _marketQty.asStateFlow()
    private val _marketAnalysis = MutableStateFlow(FairMarketEngine.analyze("বগুড়ায় আলু বিক্রি করব, দালাল ১৮ টাকা কেজি বলছে", 200.0))
    val marketAnalysis: StateFlow<MarketPriceAnalysis> = _marketAnalysis.asStateFlow()

    // Farmers' Trust
    private val seededCohort: List<HarvestRecord> = TrustAggregator.buildSeededCohort()
    private val _trustCropId = MutableStateFlow("boro")
    val trustCropId: StateFlow<String> = _trustCropId.asStateFlow()

    // WhatsApp/SMS offline command simulator
    private val _channelMessages = MutableStateFlow<List<ChannelSimMessage>>(
        listOf(
            ChannelSimMessage(
                "welcome",
                fromFarmer = false,
                text = "GrowthTwin চ্যানেল প্রস্তুত। 'অবস্থা', 'সেচ', 'দাম আলু', বা 'LINK <code>' পাঠান।"
            )
        )
    )
    val channelMessages: StateFlow<List<ChannelSimMessage>> = _channelMessages.asStateFlow()

    val trustReport: StateFlow<TrustReport> = combine(
        _trustCropId,
        repo.observeUserHarvests(),
        plots,
        prefs
    ) { cropId, userHarvests, plotList, p ->
        val all = seededCohort + userHarvests
        val growing = plotList.count { it.cropId == cropId && it.status in setOf("growing", "ready") }
        TrustAggregator.aggregate(cropId, p.userId, all, growing)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TrustAggregator.aggregate("boro", "farmer1", seededCohort, 1)
    )

    // Expert queues
    val expertPhotoQueue: StateFlow<List<ExpertPhotoItem>> = plots.flatMapLatest { list ->
        val items = mutableListOf<ExpertPhotoItem>()
        list.forEach { p ->
            p.reviewPending.filter { it.status == "pending" }.forEach { rev ->
                items.add(ExpertPhotoItem(p.id, p.name, p.cropId, rev))
            }
        }
        flowOf(items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expertAdvisorQueue: StateFlow<List<ExpertAdvisorQueueItem>> = repo.observeReviewQueue().flatMapLatest { rows ->
        val out = rows.map { row ->
            val slots = repo.decodeSlotsJson(row.slotsJson)
            val rep = AdvisorEngine.evaluate(row.sessionId, row.prompt, slots, row.askedSlotsCsv.split(",").filter { it.isNotBlank() }, emptyList(), finishNow = true)
            ExpertAdvisorQueueItem(row.sessionId, row.prompt, rep.headlineBn, rep.headlineEn, rep.confidence)
        }
        flowOf(out)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // TTS for Plot Bangla summary
    private var tts: TextToSpeech? = null
    private val _ttsReady = MutableStateFlow(false)
    private val _ttsSpeaking = MutableStateFlow(false)
    val ttsSpeaking: StateFlow<Boolean> = _ttsSpeaking.asStateFlow()

    // Auth & Real Backend API State
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    private val _authInfo = MutableStateFlow<String?>(null)
    val authInfo: StateFlow<String?> = _authInfo.asStateFlow()
    private val _serverStatusText = MutableStateFlow("")
    val serverStatusText: StateFlow<String> = _serverStatusText.asStateFlow()

    // Live Open-Meteo Bangladesh Weather State
    private val _liveWeather = MutableStateFlow<LiveWeatherSnapshot?>(null)
    val liveWeather: StateFlow<LiveWeatherSnapshot?> = _liveWeather.asStateFlow()
    private val _liveWeatherLoading = MutableStateFlow(false)
    val liveWeatherLoading: StateFlow<Boolean> = _liveWeatherLoading.asStateFlow()

    init {
        viewModelScope.launch {
            val p = prefsStore.prefsFlow.first()
            repo.ensureSeeded(p.userId)
            val initialPlots = repo.observePlots().first()
            initialPlots.firstOrNull()?.let { first ->
                _selectedPlotId.value = first.id
                IrrigationController.seedFromPlot(first.id, first.awPct())
                _irrigationView.value = IrrigationController.view(first.id)
            }
            if (p.serverBaseUrl.isNotBlank()) {
                GrowthTwinApiClient.pingServer(p.serverBaseUrl).onSuccess { meta ->
                    _serverStatusText.value = "${meta.detail} • provider=${meta.provider}"
                }
            }
        }
        refreshLiveWeather()
        startIrrigationLoop()
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _ttsReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { _ttsSpeaking.value = true }
                    override fun onDone(utteranceId: String?) { _ttsSpeaking.value = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { _ttsSpeaking.value = false }
                })
            }
        }
    }

    private fun startIrrigationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                val p = selectedPlot.value
                if (p != null) {
                    IrrigationController.seedFromPlot(p.id, p.awPct())
                    val sync = IrrigationController.tick(p.id, p.awPct())
                    _irrigationView.value = IrrigationController.view(p.id)
                    if (sync != null && p.status in setOf("growing", "ready")) {
                        val (kind, secs) = sync
                        val copy = p.deepCopy()
                        val evs = mutableListOf(PlotTwinEngine.applyAction(copy, "pump", pumpOnVal = (kind == "on")))
                        val mm = (secs / 60.0 * IrrigationController.MM_PER_MIN).coerceAtMost(60.0)
                        if (kind == "off" && mm > 0.2) {
                            evs.add(PlotTwinEngine.applyAction(copy, "irrigate", mmOverride = mm))
                        }
                        repo.savePlotAndSideEffects(copy, events = evs)
                    }
                }
            }
        }
    }

    private fun toastMsg(msg: String) {
        _toast.tryEmit(msg)
    }

    // ---- Preferences / Shell ----
    fun toggleTheme() {
        viewModelScope.launch { prefsStore.setDarkTheme(!prefs.value.darkTheme) }
    }

    fun toggleLang() {
        viewModelScope.launch {
            val next = if (prefs.value.lang == AppLang.BN) AppLang.EN else AppLang.BN
            prefsStore.setLang(next)
        }
    }

    fun setIdentity(userId: String, role: String) {
        viewModelScope.launch {
            prefsStore.setIdentity(userId, role)
            val l = prefs.value.lang
            toastMsg(tr(l, "ভূমিকা পরিবর্তিত হয়েছে: $role", "Role switched to: $role"))
        }
    }

    // ---- Auth (#auth Login / Register / Dev Mode / Logout + Real Backend API) ----
    fun login(email: String, password: String) {
        val l = prefs.value.lang
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || password.isBlank()) {
            _authError.value = tr(l, "সঠিক ইমেইল ও পাসওয়ার্ড দিন।", "Enter a valid email and password.")
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            _authInfo.value = null
            val srvUrl = prefs.value.serverBaseUrl

            val remoteResult = when {
                srvUrl.isNotBlank() -> GrowthTwinApiClient.serverLogin(srvUrl, cleanEmail, password)
                GrowthTwinApiClient.hasSupabaseConfig() -> GrowthTwinApiClient.supabaseLogin(cleanEmail, password)
                else -> null
            }

            val finalRes = remoteResult ?: repo.loginLocal(cleanEmail, password)
            finalRes.onSuccess { session ->
                prefsStore.saveAuthSession(session)
                _authInfo.value = null
                toastMsg(tr(l, "স্বাগতম, ${session.name} (${session.provider})", "Welcome, ${session.name} (${session.provider})"))
            }.onFailure { err ->
                // If remote server was unreachable, check if local account exists as fallback only on connection failure; otherwise surface server error
                _authError.value = err.message ?: tr(l, "লগইন ব্যর্থ হয়েছে", "Login failed")
            }
            _authLoading.value = false
        }
    }

    fun register(name: String, mobile: String, email: String, password: String, role: String) {
        val l = prefs.value.lang
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (cleanName.isBlank() || !cleanEmail.contains("@")) {
            _authError.value = tr(l, "নাম ও সঠিক ইমেইল দিন।", "Enter your name and a valid email.")
            return
        }
        if (password.length < 8 || !hasLetter || !hasDigit) {
            _authError.value = tr(
                l,
                "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে এবং অক্ষর ও সংখ্যা থাকতে হবে।",
                "Password must be 8+ characters with both letters and numbers."
            )
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            _authInfo.value = null
            val srvUrl = prefs.value.serverBaseUrl

            val res = when {
                srvUrl.isNotBlank() -> GrowthTwinApiClient.serverRegister(srvUrl, cleanName, mobile, cleanEmail, password, role)
                GrowthTwinApiClient.hasSupabaseConfig() -> GrowthTwinApiClient.supabaseRegister(cleanName, mobile, cleanEmail, password, role)
                else -> repo.registerLocal(cleanName, mobile, cleanEmail, password, role)
            }

            res.onSuccess { session ->
                if (!session.approved) {
                    _authInfo.value = tr(
                        l,
                        "বিশেষজ্ঞ একাউন্ট তৈরি হয়েছে (অ্যাডমিন অনুমোদনের পর সক্রিয় হবে)।",
                        "Expert account created (pending admin approval)."
                    )
                }
                if (session.accessToken.isNotBlank() || session.provider == "local") {
                    prefsStore.saveAuthSession(session)
                    toastMsg(tr(l, "একাউন্ট তৈরি সফল হয়েছে!", "Account created!"))
                } else {
                    _authInfo.value = tr(
                        l,
                        "নিবন্ধন সম্পন্ন! ইমেইল যাচাই করে লগইন করুন।",
                        "Registration submitted! Check your email or sign in."
                    )
                }
            }.onFailure { err ->
                _authError.value = err.message ?: tr(l, "একাউন্ট খোলা ব্যর্থ হয়েছে", "Registration failed")
            }
            _authLoading.value = false
        }
    }

    fun enterDevMode(userId: String, role: String) {
        viewModelScope.launch {
            _authError.value = null
            _authInfo.value = null
            prefsStore.setIdentity(userId, role)
            val l = prefs.value.lang
            toastMsg(tr(l, "ডেভ মোডে প্রবেশ: $userId ($role)", "Entered Dev Mode as $userId ($role)"))
        }
    }

    fun logout() {
        viewModelScope.launch {
            val srv = prefs.value.serverBaseUrl
            val tok = prefs.value.authToken
            GrowthTwinApiClient.serverLogout(srv, tok)
            prefsStore.logout()
            _authError.value = null
            _authInfo.value = null
            toastMsg(tr(prefs.value.lang, "লগআউট হয়েছে", "Signed out"))
        }
    }

    fun saveAndTestServerUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        viewModelScope.launch {
            prefsStore.setServerBaseUrl(clean)
            if (clean.isBlank()) {
                _serverStatusText.value = "Local Engine + Open-Meteo Weather"
                return@launch
            }
            _serverStatusText.value = "Checking $clean/auth/config ..."
            GrowthTwinApiClient.pingServer(clean).fold(
                onSuccess = { meta ->
                    _serverStatusText.value = "OK • auth_required=${meta.authRequired} • store=${meta.provider}"
                    toastMsg(tr(prefs.value.lang, "সার্ভার সংযুক্ত: $clean", "Connected to $clean"))
                },
                onFailure = { err ->
                    _serverStatusText.value = "Unreachable: ${err.message}"
                    toastMsg(err.message ?: "Server unreachable")
                }
            )
        }
    }

    fun refreshLiveWeather(lat: Double = 23.8103, lon: Double = 90.4125, locationName: String = "Dhaka") {
        viewModelScope.launch {
            _liveWeatherLoading.value = true
            GrowthTwinApiClient.fetchLiveWeather(lat, lon, locationName).onSuccess { snap ->
                _liveWeather.value = snap
            }
            _liveWeatherLoading.value = false
        }
    }

    // ---- Section 1 Advisor ----
    fun setAdvisorPrompt(text: String) { _advisorPrompt.value = text }

    fun submitAdvisorPrompt(promptText: String = _advisorPrompt.value) {
        val q = promptText.trim()
        if (q.isEmpty()) return
        _advisorPrompt.value = q
        viewModelScope.launch {
            val p = prefs.value
            val sid = UUID.randomUUID().toString().take(8)
            _advisorSessionId.value = sid
            val extracted = AdvisorEngine.extractSlots(q)
            _advisorSlots.value = extracted
            _advisorAsked.value = mutableListOf()
            val stats = repo.getHarvestStats(p.userId)
            val rep = AdvisorEngine.evaluate(sid, q, extracted, emptyList(), stats)
            _advisorReport.value = rep
            repo.saveAdvisorSession(sid, p.userId, q, extracted, emptyList())

            // Optional real Groq LLM or backend API enrichment
            if (GrowthTwinApiClient.hasGroqConfig()) {
                val baseSummary = if (p.lang == AppLang.BN) rep.narrativeBn else rep.narrativeEn
                GrowthTwinApiClient.groqAdvisorNarrative(q, baseSummary, p.lang.code).onSuccess { aiNote ->
                    val cur = _advisorReport.value ?: return@onSuccess
                    if (cur.sessionId == sid) {
                        _advisorReport.value = if (p.lang == AppLang.BN) {
                            cur.copy(narrativeBn = "${cur.narrativeBn}\n\n[AI ব্যাখ্যা]\n$aiNote")
                        } else {
                            cur.copy(narrativeEn = "${cur.narrativeEn}\n\n[AI Summary]\n$aiNote")
                        }
                    }
                }
            }
            if (p.serverBaseUrl.isNotBlank()) {
                GrowthTwinApiClient.serverAdvisorAsk(p.serverBaseUrl, p.authToken, p.userId, p.userRole, q)
            }
        }
    }

    fun answerAdvisorSlot(slot: String, value: String) {
        viewModelScope.launch {
            val p = prefs.value
            val cur = _advisorSlots.value.toMutableMap()
            val asked = _advisorAsked.value.toMutableList()
            if (slot !in asked) asked.add(slot)
            if (value == "unknown") {
                val def = AdvisorEngine.defaultValues()[slot] ?: value
                cur[slot] = SlotValue(def, "assumed")
            } else {
                cur[slot] = SlotValue(value, "user")
            }
            _advisorSlots.value = cur
            _advisorAsked.value = asked
            val stats = repo.getHarvestStats(p.userId)
            val sid = _advisorSessionId.value
            val rep = AdvisorEngine.evaluate(sid, _advisorPrompt.value, cur, asked, stats, finishNow = false)
            _advisorReport.value = rep
            repo.saveAdvisorSession(sid, p.userId, _advisorPrompt.value, cur, asked)
        }
    }

    fun updateAdvisorPh(phVal: Double) {
        viewModelScope.launch {
            val p = prefs.value
            val cur = _advisorSlots.value.toMutableMap()
            cur["ph"] = SlotValue(phVal.coerceIn(3.0, 10.0).toString(), "user")
            _advisorSlots.value = cur
            val stats = repo.getHarvestStats(p.userId)
            val sid = _advisorSessionId.value
            val rep = AdvisorEngine.evaluate(sid, _advisorPrompt.value, cur, _advisorAsked.value, stats, finishNow = true)
            _advisorReport.value = rep
            repo.saveAdvisorSession(sid, p.userId, _advisorPrompt.value, cur, _advisorAsked.value, rep.reviewStatus, rep.expertVerdict, rep.expertNote)
            toastMsg(tr(p.lang, "pH আপডেট করে পুনরায় হিসাব করা হয়েছে।", "Re-evaluated with pH $phVal."))
        }
    }

    fun requestAdvisorExpertReview() {
        val rep = _advisorReport.value ?: return
        viewModelScope.launch {
            val p = prefs.value
            repo.saveAdvisorSession(
                rep.sessionId, p.userId, rep.prompt, _advisorSlots.value, _advisorAsked.value,
                reviewStatus = "requested"
            )
            _advisorReport.value = rep.copy(reviewStatus = "requested")
            toastMsg(tr(p.lang, "বিশেষজ্ঞ রিভিউয়ের জন্য পাঠানো হয়েছে।", "Submitted to Expert Review queue."))
        }
    }

    // ---- Section 2 Plot Twin ----
    fun selectPlot(plotId: String) {
        _selectedPlotId.value = plotId
        plots.value.firstOrNull { it.id == plotId }?.let { p ->
            IrrigationController.seedFromPlot(p.id, p.awPct())
            _irrigationView.value = IrrigationController.view(p.id)
        }
    }

    fun createNewPlot(name: String, cropId: String, soil: String, area: Double, ph: Double?, ec: Double?, autoIrr: Boolean, autoInputs: Boolean) {
        viewModelScope.launch {
            val userId = prefs.value.userId
            val plot = PlotTwinEngine.newPlot(
                userId = userId, name = name, cropId = cropId, soil = soil,
                area = area, ph = ph, ec = ec,
                config = PlotConfig(autoIrrigation = autoIrr, autoInputs = autoInputs)
            )
            val ev = TwinEvent(0, "status", "নতুন প্লট তৈরি হয়েছে: ${plot.name}।", "Created plot: ${plot.name}.")
            repo.savePlotAndSideEffects(plot, events = listOf(ev))
            _selectedPlotId.value = plot.id
            IrrigationController.seedFromPlot(plot.id, plot.awPct())
            _irrigationView.value = IrrigationController.view(plot.id)
            toastMsg(tr(prefs.value.lang, "প্লট তৈরি হয়েছে", "Plot created"))
        }
    }

    fun advanceSelectedPlot(days: Int, stopOnAlert: Boolean, autoHarvest: Boolean) {
        val p = selectedPlot.value ?: return
        if (p.status !in setOf("growing", "ready")) {
            toastMsg(tr(prefs.value.lang, "এই ফসলের জীবনচক্র শেষ", "Crop lifecycle already ended"))
            return
        }
        viewModelScope.launch {
            val copy = p.deepCopy()
            val out = PlotTwinEngine.advance(copy, days, stopOnAlert, autoHarvest)
            repo.savePlotAndSideEffects(copy, out.logs, out.events)
            val l = prefs.value.lang
            if (out.stoppedForBn != null) {
                toastMsg(tr(l, "সতর্কতায় থামলো (${out.logs.size} দিন): ${out.stoppedForBn}", "Paused on alert (${out.logs.size}d): ${out.stoppedForEn}"))
            } else {
                toastMsg(tr(l, "${out.logs.size} দিন এগিয়ে নেওয়া হয়েছে", "Simulated ${out.logs.size} day(s)"))
            }
        }
    }

    fun applyPlotAction(action: TwinActionSpec) {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            try {
                val copy = p.deepCopy()
                val ev = PlotTwinEngine.applyAction(copy, action.type, nutrient = action.nutrient, pumpOnVal = action.on ?: false)
                repo.savePlotAndSideEffects(copy, events = listOf(ev))
                val l = prefs.value.lang
                toastMsg(if (l == AppLang.BN) ev.textBn else ev.textEn)
            } catch (e: Exception) {
                toastMsg(e.message ?: "Error")
            }
        }
    }

    fun answerPlotQuestion(qid: String, valStr: String) {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            val copy = p.deepCopy()
            val evs = PlotTwinEngine.answerQuestions(copy, mapOf(qid to valStr))
            repo.savePlotAndSideEffects(copy, events = evs)
            val l = prefs.value.lang
            evs.firstOrNull()?.let { toastMsg(if (l == AppLang.BN) it.textBn else it.textEn) }
        }
    }

    fun submitManualReading(ph: Double?, ec: Double?, moisturePct: Double?) {
        val p = selectedPlot.value ?: return
        if (ph == null && ec == null && moisturePct == null) {
            toastMsg(tr(prefs.value.lang, "কমপক্ষে একটি মান দিন", "Enter at least one reading"))
            return
        }
        viewModelScope.launch {
            val copy = p.deepCopy()
            var correctedMoisture = moisturePct
            if (moisturePct != null) {
                val gap = SentinelEngine.correctState(copy.awPct(), moisturePct, quality = "valid")
                correctedMoisture = gap.estimatedValue
            }
            val ev = PlotTwinEngine.applyReading(copy, ph, ec, correctedMoisture, "manual")
            repo.savePlotAndSideEffects(copy, events = listOf(ev))
            toastMsg(if (prefs.value.lang == AppLang.BN) ev.textBn else ev.textEn)
        }
    }

    fun inspectLeafPhoto(uri: Uri) {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            val l = prefs.value.lang
            val bmp = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            val pred = PlotTwinEngine.inspectLeafBitmap(bmp, p.cropId)
            val copy = p.deepCopy()
            val evs = PlotTwinEngine.recordPhoto(copy, pred)
            repo.savePlotAndSideEffects(copy, events = evs)
            evs.firstOrNull()?.let { toastMsg(if (l == AppLang.BN) it.textBn else it.textEn) }
        }
    }

    fun runPlotExperiment(name: String, horizonDays: Int, intervention: String, irrigationAuto: Boolean?, nutrient: String?) {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            val copy = p.deepCopy()
            val rec = PlotTwinEngine.runExperiment(copy, name, horizonDays, intervention, irrigationAuto, nutrient)
            repo.savePlot(copy)
            toastMsg(tr(prefs.value.lang, "পরীক্ষা সম্পন্ন: ${rec.name}", "Experiment completed: ${rec.name}"))
        }
    }

    fun updatePlotConfig(autoIrr: Boolean, autoInputs: Boolean) {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            val copy = p.deepCopy().copy(config = p.config.copy(autoIrrigation = autoIrr, autoInputs = autoInputs))
            repo.savePlot(copy)
            toastMsg(tr(prefs.value.lang, "সেটিংস সংরক্ষিত হয়েছে", "Plot settings updated"))
        }
    }

    fun deleteSelectedPlot() {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            repo.deletePlot(p.id)
            _selectedPlotId.value = null
            toastMsg(tr(prefs.value.lang, "প্লট মুছে ফেলা হয়েছে", "Plot deleted"))
        }
    }

    fun speakPlotSummary() {
        val p = selectedPlot.value ?: return
        val l = prefs.value.lang
        val text = PlotTwinEngine.buildSummaryText(p, l)
        if (!_ttsReady.value) {
            toastMsg(text)
            return
        }
        val locale = if (l == AppLang.BN) Locale("bn", "BD") else Locale.US
        val res = tts?.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gt_summary_${p.id}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _ttsSpeaking.value = false
    }

    // ---- Section 2b Irrigation Control ----
    fun startPump(minutes: Int, force: Boolean = false) {
        val p = selectedPlot.value ?: return
        val res = IrrigationController.startPump(p.id, minutes, force)
        res.onSuccess {
            viewModelScope.launch {
                val copy = p.deepCopy()
                val ev = PlotTwinEngine.applyAction(copy, "pump", pumpOnVal = true)
                repo.savePlotAndSideEffects(copy, events = listOf(ev))
            }
            _irrigationView.value = IrrigationController.view(p.id)
            toastMsg(tr(prefs.value.lang, "পাম্প চালু হয়েছে ($minutes মিনিট)", "Pump started ($minutes min)"))
        }.onFailure { err ->
            toastMsg(err.message ?: "Cannot start pump")
        }
    }

    fun stopPump() {
        val p = selectedPlot.value ?: return
        val sync = IrrigationController.stopPump(p.id)
        _irrigationView.value = IrrigationController.view(p.id)
        if (sync != null) {
            viewModelScope.launch {
                val copy = p.deepCopy()
                val evs = mutableListOf(PlotTwinEngine.applyAction(copy, "pump", pumpOnVal = false))
                val mm = (sync.second / 60.0 * IrrigationController.MM_PER_MIN).coerceAtMost(60.0)
                if (mm > 0.2) evs.add(PlotTwinEngine.applyAction(copy, "irrigate", mmOverride = mm))
                repo.savePlotAndSideEffects(copy, events = evs)
            }
        }
        toastMsg(tr(prefs.value.lang, "পাম্প বন্ধ করা হয়েছে", "Pump stopped"))
    }

    fun updateSchedule(enabled: Boolean, everyMinutes: Int, minutes: Int, onlyIfDry: Boolean) {
        val p = selectedPlot.value ?: return
        val res = IrrigationController.updateSchedule(p.id, enabled, everyMinutes, minutes, onlyIfDry)
        res.onSuccess {
            _irrigationView.value = IrrigationController.view(p.id)
            toastMsg(tr(prefs.value.lang, "অটো সময়সূচী সংরক্ষিত", "Auto schedule updated"))
        }.onFailure { err ->
            toastMsg(err.message ?: "Invalid schedule")
        }
    }

    fun setDemoMoisture(pct: Double) {
        val p = selectedPlot.value ?: return
        IrrigationController.setDemoMoisture(p.id, pct)
            .onSuccess { _irrigationView.value = IrrigationController.view(p.id) }
            .onFailure { toastMsg(it.message ?: "Real sensor active") }
    }

    // ---- Sentinel Monitor & Calibration ----
    fun selectSentinelScenario(id: String) { _sentinelScenarioId.value = id }

    fun toggleChecklist(id: String) {
        viewModelScope.launch { prefsStore.toggleChecklistItem(id) }
    }

    fun resetChecklist() {
        viewModelScope.launch { prefsStore.resetChecklist() }
    }

    fun generateSimulatedTelemetry() {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            repo.generateSimulatedTelemetry(p, count = 10)
            toastMsg(tr(prefs.value.lang, "১০টি সিমুলেটেড টেলিমেট্রি তৈরি হয়েছে (ডেমো)", "Generated 10 simulated demo readings"))
        }
    }

    fun saveCalibration(deviceId: String, wp: Int, fc: Int) {
        val p = selectedPlot.value ?: return
        val cal = MoistureCalibration(deviceId = deviceId.ifBlank { "ESP32-NODE-01" }, adcAtWiltingPoint = wp, adcAtFieldCapacity = fc)
        val err = cal.validate()
        if (err != null) {
            toastMsg(err)
            return
        }
        viewModelScope.launch {
            val copy = p.deepCopy().apply { sensorConfig = cal }
            repo.savePlot(copy)
            toastMsg(tr(prefs.value.lang, "সেন্সর ক্যালিব্রেশন সংরক্ষিত হয়েছে", "Sensor calibration saved for ${cal.deviceId}"))
        }
    }

    fun ingestTestDevicePacket(deviceId: String, rawAdc: Int, tempC: Double?, humidityPct: Double?) {
        val p = selectedPlot.value ?: return
        viewModelScope.launch {
            val seq = (telemetryList.value.maxOfOrNull { it.sequence } ?: 0) + 1
            val copy = p.deepCopy()
            val res = repo.ingestDeviceReading(copy, deviceId.ifBlank { "ESP32-NODE-01" }, seq, rawAdc, tempC, humidityPct)
            res.onSuccess { ent ->
                if (ent.measurementUsed) {
                    IrrigationController.pushSensorReading(copy.id, ent.correctedMoisturePct)
                }
                val srv = prefs.value.serverBaseUrl
                if (srv.isNotBlank()) {
                    GrowthTwinApiClient.serverIngestSensor(srv, copy.id, ent.deviceId, seq, rawAdc, tempC, humidityPct)
                }
                toastMsg(
                    tr(
                        prefs.value.lang,
                        "প্যাকেট গৃহীত (${ent.quality}): ${ent.calibratedMoisturePct}% → সংশোধিত ${ent.correctedMoisturePct}%",
                        "Ingested (${ent.quality}): ${ent.calibratedMoisturePct}% → est. ${ent.correctedMoisturePct}%"
                    )
                )
            }.onFailure { err ->
                toastMsg(err.message ?: "Packet rejected")
            }
        }
    }

    // ---- SolarFlow AI ----
    fun updateSolarFlow(updated: SolarFlowInput) {
        _solarInput.value = updated
        _solarResult.value = SolarFlowEngine.recommend(updated)
        val srv = prefs.value.serverBaseUrl
        if (srv.isNotBlank()) {
            viewModelScope.launch {
                GrowthTwinApiClient.serverSolarFlowRecommend(srv, prefs.value.authToken, updated)
                    .onSuccess { remoteRes -> _solarResult.value = remoteRes }
            }
        }
    }

    fun applyLiveWeatherToSolarFlow() {
        val w = _liveWeather.value ?: return
        val today = w.forecast.firstOrNull() ?: return
        val updated = _solarInput.value.copy(
            forecastRainMm = today.rainMm,
            rainProbability = (today.prob / 100.0).coerceIn(0.0, 1.0)
        )
        updateSolarFlow(updated)
        toastMsg(tr(prefs.value.lang, "লাইভ বৃষ্টির পূর্বাভাস (${today.rainMm} মিমি) যুক্ত হয়েছে", "Applied live rain forecast (${today.rainMm} mm)"))
    }

    fun resetSolarFlow() {
        val def = SolarFlowInput()
        _solarInput.value = def
        _solarResult.value = SolarFlowEngine.recommend(def)
    }

    // ---- Fair Market ----
    fun setMarketQuery(q: String) { _marketQuery.value = q }
    fun setMarketQty(qty: String) { _marketQty.value = qty }

    fun analyzeMarket(queryOverride: String? = null) {
        val q = (queryOverride ?: _marketQuery.value).trim()
        if (q.isEmpty()) return
        _marketQuery.value = q
        val qty = _marketQty.value.toDoubleOrNull() ?: 100.0
        val local = FairMarketEngine.analyze(q, qty)
        _marketAnalysis.value = local
        val srv = prefs.value.serverBaseUrl
        val tok = prefs.value.authToken
        if (srv.isNotBlank() && tok.isNotBlank()) {
            viewModelScope.launch {
                GrowthTwinApiClient.serverFairMarketAsk(srv, tok, q).onSuccess { j ->
                    val ans = j.optString("answer")
                        .ifBlank { j.optString("answer_bn") }
                        .ifBlank { j.optString("answer_en") }
                        .ifBlank { j.optString("reply") }
                    if (ans.isNotBlank()) {
                        _marketAnalysis.value = _marketAnalysis.value.copy(
                            answerBn = "${local.answerBn}\n\n[সার্ভার তথ্য]\n$ans",
                            answerEn = "${local.answerEn}\n\n[Live Server]\n$ans"
                        )
                    }
                }
            }
        }
    }

    // ---- Trust ----
    fun selectTrustCrop(cropId: String) { _trustCropId.value = cropId }

    // ---- Messaging Demo ----
    fun generateLinkCode() {
        val code = (100000..999999).random().toString()
        viewModelScope.launch {
            prefsStore.setWhatsappLinkCode(code)
            toastMsg(tr(prefs.value.lang, "লিংক কোড তৈরি: LINK $code", "Generated code: LINK $code"))
        }
    }

    fun simulateWhatsappLink(phone: String) {
        val clean = phone.trim()
        if (clean.length < 10) {
            toastMsg(tr(prefs.value.lang, "সঠিক ১১ ডিজিটের মোবাইল নম্বর দিন", "Enter a valid mobile number"))
            return
        }
        viewModelScope.launch {
            prefsStore.setWhatsappDemoLinked(true, clean)
            toastMsg(tr(prefs.value.lang, "ডেমো WhatsApp সংযুক্ত হয়েছে", "Demo WhatsApp linked"))
        }
    }

    fun unlinkWhatsappDemo() {
        viewModelScope.launch {
            prefsStore.setWhatsappDemoLinked(false, "")
        }
    }

    fun sendChannelCommand(rawText: String) {
        val cmd = rawText.trim()
        if (cmd.isEmpty()) return
        val lower = cmd.lowercase(Locale.ROOT)
        val l = prefs.value.lang
        val p = selectedPlot.value
        val code = prefs.value.whatsappLinkCode

        val reply = when {
            lower.startsWith("link") -> {
                val token = cmd.split(Regex("\\s+")).getOrNull(1).orEmpty()
                if (code.isNotEmpty() && token == code) {
                    viewModelScope.launch { prefsStore.setWhatsappDemoLinked(true, "01712345678") }
                    tr(l, "✅ ফোন সফলভাবে যুক্ত হয়েছে (${prefs.value.userId})।", "✅ Phone linked to ${prefs.value.userId}.")
                } else {
                    tr(l, "⚠️ প্রথমে কোড তৈরি করে 'LINK $code' পাঠান।", "⚠️ Generate a code first and send 'LINK <code>'.")
                }
            }
            "অবস্থা" in lower || "status" in lower -> {
                if (p == null) tr(l, "কোনো সক্রিয় প্লট নেই।", "No active plot found.")
                else PlotTwinEngine.buildSummaryText(p, l)
            }
            "সেচ" in lower || "irrig" in lower || "pump" in lower -> {
                val v = _irrigationView.value
                if (p == null || v == null) tr(l, "প্লট নির্বাচন করুন।", "Select a plot first.")
                else tr(
                    l,
                    "💧 ${p.name}: আর্দ্রতা ${v.moisturePct.toInt()}%, পাম্প ${if (v.pump != null) "চালু" else "বন্ধ"}, ২৪ঘণ্টায় বাকি ${v.left24hMin.toInt()} মিনিট।",
                    "💧 ${p.name}: Moisture ${v.moisturePct.toInt()}%, Pump ${if (v.pump != null) "ON" else "OFF"}, 24h cap left ${v.left24hMin.toInt()} min."
                )
            }
            "দাম" in lower || "price" in lower || "বাজার" in lower -> {
                val res = FairMarketEngine.analyze(cmd, 100.0)
                if (l == AppLang.BN) res.answerBn else res.answerEn
            }
            else -> tr(
                l,
                "কমান্ড: 'অবস্থা' (প্লট সারাংশ), 'সেচ' (পাম্প ও আর্দ্রতা), 'দাম আলু' (বাজারদর), অথবা 'LINK <কোড>'।",
                "Commands: 'STATUS' (plot summary), 'IRRIGATE' (pump & moisture), 'PRICE potato', or 'LINK <code>'."
            )
        }

        _channelMessages.value = (_channelMessages.value + listOf(
            ChannelSimMessage(UUID.randomUUID().toString().take(8), fromFarmer = true, text = cmd),
            ChannelSimMessage(UUID.randomUUID().toString().take(8), fromFarmer = false, text = reply)
        )).takeLast(12)
    }

    // ---- Expert / Admin ----
    fun resolveAdvisorQueueItem(sessionId: String, verdict: String, note: String) {
        viewModelScope.launch {
            val row = db.advisorDao().getById(sessionId) ?: return@launch
            db.advisorDao().upsert(
                row.copy(
                    reviewStatus = "reviewed",
                    expertVerdict = verdict,
                    expertNote = note,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            if (_advisorReport.value?.sessionId == sessionId) {
                _advisorReport.value = _advisorReport.value?.copy(
                    reviewStatus = "reviewed",
                    expertVerdict = verdict,
                    expertNote = note
                )
            }
            toastMsg(tr(prefs.value.lang, "পরামর্শ রিভিউ সংরক্ষিত", "Advisor review saved"))
        }
    }

    fun resolvePhotoQueueItem(plotId: String, reviewId: String, verdict: String, label: String, note: String) {
        viewModelScope.launch {
            val targetPlot = plots.value.firstOrNull { it.id == plotId } ?: return@launch
            val copy = targetPlot.deepCopy()
            val ev = PlotTwinEngine.resolvePhotoReview(copy, reviewId, verdict, label, note, prefs.value.userId)
            repo.savePlotAndSideEffects(copy, events = listOf(ev))
            toastMsg(if (prefs.value.lang == AppLang.BN) ev.textBn else ev.textEn)
        }
    }

    override fun onCleared() {
        tts?.shutdown()
        super.onCleared()
    }
}
