package com.postlainmusic.antigravity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF08090C)
private val Panel = Color(0xFF101217)
private val Panel2 = Color(0xFF151820)
private val Line = Color(0xFF242832)
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

@Composable
private fun AntigravityApp() {
    var tab by remember { mutableStateOf(0) }
    var selectedFile by remember { mutableStateOf("MainActivity.kt") }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Ink)) {
            TopBar()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                FileRail(
                    selectedFile = selectedFile,
                    onFileSelected = { selectedFile = it }
                )
                Editor(
                    file = selectedFile,
                    modifier = Modifier.weight(1f)
                )
            }
            BottomDock(selected = tab, onSelect = { tab = it })
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        Modifier.fillMaxWidth().height(58.dp).background(Panel).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).background(Violet, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) { Text("✦", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Antigravity", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("AI workspace", color = Muted, fontSize = 10.sp)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Search, null, tint = Muted) }
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Muted) }
    }
}

@Composable
private fun FileRail(selectedFile: String, onFileSelected: (String) -> Unit) {
    val files = listOf("app/", "src/", "MainActivity.kt", "Editor.kt", "build.gradle.kts", "README.md")
    Column(
        Modifier.width(178.dp).fillMaxHeight().background(Color(0xFF0D0F13)).padding(vertical = 12.dp)
    ) {
        Text("PROJECT", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
        files.forEach { file ->
            val active = file == selectedFile
            Row(
                Modifier.fillMaxWidth().height(36.dp)
                    .background(if (active) Color(0xFF1B1E27) else Color.Transparent)
                    .clickable { onFileSelected(file) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (file.endsWith("/")) Icons.Default.Folder else Icons.Default.Description,
                    null,
                    tint = if (active) Violet else Muted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(file, color = if (active) Text else Muted, fontSize = 12.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(Color(0xFF68E39B), RoundedCornerShape(50)))
            Spacer(Modifier.width(7.dp))
            Text("Ready", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun Editor(file: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().background(Ink)) {
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(Panel).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(file, color = Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text("●", color = Violet, fontSize = 8.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.PlayArrow, null, tint = Cyan, modifier = Modifier.size(18.dp)) }
        }
        HorizontalDivider(color = Line, thickness = 1.dp)
        val code = listOf(
            "01  package com.postlainmusic.antigravity",
            "02",
            "03  class MainActivity : ComponentActivity() {",
            "04      override fun onCreate(savedInstanceState: Bundle?) {",
            "05          super.onCreate(savedInstanceState)",
            "06          setContent {",
            "07              AntigravityApp()",
            "08          }",
            "09      }",
            "10  }",
            "11",
            "12  // Ask Antigravity to change this file",
            "13  // and watch the workspace update live."
        )
        LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(code) { line ->
                Text(
                    line,
                    color = if (line.contains("Antigravity")) Violet else Text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomDock(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(Icons.Default.Folder to "Files", Icons.Default.AutoAwesome to "Agent", Icons.Default.Terminal to "Terminal", Icons.Default.PlayArrow to "Run")
    Row(
        Modifier.fillMaxWidth().height(62.dp).background(Panel).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, pair ->
            val active = selected == index
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(pair.first, null, tint = if (active) Violet else Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(3.dp))
                Text(pair.second, color = if (active) Text else Muted, fontSize = 9.sp)
            }
        }
    }
}
