package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AdvisorScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExpertScreen
import com.example.ui.screens.FairMarketScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IrrigationScreen
import com.example.ui.screens.PlotTwinScreen
import com.example.ui.screens.Routes
import com.example.ui.screens.SentinelScreen
import com.example.ui.screens.SolarFlowScreen
import com.example.ui.screens.TrustAndMessagingScreen
import com.example.ui.theme.AppLang
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr
import com.example.ui.viewmodel.GrowthTwinViewModel
import kotlinx.coroutines.launch

private data class NavDestination(
    val route: String,
    val icon: ImageVector,
    val labelBn: String,
    val labelEn: String,
    val primaryBar: Boolean = false
)

private val AllDestinations = listOf(
    NavDestination(Routes.HOME, Icons.Default.Home, "হোম", "Home", primaryBar = true),
    NavDestination(Routes.ADVISOR, Icons.Default.Lightbulb, "পরামর্শ", "Advisor", primaryBar = true),
    NavDestination(Routes.TWIN, Icons.Default.Eco, "টুইন", "Twin", primaryBar = true),
    NavDestination(Routes.IRRIGATION, Icons.Default.WaterDrop, "সেচ", "Pump", primaryBar = true),
    NavDestination(Routes.MARKET, Icons.Default.Storefront, "বাজার", "Market", primaryBar = true),
    NavDestination(Routes.SENTINEL, Icons.Default.Sensors, "সেন্টিনেল", "Sentinel"),
    NavDestination(Routes.SOLARFLOW, Icons.Default.WbSunny, "সোলারফ্লো", "SolarFlow"),
    NavDestination(Routes.TRUST, Icons.Default.Groups, "ভরসা ও বার্তা", "Trust & Msg"),
    NavDestination(Routes.EXPERT, Icons.Default.AdminPanelSettings, "বিশেষজ্ঞ", "Expert")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppShell(vm: GrowthTwinViewModel) {
    val c = GTTheme.colors
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val lang = prefs.lang

    val plots by vm.plots.collectAsStateWithLifecycle()
    val selectedPlot by vm.selectedPlot.collectAsStateWithLifecycle()
    val plotLogs by vm.plotLogs.collectAsStateWithLifecycle()
    val plotEvents by vm.plotEvents.collectAsStateWithLifecycle()
    val telemetryList by vm.telemetryList.collectAsStateWithLifecycle()
    val irrigationView by vm.irrigationView.collectAsStateWithLifecycle()
    val advisorPrompt by vm.advisorPrompt.collectAsStateWithLifecycle()
    val advisorReport by vm.advisorReport.collectAsStateWithLifecycle()
    val sentinelScenarioId by vm.sentinelScenarioId.collectAsStateWithLifecycle()
    val solarInput by vm.solarInput.collectAsStateWithLifecycle()
    val solarResult by vm.solarResult.collectAsStateWithLifecycle()
    val marketQuery by vm.marketQuery.collectAsStateWithLifecycle()
    val marketQty by vm.marketQty.collectAsStateWithLifecycle()
    val marketAnalysis by vm.marketAnalysis.collectAsStateWithLifecycle()
    val trustCropId by vm.trustCropId.collectAsStateWithLifecycle()
    val trustReport by vm.trustReport.collectAsStateWithLifecycle()
    val channelMessages by vm.channelMessages.collectAsStateWithLifecycle()
    val expertPhotoQueue by vm.expertPhotoQueue.collectAsStateWithLifecycle()
    val expertAdvisorQueue by vm.expertAdvisorQueue.collectAsStateWithLifecycle()
    val ttsSpeaking by vm.ttsSpeaking.collectAsStateWithLifecycle()
    val authLoading by vm.authLoading.collectAsStateWithLifecycle()
    val authError by vm.authError.collectAsStateWithLifecycle()
    val authInfo by vm.authInfo.collectAsStateWithLifecycle()
    val serverStatusText by vm.serverStatusText.collectAsStateWithLifecycle()
    val liveWeather by vm.liveWeather.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.toast.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (!prefs.isLoggedIn) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(
                lang = lang,
                darkTheme = prefs.darkTheme,
                serverBaseUrl = prefs.serverBaseUrl,
                serverStatusText = serverStatusText,
                loading = authLoading,
                errorText = authError,
                infoText = authInfo,
                onToggleLang = vm::toggleLang,
                onToggleTheme = vm::toggleTheme,
                onLogin = vm::login,
                onRegister = vm::register,
                onEnterDevMode = vm::enterDevMode,
                onTestServerUrl = vm::saveAndTestServerUrl
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showRoleDialog by remember { mutableStateOf(false) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showRoleDialog) {
        RoleSwitcherDialog(
            lang = lang,
            currentUserId = prefs.userId,
            currentName = prefs.userName,
            currentEmail = prefs.userEmail,
            currentProvider = prefs.authProvider,
            currentRole = prefs.userRole,
            serverBaseUrl = prefs.serverBaseUrl,
            serverStatusText = serverStatusText,
            onDismiss = { showRoleDialog = false },
            onConfirm = { uid, role ->
                vm.setIdentity(uid, role)
                showRoleDialog = false
            },
            onTestServerUrl = vm::saveAndTestServerUrl,
            onLogout = {
                showRoleDialog = false
                vm.logout()
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(c.bg)) {
        val isWide = maxWidth >= 600.dp

        if (c.isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1FA3F20A), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.maxDimension * 0.45f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x193DE0A0), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.78f),
                        radius = size.maxDimension * 0.48f
                    )
                )
            }
        }

        val scaffoldBody: @Composable () -> Unit = {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = c.surface,
                            titleContentColor = c.ink,
                            navigationIconContentColor = c.ink,
                            actionIconContentColor = c.ink
                        ),
                        navigationIcon = {
                            if (!isWide) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("shell_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = tr(lang, "মেনু খুলুন", "Open navigation menu")
                                    )
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Growth")
                                        withStyle(SpanStyle(color = c.lime)) { append("Twin") }
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                selectedPlot?.let { p ->
                                    Text(
                                        text = "${p.name} · ${tr(lang, "দিন ${p.day}", "Day ${p.day}")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.mute,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            OutlinedButton(
                                onClick = vm::toggleLang,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("shell_lang_toggle")
                            ) {
                                Text(
                                    text = if (lang == AppLang.BN) "EN" else "বাং",
                                    fontFamily = MonoFont,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            IconButton(
                                onClick = vm::toggleTheme,
                                modifier = Modifier.testTag("shell_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (prefs.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = tr(lang, "দিন/রাত থিম বদলান", "Toggle day or night theme"),
                                    tint = c.warn
                                )
                            }
                            IconButton(
                                onClick = { showRoleDialog = true },
                                modifier = Modifier.testTag("shell_role_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = tr(lang, "ভূমিকা ও একাউন্ট", "Switch user role"),
                                    tint = if (prefs.userRole == "farmer") c.mint else c.lime
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWide) {
                        NavigationBar(containerColor = c.surface, contentColor = c.ink) {
                            AllDestinations.filter { it.primaryBar }.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = tr(lang, dest.labelBn, dest.labelEn)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tr(lang, dest.labelBn, dest.labelEn),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWide) {
                        NavigationRail(
                            containerColor = c.surface,
                            contentColor = c.ink,
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(8.dp))
                            AllDestinations.forEach { dest ->
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = tr(lang, dest.labelBn, dest.labelEn)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tr(lang, dest.labelBn, dest.labelEn),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("rail_${dest.route}")
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = Routes.HOME,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(Routes.HOME) {
                                    HomeScreen(
                                        lang = lang,
                                        activePlot = selectedPlot,
                                        userRole = prefs.userRole,
                                        sentinelScenarioId = sentinelScenarioId,
                                        telemetryCount = telemetryList.size,
                                        liveWeather = liveWeather,
                                        serverConnected = prefs.serverBaseUrl.isNotBlank(),
                                        onRefreshWeather = { vm.refreshLiveWeather() },
                                        onSelectSentinelScenario = vm::selectSentinelScenario,
                                        onNavigate = ::navigateTo
                                    )
                                }
                                composable(Routes.ADVISOR) {
                                    AdvisorScreen(
                                        lang = lang,
                                        promptText = advisorPrompt,
                                        report = advisorReport,
                                        onPromptChange = vm::setAdvisorPrompt,
                                        onSubmitPrompt = vm::submitAdvisorPrompt,
                                        onAnswerSlot = vm::answerAdvisorSlot,
                                        onUpdatePh = vm::updateAdvisorPh,
                                        onRequestReview = vm::requestAdvisorExpertReview
                                    )
                                }
                                composable(Routes.TWIN) {
                                    PlotTwinScreen(
                                        lang = lang,
                                        plots = plots,
                                        selectedPlot = selectedPlot,
                                        logs = plotLogs,
                                        events = plotEvents,
                                        ttsSpeaking = ttsSpeaking,
                                        onSelectPlot = vm::selectPlot,
                                        onCreatePlot = vm::createNewPlot,
                                        onAdvance = vm::advanceSelectedPlot,
                                        onAction = vm::applyPlotAction,
                                        onAnswerQuestion = vm::answerPlotQuestion,
                                        onManualReading = vm::submitManualReading,
                                        onInspectPhoto = vm::inspectLeafPhoto,
                                        onRunExperiment = vm::runPlotExperiment,
                                        onUpdateConfig = vm::updatePlotConfig,
                                        onDeletePlot = vm::deleteSelectedPlot,
                                        onSpeakSummary = vm::speakPlotSummary,
                                        onStopSpeaking = vm::stopSpeaking
                                    )
                                }
                                composable(Routes.IRRIGATION) {
                                    IrrigationScreen(
                                        lang = lang,
                                        plot = selectedPlot,
                                        view = irrigationView,
                                        onStartPump = vm::startPump,
                                        onStopPump = vm::stopPump,
                                        onSetDemoMoisture = vm::setDemoMoisture,
                                        onUpdateSchedule = vm::updateSchedule
                                    )
                                }
                                composable(Routes.SENTINEL) {
                                    SentinelScreen(
                                        lang = lang,
                                        plot = selectedPlot,
                                        telemetry = telemetryList,
                                        selectedScenarioId = sentinelScenarioId,
                                        checklistDone = prefs.checklistDone,
                                        onSelectScenario = vm::selectSentinelScenario,
                                        onToggleChecklist = vm::toggleChecklist,
                                        onResetChecklist = vm::resetChecklist,
                                        onGenerateSimulation = vm::generateSimulatedTelemetry,
                                        onSaveCalibration = vm::saveCalibration,
                                        onIngestPacket = vm::ingestTestDevicePacket
                                    )
                                }
                                composable(Routes.SOLARFLOW) {
                                    SolarFlowScreen(
                                        lang = lang,
                                        input = solarInput,
                                        result = solarResult,
                                        liveWeather = liveWeather,
                                        onApplyLiveRain = vm::applyLiveWeatherToSolarFlow,
                                        onUpdateInput = vm::updateSolarFlow,
                                        onResetDemo = vm::resetSolarFlow
                                    )
                                }
                                composable(Routes.MARKET) {
                                    FairMarketScreen(
                                        lang = lang,
                                        query = marketQuery,
                                        qtyText = marketQty,
                                        analysis = marketAnalysis,
                                        onQueryChange = vm::setMarketQuery,
                                        onQtyChange = vm::setMarketQty,
                                        onAnalyze = vm::analyzeMarket
                                    )
                                }
                                composable(Routes.TRUST) {
                                    TrustAndMessagingScreen(
                                        lang = lang,
                                        prefs = prefs,
                                        trustCropId = trustCropId,
                                        trustReport = trustReport,
                                        channelMessages = channelMessages,
                                        onSelectTrustCrop = vm::selectTrustCrop,
                                        onGenerateLinkCode = vm::generateLinkCode,
                                        onSimulateWhatsappLink = vm::simulateWhatsappLink,
                                        onUnlinkWhatsapp = vm::unlinkWhatsappDemo,
                                        onSendCommand = vm::sendChannelCommand
                                    )
                                }
                                composable(Routes.EXPERT) {
                                    ExpertScreen(
                                        lang = lang,
                                        userId = prefs.userId,
                                        userRole = prefs.userRole,
                                        advisorQueue = expertAdvisorQueue,
                                        photoQueue = expertPhotoQueue,
                                        onSwitchRole = vm::setIdentity,
                                        onResolveAdvisor = vm::resolveAdvisorQueueItem,
                                        onResolvePhoto = vm::resolvePhotoQueueItem
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isWide) {
            scaffoldBody()
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = c.surface,
                        drawerContentColor = c.ink,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append("Growth")
                                    withStyle(SpanStyle(color = c.lime)) { append("Twin") }
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                            Surface(
                                color = c.surfaceCard,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = prefs.userName.ifBlank { prefs.userId },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = c.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${prefs.userRole.uppercase()} · ${prefs.authProvider.uppercase()}",
                                            fontFamily = MonoFont,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c.lime
                                        )
                                    }
                                    TextButton(onClick = {
                                        scope.launch { drawerState.close() }
                                        showRoleDialog = true
                                    }) {
                                        Text(tr(lang, "একাউন্ট", "Account"))
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = c.line)
                            AllDestinations.forEach { dest ->
                                NavigationDrawerItem(
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigateTo(dest.route)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = tr(lang, dest.labelBn, dest.labelEn)
                                        )
                                    },
                                    label = { Text(tr(lang, dest.labelBn, dest.labelEn)) },
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                        .testTag("drawer_${dest.route}")
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = c.line)
                            NavigationDrawerItem(
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    vm.logout()
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = tr(lang, "লগ আউট", "Sign out")
                                    )
                                },
                                label = { Text(tr(lang, "লগ আউট", "Sign Out")) },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .testTag("drawer_logout")
                            )
                        }
                    }
                },
                content = scaffoldBody
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleSwitcherDialog(
    lang: AppLang,
    currentUserId: String,
    currentName: String,
    currentEmail: String,
    currentProvider: String,
    currentRole: String,
    serverBaseUrl: String,
    serverStatusText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onTestServerUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    val c = GTTheme.colors
    var uid by remember { mutableStateOf(currentUserId) }
    var role by remember { mutableStateOf(currentRole) }
    var serverUrl by remember(serverBaseUrl) { mutableStateOf(serverBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(tr(lang, "একাউন্ট, সার্ভার ও ভূমিকা", "Account, Server & Role"))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(color = c.surfaceCard, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${currentName.ifBlank { currentUserId }} (${currentRole.uppercase()})",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                        if (currentEmail.isNotBlank()) {
                            Text(currentEmail, style = MaterialTheme.typography.bodySmall, color = c.mute)
                        }
                        Text(
                            text = "Provider: $currentProvider",
                            fontFamily = MonoFont,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.mint
                        )
                    }
                }
                OutlinedTextField(
                    value = uid,
                    onValueChange = { if (it.length <= 30) uid = it },
                    label = { Text(tr(lang, "নাম / আইডি", "User ID")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("role_uid_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "farmer" to tr(lang, "কৃষক", "Farmer"),
                        "expert" to tr(lang, "বিশেষজ্ঞ", "Expert"),
                        "admin" to tr(lang, "অ্যাডমিন", "Admin")
                    ).forEach { (rId, rLabel) ->
                        FilterChip(
                            selected = role == rId,
                            onClick = { role = rId },
                            label = { Text(rLabel) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("role_chip_$rId")
                        )
                    }
                }
                HorizontalDivider(color = c.line)
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(tr(lang, "GrowthTwin সার্ভার URL", "GrowthTwin Server URL")) },
                    placeholder = { Text("https://growth-twin.onrender.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_server_url_input")
                )
                OutlinedButton(
                    onClick = { onTestServerUrl(serverUrl) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_server_ping_button")
                ) {
                    Text(tr(lang, "সংযোগ পরীক্ষা (/auth/config)", "Save & Ping (/auth/config)"))
                }
                if (serverStatusText.isNotBlank()) {
                    Text(
                        text = serverStatusText,
                        fontFamily = MonoFont,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.mint
                    )
                }
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = c.bad)
                    Spacer(Modifier.width(8.dp))
                    Text(tr(lang, "লগ আউট", "Sign Out"), color = c.bad)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(uid.trim().ifBlank { "farmer1" }, role) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("role_save_button")
            ) {
                Text(tr(lang, "সংরক্ষণ করুন", "Apply"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(tr(lang, "বাতিল", "Cancel"))
            }
        }
    )
}
