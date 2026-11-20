package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GTCard
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.GTTheme
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ExpertAdvisorQueueItem
import com.example.ui.viewmodel.ExpertPhotoItem

@Composable
fun ExpertScreen(
    lang: AppLang,
    userId: String,
    userRole: String,
    advisorQueue: List<ExpertAdvisorQueueItem>,
    photoQueue: List<ExpertPhotoItem>,
    onSwitchRole: (String, String) -> Unit,
    onResolveAdvisor: (String, String, String) -> Unit,
    onResolvePhoto: (String, String, String, String, String) -> Unit
) {
    val c = GTTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            kicker = "EXPERT & ADMIN CONSOLE",
            title = tr(lang, "কৃষি বিশেষজ্ঞ ও অ্যাডমিন রিভিউ", "Expert & Admin Review Queue"),
            subtitle = tr(
                lang,
                "কম-নিশ্চয়তার পরামর্শ এবং অস্পষ্ট পাতার ছবি যাচাই করুন।",
                "Verify low-confidence advisor recommendations and uncertain leaf disease photos."
            )
        )

        if (userRole !in setOf("expert", "admin")) {
            NoticeBanner(
                title = tr(lang, "বিশেষজ্ঞ বা অ্যাডমিন ভূমিকা প্রয়োজন", "Expert / Admin Role Required"),
                body = tr(
                    lang,
                    "আপনি এখন কৃষক (farmer) মোডে আছেন। রিভিউ কিউ পরিচালনা করতে ভূমিকা পরিবর্তন করুন।",
                    "You are currently in 'farmer' role. Switch to Expert or Admin below to inspect review queues."
                ),
                tone = "warn"
            )
            Button(
                onClick = { onSwitchRole(userId, "expert") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("expert_switch_role_button")
            ) {
                Text(tr(lang, "বিশেষজ্ঞ (Expert) মোডে যান", "Switch to Expert Role"))
            }
            return
        }

        // Advisor Queue
        Text(
            tr(lang, "পরামর্শ রিভিউ কিউ (${advisorQueue.size})", "Advisor Queue (${advisorQueue.size})"),
            style = MaterialTheme.typography.titleLarge,
            color = c.ink
        )
        if (advisorQueue.isEmpty()) {
            GTCard {
                Text(
                    tr(lang, "কোনো পরামর্শ রিভিউ বাকি নেই। সেকশন ১ থেকে যেকোনো পরামর্শ পাঠাতে পারেন।", "No advisor reviews pending. Submit one from Crop Advisor."),
                    color = c.mute
                )
            }
        } else {
            advisorQueue.forEach { item ->
                var note by remember(item.sessionId) { mutableStateOf("") }
                GTCard(accentBorder = c.warn.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr(lang, item.headlineBn, item.headlineEn), style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
                        StatusBadge(text = "${(item.confidence * 100).toInt()}%", tone = "caution")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.prompt, style = MaterialTheme.typography.bodyMedium, color = c.mute)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text(tr(lang, "বিশেষজ্ঞের মন্তব্য", "Expert note")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { onResolveAdvisor(item.sessionId, "agree", note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(tr(lang, "একমত", "Agree"))
                        }
                        OutlinedButton(onClick = { onResolveAdvisor(item.sessionId, "adjust", note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(tr(lang, "সংশোধন", "Adjust"))
                        }
                        OutlinedButton(onClick = { onResolveAdvisor(item.sessionId, "disagree", note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(tr(lang, "ভিন্নমত", "Disagree"))
                        }
                    }
                }
            }
        }

        // Photo Review Queue
        Text(
            tr(lang, "পাতার ছবি রিভিউ কিউ (${photoQueue.size})", "Uncertain Photo Queue (${photoQueue.size})"),
            style = MaterialTheme.typography.titleLarge,
            color = c.ink
        )
        if (photoQueue.isEmpty()) {
            GTCard {
                Text(
                    tr(lang, "কোনো পাতার ছবি রিভিউ বাকি নেই।", "No uncertain leaf photos awaiting review."),
                    color = c.mute
                )
            }
        } else {
            photoQueue.forEach { item ->
                var label by remember(item.review.id) { mutableStateOf(item.review.labelBn ?: item.review.label ?: "") }
                var note by remember(item.review.id) { mutableStateOf("") }
                GTCard(accentBorder = c.lime.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.plotName} (${item.cropId}) • Day ${item.review.day}", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        StatusBadge(text = item.review.reason, tone = "warn")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = label, onValueChange = { label = it },
                        label = { Text(tr(lang, "রোগের নাম", "Confirmed diagnosis")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text(tr(lang, "পরামর্শ নোট", "Advice note")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { onResolvePhoto(item.plotId, item.review.id, "disease", label, note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(tr(lang, "রোগ নিশ্চিত", "Disease"))
                        }
                        OutlinedButton(onClick = { onResolvePhoto(item.plotId, item.review.id, "healthy", label, note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(tr(lang, "সুস্থ", "Healthy"))
                        }
                        OutlinedButton(onClick = { onResolvePhoto(item.plotId, item.review.id, "unsure", label, note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(tr(lang, "অনিশ্চিত", "Unsure"))
                        }
                    }
                }
            }
        }

        if (userRole == "admin") {
            GTCard {
                Text("ADMIN ACCOUNT SUMMARY", style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$userId ($userRole)", color = c.ink)
                    StatusBadge(text = "ACTIVE", tone = "go")
                }
            }
        }
    }
}
