package com.paysync.gateway

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TransactionLog(
    val id: String,
    val gateway: String,
    val amount: String,
    val sender: String,
    val time: String,
    val status: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF10B981),
                    secondary = Color(0xFF3B82F6),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PaySyncApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaySyncApp() {
    var selectedTab by remember { mutableStateOf(0) }
    var isServiceActive by remember { mutableStateOf(true) }
    var selectedSim by remember { mutableStateOf("SIM 2") }
    
    // Gateway Toggles
    var bkashEnabled by remember { mutableStateOf(true) }
    var nagadEnabled by remember { mutableStateOf(true) }
    var rocketEnabled by remember { mutableStateOf(true) }

    // Universal Telegram Bot Config
    var botToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }
    var isTestingConnection by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val sampleLogs = remember {
        mutableStateListOf(
            TransactionLog("BLA9X10QZ", "bKash", "৳ 500.00", "017XXXXXXXX", "Just now", "Approved"),
            TransactionLog("NGD84928A", "Nagad", "৳ 120.00", "019XXXXXXXX", "10 mins ago", "Approved"),
            TransactionLog("RCK48194M", "Rocket", "৳ 1,000.00", "018XXXXXXXX", "1 hour ago", "Approved")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PaySync Gateway",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.SimCard, contentDescription = "SIMs") },
                    label = { Text("SIM & Filter") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Bot") },
                    label = { Text("Bot Connect") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    isActive = isServiceActive,
                    onToggleService = { isServiceActive = it },
                    logs = sampleLogs
                )
                1 -> SimFilterScreen(
                    selectedSim = selectedSim,
                    onSimSelect = { selectedSim = it },
                    bkash = bkashEnabled,
                    onBkashChange = { bkashEnabled = it },
                    nagad = nagadEnabled,
                    onNagadChange = { nagadEnabled = it },
                    rocket = rocketEnabled,
                    onRocketChange = { rocketEnabled = it }
                )
                2 -> BotConnectScreen(
                    botToken = botToken,
                    onTokenChange = { botToken = it },
                    chatId = chatId,
                    onChatIdChange = { chatId = it },
                    isTesting = isTestingConnection,
                    onTestClick = {
                        if (botToken.isBlank() || chatId.isBlank()) {
                            Toast.makeText(context, "Bot Token এবং Chat ID দিন!", Toast.LENGTH_SHORT).show()
                            return@BotConnectScreen
                        }
                        isTestingConnection = true
                        coroutineScope.launch {
                            val success = testTelegramBot(botToken, chatId)
                            isTestingConnection = false
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    Toast.makeText(context, "সফল! বটে টেস্ট মেসেজ পাঠানো হয়েছে ✅", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "ব্যর্থ! Bot Token বা Chat ID চেক করুন ❌", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    isActive: Boolean,
    onToggleService: (Boolean) -> Unit,
    logs: List<TransactionLog>
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isActive) "Gateway Running" else "Gateway Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isActive) "Listening to incoming SMS & Push" else "Service stopped",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = onToggleService)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Today Count", "${logs.size}", Modifier.weight(1f), Color(0xFF3B82F6))
                StatCard("Total BDT", "৳ 1,620", Modifier.weight(1f), Color(0xFF10B981))
            }
        }

        item {
            Text(text = "Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(logs) { log ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeColor = when (log.gateway) {
                            "bKash" -> Color(0xFFE2136E)
                            "Nagad" -> Color(0xFFF7941D)
                            else -> Color(0xFF8C3494)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(log.gateway, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = log.id, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "${log.sender} • ${log.time}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Text(text = log.amount, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }
    }
}

@Composable
fun SimFilterScreen(
    selectedSim: String,
    onSimSelect: (String) -> Unit,
    bkash: Boolean,
    onBkashChange: (Boolean) -> Unit,
    nagad: Boolean,
    onNagadChange: (Boolean) -> Unit,
    rocket: Boolean,
    onRocketChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Target SIM Selection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("যে সিমে বটের টাকা আসে সেটি সিলেক্ট করুন:", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SIM 1", "SIM 2", "Both SIMs").forEach { sim ->
                        FilterChip(
                            selected = selectedSim == sim,
                            onClick = { onSimSelect(sim) },
                            label = { Text(sim) }
                        )
                    }
                }
            }
        }

        Text("Active Gateways", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                GatewaySwitch("bKash Gateway", Color(0xFFE2136E), bkash, onBkashChange)
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                GatewaySwitch("Nagad Gateway", Color(0xFFF7941D), nagad, onNagadChange)
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                GatewaySwitch("Rocket Gateway", Color(0xFF8C3494), rocket, onRocketChange)
            }
        }
    }
}

@Composable
fun BotConnectScreen(
    botToken: String,
    onTokenChange: (String) -> Unit,
    chatId: String,
    onChatIdChange: (String) -> Unit,
    isTesting: Boolean,
    onTestClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Universal Telegram Bot Link", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("কোনো বটের কোড পরিবর্তন ছাড়াই যেকোনো বটে সংযুক্ত করুন:", fontSize = 12.sp, color = Color.Gray)
                
                OutlinedTextField(
                    value = botToken,
                    onValueChange = onTokenChange,
                    label = { Text("Telegram Bot Token") },
                    placeholder = { Text("123456789:ABCdefGhIJKlmNo...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chatId,
                    onValueChange = onChatIdChange,
                    label = { Text("Target Chat ID / Admin ID") },
                    placeholder = { Text("e.g. 192847291") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = onTestClick,
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Connection to Bot")
                    }
                }
            }
        }
    }
}

@Composable
fun GatewaySwitch(title: String, color: Color, state: Boolean, onStateChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Medium)
        }
        Switch(checked = state, onCheckedChange = onStateChange)
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier, accentColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

suspend fun testTelegramBot(token: String, chatId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val message = "🟢 *PaySync Gateway Connected!*\n\nআপনার অ্যান্ড্রয়েড অ্যাপ সফলভাবে বটের সাথে কানেক্ট হয়েছে।"
        val url = URL("https://api.telegram.org/bot$token/sendMessage")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        
        val postData = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                "&text=" + URLEncoder.encode(message, "UTF-8") +
                "&parse_mode=Markdown"
                
        OutputStreamWriter(conn.outputStream).use { it.write(postData) }
        conn.responseCode == 200
    } catch (e: Exception) {
        false
    }
}
