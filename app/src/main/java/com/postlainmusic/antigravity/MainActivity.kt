package com.postlainmusic.antigravity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.net.URLEncoder
import java.net.URL

private val Ink = Color(0xFF08090C)
private val Panel = Color(0xFF101217)
private val Rail = Color(0xFF0D0F13)
private val Line = Color(0xFF242832)
private val Txt = Color(0xFFE8EAF0)
private val Muted = Color(0xFF858B9A)
private val Violet = Color(0xFF9B8CFF)
private val Cyan = Color(0xFF72D7FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AntigravityApp() }
    }
}

data class FsItem(val name: String, val path: String, val directory: Boolean)

@Composable
private fun AntigravityApp() {
    var tab by remember { mutableStateOf(0) }
    var currentDir by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<FsItem>>(emptyList()) }
    var selected by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var agentInput by remember { mutableStateOf("") }
    var agentOutput by remember { mutableStateOf("Ready. Start the Antigravity bridge in Termux.") }
    var terminalInput by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh(dir: String = currentDir) { scope.launch { files = listFiles(dir); currentDir = dir } }
    fun openFile(path: String) { scope.launch { content = readFile(path); selected = path; tab = 0 } }
    fun saveFile(text: String) { if (selected.isNotBlank()) scope.launch { writeFile(selected, text); content = text } }

    LaunchedEffect(Unit) { refresh("") }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Violet)) {
        Column(Modifier.fillMaxSize().background(Ink).windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopBar { refresh() }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (tab == 0) FileRail(currentDir, files, selected, { path, dir -> if (dir) refresh(path) else openFile(path) }, { refresh(currentDir) })
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (tab) {
                        0 -> Editor(selected, content, ::saveFile)
                        1 -> AgentPanel(agentInput, { agentInput = it }, agentOutput, busy) {
                            if (!busy && agentInput.isNotBlank()) { busy = true; scope.launch { agentOutput = runAgent(agentInput); busy = false } }
                        }
                        2 -> TerminalPanel(terminalInput, { terminalInput = it }, terminalOutput, busy) {
                            if (!busy && terminalInput.isNotBlank()) { busy = true; val cmd = terminalInput; scope.launch { terminalOutput = runTerminal(cmd); terminalInput = ""; busy = false } }
                        }
                        else -> RunPanel(busy) {
                            if (!busy) { busy = true; scope.launch { terminalOutput = runBuild(); tab = 2; busy = false } }
                        }
                    }
                }
            }
            BottomDock(tab) { tab = it }
        }
    }
}

@Composable
private fun TopBar(onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).background(Panel).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(Violet, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text("✦", color = Ink, fontSize = 18.sp) }
        Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text("Antigravity", color = Txt, fontSize = 15.sp); Text("workspace connected", color = Cyan, fontSize = 10.sp) }
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = Muted) }
    }
}

@Composable
private fun FileRail(dir: String, files: List<FsItem>, selected: String, onOpen: (String, Boolean) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.width(190.dp).fillMaxHeight().background(Rail).padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (dir.isBlank()) "PROJECT" else dir, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = Muted, modifier = Modifier.size(15.dp)) }
        }
        if (dir.isNotBlank()) Row(Modifier.fillMaxWidth().clickable { onOpen(dir.substringBeforeLast('/', ""), true) }.padding(12.dp)) { Text("‹  Parent", color = Muted, fontSize = 11.sp) }
        LazyColumn {
            items(files, key = { it.path }) { item ->
                val active = item.path == selected
                Row(Modifier.fillMaxWidth().height(36.dp).background(if (active) Color(0xFF1B1E27) else Color.Transparent).clickable { onOpen(item.path, item.directory) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (item.directory) Icons.Default.Folder else Icons.Default.Description, null, tint = if (active) Violet else Muted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(item.name, color = if (active) Txt else Muted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun Editor(file: String, text: String, onSave: (String) -> Unit) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    Column(Modifier.fillMaxSize().background(Ink)) {
        Row(Modifier.fillMaxWidth().height(40.dp).background(Panel).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (file.isBlank()) "No file selected" else file, color = Txt, fontSize = 12.sp, maxLines = 1); Spacer(Modifier.weight(1f)); Text("Ctrl+S", color = Muted, fontSize = 9.sp)
            IconButton(onClick = { webViewRef?.evaluateJavascript("window.saveEditorContent();", null) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Save, null, tint = Cyan, modifier = Modifier.size(18.dp)) }
        }
        HorizontalDivider(color = Line)
        AndroidView<WebView>(
            factory = { context: Context ->
                WebView(context).apply {
                    webViewRef = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(object {
                        @JavascriptInterface fun save(value: String) { onSave(value) }
                    }, "Native")
                    loadUrl("file:///android_asset/editor.html")
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { web: WebView -> web.evaluateJavascript("window.setEditorContent(${JSONObject.quote(text)});", null) }
        )
    }
}

@Composable
private fun AgentPanel(input: String, onInput: (String) -> Unit, output: String, busy: Boolean, run: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Agent", color = Txt, fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Text(if (busy) "RUNNING" else "READY", color = if (busy) Cyan else Violet, fontSize = 9.sp) }
        Text("Antigravity CLI · real project agent", color = Muted, fontSize = 11.sp); Spacer(Modifier.height(14.dp))
        Surface(Modifier.fillMaxWidth().weight(1f), color = Panel, shape = RoundedCornerShape(14.dp)) { LazyColumn(Modifier.padding(16.dp)) { item { Text(output, color = Txt, fontSize = 12.sp) } } }
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ask Antigravity to edit, fix or build…") }, minLines = 3)
        Spacer(Modifier.height(8.dp)); Button(onClick = run, enabled = !busy && input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(if (busy) "Running…" else "Run agent") }
    }
}

@Composable
private fun TerminalPanel(input: String, onInput: (String) -> Unit, output: String, busy: Boolean, run: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF050609)).padding(14.dp)) {
        Text("Terminal", color = Txt, fontSize = 18.sp); Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth().weight(1f), color = Color(0xFF080A0E)) { LazyColumn(Modifier.padding(12.dp)) { item { Text(output.ifBlank { "Connected terminal\n" }, color = Color(0xFFB7BECF), fontSize = 12.sp) } } }
        Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("$", color = Violet, fontSize = 13.sp); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.weight(1f), singleLine = true, enabled = !busy); IconButton(onClick = run, enabled = !busy && input.isNotBlank()) { Icon(Icons.Default.Send, null, tint = Cyan) } }
    }
}

