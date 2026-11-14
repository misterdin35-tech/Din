package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.AgentStatus
import com.example.domain.model.AgentStepState
import com.example.domain.model.AnalystMessage
import com.example.domain.model.AnalystStructuredResponse
import com.example.domain.model.DataTable
import com.example.domain.voice.VoiceAnalystController
import com.example.domain.voice.VoiceLocaleOption
import com.example.ui.components.DataChartCard
import com.example.ui.components.EmptyDatasetPromptCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColorFor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnalystWorkspaceScreen(
    activeTable: DataTable?,
    messages: List<AnalystMessage>,
    liveAgentSteps: List<AgentStepState>,
    isAgentRunning: Boolean,
    onAskQuestion: (String, onSpeakReady: ((String) -> Unit)?) -> Unit,
    onOpenInSqlStudio: (String) -> Unit,
    onOpenInPythonStudio: (String) -> Unit,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    val context = LocalContext.current
    val voiceController = remember(context) { VoiceAnalystController(context) }
    val voiceState by voiceController.state.collectAsState()

    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    var inputText by remember { mutableStateOf("") }
    var showLocaleMenu by remember { mutableStateOf(false) }

    val sendQuestion: (String) -> Unit = { q ->
        onAskQuestion(q) { summary ->
            if (voiceState.autoSpeakResponses) {
                voiceController.speak(summary)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.startListening { transcript ->
                inputText = transcript
                sendQuestion(transcript)
            }
        }
    }

    val starterPrompts = remember {
        listOf(
            "Why did my company sales decrease this month?",
            "Which region and product drive the highest profit?",
            "What anomalies or data quality issues exist?",
            "Forecast revenue for the next 3 months with risks",
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Voice & Multi-Agent Top Bar
        Surface(
            color = MidnightSurface,
            border = BorderStroke(1.dp, SubtleDivider),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = CyberCyan)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("7-Agent AI Analyst", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = activeTable?.name ?: "No dataset active",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { showLocaleMenu = true }) {
                            Icon(Icons.Rounded.Language, contentDescription = "Voice Language", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showLocaleMenu,
                            onDismissRequest = { showLocaleMenu = false },
                        ) {
                            VoiceLocaleOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        voiceController.setLocale(option)
                                        showLocaleMenu = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { voiceController.toggleAutoSpeak() }) {
                        Icon(
                            imageVector = if (voiceState.autoSpeakResponses) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = "Toggle AI Voice Playback",
                            tint = if (voiceState.autoSpeakResponses) CyberCyan else TextSecondary,
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (activeTable == null) {
                item {
                    EmptyDatasetPromptCard(
                        onUploadClick = onUploadClick,
                        onLoadSampleClick = onLoadSampleClick,
                    )
                }
            } else if (messages.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Autonomous Multi-Agent Analyst", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Ask any business question in natural language or via Voice AI. DataPilot plans SQL & Python queries, executes them locally on your dataset, verifies numbers with Quality Control, and cites only calculated evidence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(starterPrompts) { prompt ->
                                Surface(
                                    onClick = { sendQuestion(prompt) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = SlateElevated,
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                            color = ElectricBlue.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(0.86f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(msg.timestampLabel, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (msg.agentTrace.isNotEmpty()) {
                            AgentTraceCard(steps = msg.agentTrace, title = "Executed 7-Agent Pipeline")
                        }
                        if (msg.structured != null) {
                            StructuredAnalystResponseCard(
                                response = msg.structured,
                                onSpeak = { voiceController.speak(msg.structured.executiveSummary) },
                                onStopSpeak = { voiceController.stopSpeaking() },
                                isSpeaking = voiceState.isSpeaking,
                                onFollowUpClick = { sendQuestion(it) },
                                onOpenSql = { onOpenInSqlStudio(msg.structured.sqlQuery) },
                                onOpenPython = { onOpenInPythonStudio(msg.structured.pythonScript) },
                            )
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = CoralError)
                            }
                        }
                    }
                }
            }

            if (isAgentRunning && liveAgentSteps.isNotEmpty()) {
                item {
                    AgentTraceCard(steps = liveAgentSteps, title = "Live Autonomous Execution")
                }
            }
        }

        // Voice Listening Banner
        AnimatedVisibility(visible = voiceState.isListening || voiceState.error != null) {
            Surface(
                color = if (voiceState.error != null) CoralError.copy(alpha = 0.16f) else CyberCyan.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (voiceState.error != null) CoralError else CyberCyan,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = voiceState.error ?: voiceState.partialTranscript.ifBlank { "Listening (${voiceState.selectedLocale.label})..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                        )
                    }
                    if (voiceState.isListening) {
                        IconButton(onClick = { voiceController.stopListening() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Stop listening", tint = CoralError)
                        }
                    }
                }
            }
        }

        // Input Composer Bar
        Surface(
            color = MidnightSurface,
            border = BorderStroke(1.dp, SubtleDivider),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (voiceState.isListening) {
                            voiceController.stopListening()
                        } else {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                voiceController.startListening { transcript ->
                                    inputText = transcript
                                    sendQuestion(transcript)
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (voiceState.isListening) CoralError.copy(alpha = 0.25f) else SlateElevated)
                        .testTag("voice_mic_button"),
                ) {
                    Icon(
                        imageVector = if (voiceState.isListening) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = if (voiceState.isListening) "Stop voice input" else "Start voice input",
                        tint = if (voiceState.isListening) CoralError else CyberCyan,
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask DataPilot AI about your data...", color = TextSecondary) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SubtleDivider,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SlateCard,
                        unfocusedContainerColor = SlateCard,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("analyst_question_input"),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val q = inputText.trim()
                        if (q.isNotEmpty() && !isAgentRunning) {
                            inputText = ""
                            sendQuestion(q)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAgentRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isAgentRunning) ElectricBlue else SlateElevated)
                        .testTag("analyst_send_button"),
                ) {
                    if (isAgentRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyberCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send question", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentTraceCard(steps: List<AgentStepState>, title: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = CyberCyan)
        Spacer(Modifier.height(8.dp))
        steps.forEach { step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
            ) {
                Text(
                    text = "A${step.agentNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuralViolet,
                    modifier = Modifier.width(26.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(step.agentName, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    if (step.outputSummary.isNotBlank()) {
                        Text(step.outputSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                if (step.status == AgentStatus.RUNNING) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyberCyan)
                } else {
                    StatusPill(label = step.status.name, color = statusColorFor(step.status.name))
                }
            }
        }
    }
}

@Composable
private fun StructuredAnalystResponseCard(
    response: AnalystStructuredResponse,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    isSpeaking: Boolean,
    onFollowUpClick: (String) -> Unit,
    onOpenSql: () -> Unit,
    onOpenPython: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(label = "VERIFIED EXECUTIVE SUMMARY", color = EmeraldSuccess)
                IconButton(onClick = { if (isSpeaking) onStopSpeak() else onSpeak() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Read summary aloud",
                        tint = CyberCyan,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(response.executiveSummary, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)

            if (!response.clarificationQuestion.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarning.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = "Clarification Needed: ${response.clarificationQuestion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            if (response.keyFindings.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Key Findings", style = MaterialTheme.typography.titleSmall, color = CyberCyan)
                response.keyFindings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = TextPrimary) }
            }

            if (response.supportingNumbers.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Supporting Numbers (Computed)", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                response.supportingNumbers.forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMonoFontFamily, color = TextSecondary)
                }
            }

            if (response.possibleCauses.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Possible Causes", style = MaterialTheme.typography.titleSmall, color = NeuralViolet)
                response.possibleCauses.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }

            if (response.recommendations.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Business Recommendations", style = MaterialTheme.typography.titleSmall, color = EmeraldSuccess)
                response.recommendations.forEach { Text("→ $it", style = MaterialTheme.typography.bodySmall, color = TextPrimary) }
            }

            if (response.limitations.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Limitations & Caveats", style = MaterialTheme.typography.titleSmall, color = AmberWarning)
                response.limitations.forEach { Text("! $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (response.sqlQuery.isNotBlank()) {
                    OutlinedButton(
                        onClick = onOpenSql,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open SQL", color = ElectricBlue)
                    }
                }
                if (response.pythonScript.isNotBlank()) {
                    OutlinedButton(
                        onClick = onOpenPython,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeuralViolet),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.Terminal, contentDescription = null, tint = NeuralViolet, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Python", color = NeuralViolet)
                    }
                }
            }
        }

        if (response.chartSpec != null) {
            DataChartCard(chartSpec = response.chartSpec)
        }

        if (response.followUpQuestions.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(response.followUpQuestions) { fq ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateElevated,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onFollowUpClick(fq) },
                    ) {
                        Text(
                            text = fq,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
