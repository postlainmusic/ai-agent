package com.postlainmusic.antigravity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF08090C)
private val Panel = Color(0xFF101217)
private val Text = Color(0xFFE8EAF0)
private val Muted = Color(0xFF858B9A)
private val Violet = Color(0xFF9B8CFF)
private val Cyan = Color(0xFF72D7FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AntigravityApp() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AntigravityApp() {
    var tab by remember { mutableStateOf(0) }
    var selectedFile by remember { mutableStateOf("MainActivity.kt") }
    var agentText by remember { mutableStateOf("") }
    var agentOutput by remember { mutableStateOf("Ready. Antigravity engine is waiting.") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Ink)) {
            TopBar()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                FileRail(selectedFile) { selectedFile = it }
                when (tab) {
                    1 -> AgentPanel(agentText, { agentText = it }, agentOutput, running) {
                        if (agentText.isBlank() || running) return@AgentPanel
                        running = true
                        agentOutput = "Running Antigravity…"
                        scope.launch {
                            agentOutput = runAgent(agentText)
                            running = false
                        }
                    }
                    2 -> TerminalPanel()
                    else -> Editor(selectedFile, Modifier.weight(1f))
                }
            }
            BottomDock(tab) { tab = it }
        }
    }
}

@Composable
private fun TopBar() {
    Row(Modifier.fillMaxWidth().height(58.dp).background(Panel).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(Violet, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text("✦", color = Ink, fontSize = 18.sp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Antigravity", color = Text, fontSize = 15.sp)
            Text("mobile workspace", color = Muted, fontSize = 10.sp)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Search, null, tint = Muted) }
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Muted) }
    }
}

@Composable
private fun FileRail(selected: String, onSelect: (String) -> Unit) {
    val files = listOf("app/", "src/", "MainActivity.kt", "Editor.kt", "build.gradle.kts", "README.md")
    Column(Modifier.width(178.dp).fillMaxHeight().background(Color(0xFF0D0F13)).padding(vertical = 12.dp)) {
        Text("PROJECT", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
        files.forEach { file ->
            val active = file == selected
            Row(Modifier.fillMaxWidth().height(36.dp).background(if (active) Color(0xFF1B1E27) else Color.Transparent).clickable { onSelect(file) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (file.endsWith("/")) Icons.Default.Folder else Icons.Default.Description, null, tint = if (active) Violet else Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp)); Text(file, color = if (active) Text else Muted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Editor(file: String, modifier: Modifier) {
    Column(modifier.fillMaxHeight().background(Ink)) {
        Row(Modifier.fillMaxWidth().height(40.dp).background(Panel).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(file, color = Text, fontSize = 12.sp); Spacer(Modifier.width(7.dp)); Text("●", color = Violet, fontSize = 8.sp); Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.PlayArrow, null, tint = Cyan, modifier = Modifier.size(18.dp)) }
        }
        HorizontalDivider(color = Color(0xFF242832)); EditorWebView()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EditorWebView() {
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.rgb(8, 9, 12)); settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.allowFileAccess = true
            webViewClient = WebViewClient(); webChromeClient = WebChromeClient(); loadUrl("file:///android_asset/editor.html")
        }
    })
}

@Composable
private fun AgentPanel(input: String, onInput: (String) -> Unit, output: String, running: Boolean, run: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Agent", color = Text, fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Text(if (running) "RUNNING" else "READY", color = if (running) Cyan else Violet, fontSize = 9.sp) }
        Text("Antigravity CLI", color = Violet, fontSize = 11.sp); Spacer(Modifier.height(18.dp))
        Surface(Modifier.fillMaxWidth().weight(1f), color = Panel, shape = RoundedCornerShape(14.dp)) { Text(output, color = Text, fontSize = 12.sp, modifier = Modifier.padding(16.dp)) }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ask Antigravity to change the project…") }, minLines = 3)
        Spacer(Modifier.height(10.dp)); Button(onClick = run, enabled = !running && input.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (running) "Running…" else "Run agent")
        }
    }
}

@Composable
private fun TerminalPanel() {
    Column(Modifier.fillMaxSize().background(Color(0xFF050609)).padding(16.dp)) { Text("Terminal", color = Text, fontSize = 18.sp); Spacer(Modifier.height(14.dp)); Text("$ agy --version\n\n1.2.0\n\n$", color = Color(0xFFB7BECF), fontSize = 12.sp) }
}

@Composable
private fun BottomDock(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(Icons.Default.Folder to "Files", Icons.Default.AutoAwesome to "Agent", Icons.Default.Terminal to "Terminal", Icons.Default.PlayArrow to "Run")
    Row(Modifier.fillMaxWidth().height(62.dp).background(Panel).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { index, pair ->
            val active = selected == index
            Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(pair.first, null, tint = if (active) Violet else Muted, modifier = Modifier.size(20.dp)); Text(pair.second, color = if (active) Text else Muted, fontSize = 9.sp)
            }
        }
    }
}

private suspend fun runAgent(prompt: String): String = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("http://127.0.0.1:8765/run").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 5000; readTimeout = 900000; setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(JSONObject().put("prompt", prompt).toString().toByteArray()) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().readText()
        if (conn.responseCode !in 200..299) return@withContext "Bridge error (${conn.responseCode})\n$raw"
        val obj = JSONObject(raw)
        obj.optString("stdout").ifBlank { obj.optString("stderr", raw) }
    } catch (e: Exception) { "Bridge unavailable: ${e.message}\n\nStart bridge in Termux: python bridge/agy_bridge.py" }
}
