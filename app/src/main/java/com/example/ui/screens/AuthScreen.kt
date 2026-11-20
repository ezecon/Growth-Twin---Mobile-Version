package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.GrowthTwinApiClient
import com.example.ui.components.GTCard
import com.example.ui.components.NoticeBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    lang: AppLang,
    darkTheme: Boolean,
    serverBaseUrl: String,
    serverStatusText: String,
    loading: Boolean,
    errorText: String?,
    infoText: String?,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, mobile: String, email: String, password: String, role: String) -> Unit,
    onEnterDevMode: (userId: String, role: String) -> Unit,
    onTestServerUrl: (String) -> Unit
) {
    val c = GTTheme.colors
    var activeTab by remember { mutableStateOf("login") } // login | register | dev
    var showServerBox by remember { mutableStateOf(false) }

    // Login state (#fL)
    var loginEmail by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }
    var showLoginPass by remember { mutableStateOf(false) }

    // Register state (#fR)
    var regName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPass by remember { mutableStateOf("") }
    var showRegPass by remember { mutableStateOf(false) }
    var regRole by remember { mutableStateOf("farmer") }

    // Dev mode state (#fD)
    var devUser by remember { mutableStateOf("farmer1") }
    var devRole by remember { mutableStateOf("farmer") }

    // Server URL state (non-secret endpoint)
    var serverUrlInput by remember(serverBaseUrl) { mutableStateOf(serverBaseUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (c.isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2AA3F20A), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.14f),
                        radius = size.maxDimension * 0.52f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x223DE0A0), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.84f),
                        radius = size.maxDimension * 0.5f
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top #ctl row: EN/বাং + Day/Night matching index.html
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleLang,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("auth_lang_toggle")
                ) {
                    Text(
                        text = if (lang == AppLang.BN) "EN" else "বাং",
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.padding(4.dp))
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("auth_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = tr(lang, "দিন/রাত থিম", "Toggle theme"),
                        tint = c.warn
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Centered #auth .card matching GitHub index.html
            GTCard(
                accentBorder = c.lineBright,
                contentPadding = 20,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Growth")
                        withStyle(SpanStyle(color = c.lime)) { append("Twin") }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tr(lang, "চালিয়ে যেতে লগইন করুন।", "Sign in to continue."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.mute
                )
                Spacer(Modifier.height(12.dp))

                // Tabs (#tL, #tR, #fD)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeTab == "login",
                        onClick = { activeTab = "login" },
                        label = { Text(tr(lang, "লগইন", "Login")) },
                        modifier = Modifier.testTag("auth_tab_login")
                    )
                    FilterChip(
                        selected = activeTab == "register",
                        onClick = { activeTab = "register" },
                        label = { Text(tr(lang, "নতুন একাউন্ট", "Create Account")) },
                        modifier = Modifier.testTag("auth_tab_register")
                    )
                    FilterChip(
                        selected = activeTab == "dev",
                        onClick = { activeTab = "dev" },
                        label = { Text(tr(lang, "ডেভ মোড", "Dev Mode")) },
                        modifier = Modifier.testTag("auth_tab_dev")
                    )
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    NoticeBanner(
                        title = tr(lang, "ত্রুটি", "Authentication Notice"),
                        body = errorText,
                        tone = "bad"
                    )
                }
                if (!infoText.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    NoticeBanner(
                        title = tr(lang, "অবস্থা", "Status"),
                        body = infoText,
                        tone = "ok"
                    )
                }

                Spacer(Modifier.height(12.dp))

                when (activeTab) {
                    "login" -> {
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text(tr(lang, "ইমেইল", "Email")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_login_email")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = loginPass,
                            onValueChange = { loginPass = it },
                            label = { Text(tr(lang, "পাসওয়ার্ড", "Password")) },
                            singleLine = true,
                            visualTransformation = if (showLoginPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showLoginPass = !showLoginPass }) {
                                    Icon(
                                        imageVector = if (showLoginPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = tr(lang, "পাসওয়ার্ড দেখুন", "Toggle password visibility")
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_login_password")
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onLogin(loginEmail, loginPass) },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_login_submit")
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = c.onLime, strokeWidth = 2.dp)
                            } else {
                                Text(tr(lang, "লগইন", "Sign In"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "register" -> {
                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            label = { Text(tr(lang, "নাম", "Full Name")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_reg_name")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regMobile,
                            onValueChange = { regMobile = it },
                            label = { Text(tr(lang, "মোবাইল (ঐচ্ছিক)", "Mobile (optional)")) },
                            placeholder = { Text("01712345678") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_reg_mobile")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text(tr(lang, "ইমেইল", "Email")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_reg_email")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regPass,
                            onValueChange = { regPass = it },
                            label = { Text(tr(lang, "পাসওয়ার্ড (৮+ অক্ষর, অক্ষর ও সংখ্যা)", "Password (8+ chars, letters & numbers)")) },
                            singleLine = true,
                            visualTransformation = if (showRegPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showRegPass = !showRegPass }) {
                                    Icon(
                                        imageVector = if (showRegPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = tr(lang, "পাসওয়ার্ড দেখুন", "Toggle password visibility")
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_reg_password")
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(tr(lang, "আমি", "I am a"), style = MaterialTheme.typography.labelLarge, color = c.mute)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = regRole == "farmer",
                                onClick = { regRole = "farmer" },
                                label = { Text(tr(lang, "কৃষক", "Farmer")) }
                            )
                            FilterChip(
                                selected = regRole == "expert",
                                onClick = { regRole = "expert" },
                                label = { Text(tr(lang, "কৃষি বিশেষজ্ঞ (অ্যাডমিন অনুমোদন)", "Expert (Admin approval)")) }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onRegister(regName, regMobile, regEmail, regPass, regRole) },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_reg_submit")
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = c.onLime, strokeWidth = 2.dp)
                            } else {
                                Text(tr(lang, "একাউন্ট খুলুন", "Create Account"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    else -> {
                        // Dev Mode (#fD matching index.html)
                        Text(
                            text = tr(
                                lang,
                                "ডেভ মোড: সার্ভারে লগইন বন্ধ (AUTH_REQUIRED=false)। নাম ও ভূমিকা বেছে নিন। প্রোডাকশনে এটি দেখাবে না।",
                                "Dev mode (AUTH_REQUIRED=false): choose a local User ID and role to enter immediately."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.mute
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = devUser,
                            onValueChange = { if (it.length <= 30) devUser = it },
                            label = { Text(tr(lang, "নাম / আইডি", "Name / User ID")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_dev_uid")
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(tr(lang, "ভূমিকা", "Role"), style = MaterialTheme.typography.labelLarge, color = c.mute)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "farmer" to tr(lang, "কৃষক", "Farmer"),
                                "expert" to tr(lang, "বিশেষজ্ঞ", "Expert"),
                                "admin" to tr(lang, "অ্যাডমিন", "Admin")
                            ).forEach { (rId, rLabel) ->
                                FilterChip(
                                    selected = devRole == rId,
                                    onClick = { devRole = rId },
                                    label = { Text(rLabel) },
                                    modifier = Modifier.testTag("auth_dev_role_$rId")
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onEnterDevMode(devUser.trim().ifBlank { "farmer1" }, devRole) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_dev_submit")
                        ) {
                            Text(tr(lang, "ঢুকুন", "Enter App"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = c.line)
                Spacer(Modifier.height(6.dp))

                // Real Backend / API Connection Accordion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showServerBox = !showServerBox },
                        modifier = Modifier.testTag("auth_server_toggle")
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text(tr(lang, "সার্ভার ও লাইভ API সংযোগ", "Real Backend & Live API"))
                        Icon(if (showServerBox) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    StatusBadge(
                        text = if (serverBaseUrl.isNotBlank()) "API SET" else if (GrowthTwinApiClient.hasSupabaseConfig()) "SUPABASE" else "HYBRID",
                        tone = if (serverBaseUrl.isNotBlank() || GrowthTwinApiClient.hasSupabaseConfig()) "go" else "info"
                    )
                }

                AnimatedVisibility(visible = showServerBox) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr(
                                lang,
                                "আপনার GrowthTwin FastAPI সার্ভারের ঠিকানা দিন (যেমন http://10.0.2.2:8000 বা আপনার ডিপ্লয় করা HTTPS লিংক)। ফাঁকা রাখলে লোকাল + লাইভ ওয়েদার API চলবে।",
                                "Optional: enter your deployed GrowthTwin FastAPI server URL (e.g. https://... or http://10.0.2.2:8000). Leave blank for local engine + live Open-Meteo weather."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.mute
                        )
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text(tr(lang, "GrowthTwin সার্ভার URL", "GrowthTwin Server Base URL")) },
                            placeholder = { Text("https://growth-twin.onrender.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_server_url_input")
                        )
                        OutlinedButton(
                            onClick = { onTestServerUrl(serverUrlInput) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_server_test_btn")
                        ) {
                            Text(tr(lang, "সংরক্ষণ ও পরীক্ষা (/auth/config)", "Save & Ping (/auth/config)"))
                        }
                        if (serverStatusText.isNotBlank()) {
                            Text(
                                text = serverStatusText,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                                color = c.mint
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(
                                text = "Weather API: Open-Meteo",
                                tone = "go"
                            )
                            StatusBadge(
                                text = if (GrowthTwinApiClient.hasSupabaseConfig()) "Supabase: Ready" else "Supabase: Via Secrets",
                                tone = if (GrowthTwinApiClient.hasSupabaseConfig()) "go" else "info"
                            )
                            StatusBadge(
                                text = if (GrowthTwinApiClient.hasGroqConfig()) "Groq LLM: Ready" else "Groq LLM: Via Secrets",
                                tone = if (GrowthTwinApiClient.hasGroqConfig()) "go" else "info"
                            )
                        }
                    }
                }
            }
        }
    }
}
