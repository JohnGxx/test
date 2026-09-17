package com.john.youlan

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.john.youlan.data.YouLanPreferences
import com.john.youlan.model.AppEntry
import com.john.youlan.ui.YouLanTheme
import com.john.youlan.util.InstalledApps
import com.john.youlan.util.SystemState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YouLanTheme { LauncherScreen(resumeTick) } }
    }
    override fun onResume() { super.onResume(); resumeTick++ }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LauncherScreen(resumeTick: Int) {
    val context = LocalContext.current
    val selectedPackages by YouLanPreferences.selectedPackages(context).collectAsState(initial = emptySet())
    var selectedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    LaunchedEffect(selectedPackages) { selectedApps = withContext(Dispatchers.IO) { InstalledApps.loadSelectedApps(context, selectedPackages) } }
    val accessibilityEnabled = remember(resumeTick) { SystemState.isAccessibilityServiceEnabled(context) }
    val defaultLauncher = remember(resumeTick) { SystemState.isDefaultLauncher(context) }
    BackHandler(enabled = true) {}
    Scaffold(containerColor = MaterialTheme.colorScheme.surface, topBar = {
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).combinedClickable(onClick = {}, onLongClick = { context.startActivity(Intent(context, FamilySettingsActivity::class.java)) }).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("YouLan", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("点一下就能打开 · 长按标题进入家属设置", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            if (!accessibilityEnabled || !defaultLauncher || selectedApps.isEmpty()) {
                SetupPanel(accessibilityEnabled, defaultLauncher, selectedApps.isNotEmpty(),
                    { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) },
                    { context.startActivity(Intent(context, FamilySettingsActivity::class.java)) })
                Spacer(Modifier.height(12.dp))
            }
            if (selectedApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有选择应用\n请进入家属设置添加常用应用", fontSize = 22.sp, lineHeight = 32.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(selectedApps, key = { it.packageName }) { app ->
                        AppCard(app) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) context.startActivity(launchIntent) else Toast.makeText(context, "无法打开 ${app.label}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupPanel(accessibilityEnabled: Boolean, defaultLauncher: Boolean, hasApps: Boolean, onAccessibility: () -> Unit, onHomeSettings: () -> Unit, onFamilySettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("首次设置", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
            StatusRow("辅助导航", accessibilityEnabled); StatusRow("默认桌面", defaultLauncher); StatusRow("常用应用", hasApps); Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!accessibilityEnabled) Button(onClick = onAccessibility, modifier = Modifier.weight(1f)) { Text("开启辅助") }
                if (!defaultLauncher) Button(onClick = onHomeSettings, modifier = Modifier.weight(1f)) { Text("设为桌面") }
                if (!hasApps) Button(onClick = onFamilySettings, modifier = Modifier.weight(1f)) { Text("选择应用") }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "●" else "○", color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), fontSize = 18.sp)
        Text("  $label", fontSize = 16.sp); Spacer(Modifier.weight(1f)); Text(if (ok) "已完成" else "待设置", fontSize = 14.sp)
    }
}

@Composable
private fun AppCard(app: AppEntry, onClick: () -> Unit) {
    val iconBitmap = remember(app.packageName) { app.icon.toBitmap(width = 144, height = 144).asImageBitmap() }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().height(190.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(bitmap = iconBitmap, contentDescription = app.label, modifier = Modifier.size(88.dp)); Spacer(Modifier.height(14.dp))
            Text(app.label, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}