@Composable
private fun RunPanel(busy: Boolean, run: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp)) { Text("Build & Run", color = Txt, fontSize = 22.sp); Text("Build the debug APK from the active workspace.", color = Muted, fontSize = 12.sp); Spacer(Modifier.height(22.dp)); Button(onClick = run, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Build, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Building…" else "Build debug APK") } }
}

@Composable
private fun BottomDock(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(Icons.Default.Folder to "Files", Icons.Default.AutoAwesome to "Agent", Icons.Default.Terminal to "Terminal", Icons.Default.PlayArrow to "Run")
    Row(Modifier.fillMaxWidth().height(62.dp).background(Panel), horizontalArrangement = Arrangement.SpaceEvenly) { items.forEachIndexed { i, item -> Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(item.first, null, tint = if (selected == i) Violet else Muted, modifier = Modifier.size(20.dp)); Text(item.second, color = if (selected == i) Txt else Muted, fontSize = 9.sp) } } }
}

private suspend fun http(path: String, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
    val c = (URL("http://127.0.0.1:8765$path").openConnection() as HttpURLConnection).apply { requestMethod = method; connectTimeout = 4000; readTimeout = 900000; if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") } }
    if (body != null) c.outputStream.use { it.write(body.toString().toByteArray()) }
    val raw = (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().readText(); if (c.responseCode !in 200..299) throw Exception("HTTP ${c.responseCode}: $raw"); JSONObject(raw)
}

private suspend fun listFiles(path: String): List<FsItem> = try { val q = URLEncoder.encode(path, "UTF-8"); val a = http("/files?path=$q").getJSONArray("items"); List(a.length()) { val o = a.getJSONObject(it); FsItem(o.getString("name"), o.getString("path"), o.getBoolean("directory")) } } catch (_: Exception) { emptyList() }
private suspend fun readFile(path: String): String = try { val q = URLEncoder.encode(path, "UTF-8"); http("/read?path=$q").getString("content") } catch (e: Exception) { "// ${e.message}" }
private suspend fun writeFile(path: String, content: String) { try { http("/write", "POST", JSONObject().put("path", path).put("content", content)) } catch (_: Exception) {} }
private suspend fun runAgent(prompt: String): String = try { val o = http("/run", "POST", JSONObject().put("prompt", prompt)); o.optString("stdout").ifBlank { o.optString("stderr") } } catch (e: Exception) { "Bridge unavailable: ${e.message}\nStart: python bridge/agy_bridge.py" }
private suspend fun runTerminal(command: String): String = try { val o = http("/terminal", "POST", JSONObject().put("command", command)); (o.optString("stdout") + if (o.optString("stderr").isNotBlank()) "\n${o.optString("stderr")}" else "").ifBlank { "exit ${o.optInt("exitCode")}" } } catch (e: Exception) { "Terminal error: ${e.message}" }
private suspend fun runBuild(): String = runTerminal("./gradlew assembleDebug")
