package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.EngagementEvent
import com.example.ui.components.ChartColors
import com.example.ui.components.DistributionDonutChart
import com.example.ui.components.EngagementEventRow
import com.example.ui.components.MetricBarChart
import com.example.ui.components.RealTimeAreaChart
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern colors for dark theme
val ObsidianBg = Color(0xFF090B10)
val DarkCardBg = Color(0xFF11151D)
val GlowBorderColor = Color(0xFF1E293B)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsDashboard(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val totalEventsCount by viewModel.totalEventCount.collectAsState()
    val filteredEvents by viewModel.filteredEvents.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val speedMultiplier by viewModel.simulationSpeedMultiplier.collectAsState()
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    val platformFilter by viewModel.selectedPlatform.collectAsState()
    val eventTypeFilter by viewModel.selectedEventType.collectAsState()

    var activeTab by remember { mutableStateOf("Live Stream") }

    // Pulsing animation for the "LIVE" indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Compute dynamic dashboard metrics based on filteredEvents
    val activeUsersLastMinute = remember(filteredEvents) {
        val now = System.currentTimeMillis()
        filteredEvents.filter { now - it.timestamp <= 60000 }
            .distinctBy { it.userId }
            .size
            .coerceAtLeast(1)
    }

    val totalRevenue = remember(filteredEvents) {
        filteredEvents.filter { it.eventType == "purchase" }.sumOf { it.value }
    }

    val errorRatePercentage = remember(filteredEvents) {
        if (filteredEvents.isEmpty()) 0f
        else {
            val errors = filteredEvents.count { it.eventType == "error" }
            (errors.toFloat() / filteredEvents.size * 100)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. TOP HEADER & LIVE INDICATOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "METRICS ENGINE",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Pulsing LIVE dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSimulating) Color(0xFF00FF87).copy(alpha = 0.15f)
                                    else Color.Gray.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(if (isSimulating) pulseAlpha else 1f)
                                    .clip(CircleShape)
                                    .background(if (isSimulating) Color(0xFF00FF87) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSimulating) "LIVE" else "PAUSED",
                                color = if (isSimulating) Color(0xFF00FF87) else Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Real-time user engagement analytics stream",
                        color = ChartColors.TextColor,
                        fontSize = 11.sp
                    )
                }

                // Quick Simulation Switch
                IconButton(
                    onClick = {
                        viewModel.toggleSimulation()
                        viewModel.logDashboardEvent("click", "ToggleSimulationButton")
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSimulating) Color(0xFF9D4EDD).copy(alpha = 0.15f) else Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Simulation Switch",
                        tint = if (isSimulating) ChartColors.Purple else Color.White
                    )
                }
            }

            // 2. SIMULATION CONTROLS BOX (Collapsible/Fine card style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                border = BorderStroke(1.dp, GlowBorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SIMULATION CONTROL",
                            color = ChartColors.TextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Speed: ",
                                color = ChartColors.TextColor,
                                fontSize = 11.sp
                            )
                            listOf(1.0, 2.0, 5.0).forEach { speed ->
                                Text(
                                    text = "${speed.toInt()}x",
                                    color = if (speedMultiplier == speed) ChartColors.Cyan else Color.Gray,
                                    fontWeight = if (speedMultiplier == speed) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.setSimulationSpeed(speed)
                                            viewModel.logDashboardEvent("click", "SetSpeed_${speed.toInt()}x")
                                        }
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.injectTrafficSpike()
                                viewModel.logDashboardEvent("click", "InjectTrafficSpike")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("spike_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChartColors.Cyan.copy(alpha = 0.15f),
                                contentColor = ChartColors.Cyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inject Spike", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.clearLogs()
                                viewModel.logDashboardEvent("click", "ClearLogs")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ChartColors.Pink
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ChartColors.Pink.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. KEY KPI CARD ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KPICard(
                    title = "ACTIVE USER",
                    value = activeUsersLastMinute.toString(),
                    subtitle = "Last 60s active",
                    color = ChartColors.Purple,
                    modifier = Modifier.weight(1f)
                )
                KPICard(
                    title = "REVENUE",
                    value = "$${String.format(Locale.US, "%.0f", totalRevenue)}",
                    subtitle = "Simulated GMV",
                    color = ChartColors.Green,
                    modifier = Modifier.weight(1f)
                )
                KPICard(
                    title = "CRASH RATE",
                    value = "${String.format(Locale.US, "%.1f", errorRatePercentage)}%",
                    subtitle = "Error ratio",
                    color = ChartColors.Pink,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. INTERACTIVE TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCardBg)
                    .border(1.dp, GlowBorderColor, RoundedCornerShape(8.dp))
            ) {
                listOf("Live Stream", "Visual Charts", "Playground").forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabColor by animateColorAsState(
                        if (isSelected) ChartColors.Cyan else Color.Transparent,
                        label = "tabColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tabColor)
                            .clickable {
                                activeTab = tab
                                viewModel.logDashboardEvent("click", "TabSelect_$tab")
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ObsidianBg else Color.White
                        )
                    }
                }
            }

            // 5. VIEW CONTENT CONTAINER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                when (activeTab) {
                    "Live Stream" -> {
                        LiveStreamContent(
                            events = filteredEvents,
                            platformFilter = platformFilter,
                            eventTypeFilter = eventTypeFilter,
                            onPlatformChange = {
                                viewModel.setPlatformFilter(it)
                                viewModel.logDashboardEvent("click", "FilterPlatform_$it")
                            },
                            onTypeChange = {
                                viewModel.setEventTypeFilter(it)
                                viewModel.logDashboardEvent("click", "FilterEventType_$it")
                            },
                            onEventSelect = {
                                viewModel.setSelectedEvent(it)
                                viewModel.logDashboardEvent("click", "ViewEvent_${it.id}")
                            }
                        )
                    }
                    "Visual Charts" -> {
                        VisualChartsContent(events = filteredEvents)
                    }
                    "Playground" -> {
                        PlaygroundContent(
                            onTriggerActualEvent = { type, element ->
                                viewModel.logDashboardEvent(type, element)
                            }
                        )
                    }
                }
            }
        }

        // EVENT DETAIL DIALOG
        selectedEvent?.let { event ->
            EventDetailDialog(
                event = event,
                onDismiss = { viewModel.setSelectedEvent(null) }
            )
        }
    }
}

// ------------------- SUB-COMPONENTS & LAYOUTS -------------------

@Composable
fun KPICard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        border = BorderStroke(1.dp, GlowBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = ChartColors.TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveStreamContent(
    events: List<EngagementEvent>,
    platformFilter: String,
    eventTypeFilter: String,
    onPlatformChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onEventSelect: (EngagementEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Quick interactive Chips for filter selection
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            // Platform row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Platform: ",
                    fontSize = 11.sp,
                    color = ChartColors.TextColor,
                    modifier = Modifier.width(60.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Android", "iOS", "Web").forEach { plat ->
                        val active = platformFilter == plat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) ChartColors.Cyan.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (active) ChartColors.Cyan else GlowBorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPlatformChange(plat) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = plat,
                                color = if (active) ChartColors.Cyan else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Event Type row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Type: ",
                    fontSize = 11.sp,
                    color = ChartColors.TextColor,
                    modifier = Modifier.width(60.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "view", "click", "purchase", "error").forEach { type ->
                        val active = eventTypeFilter == type
                        val activeColor = when (type) {
                            "purchase" -> ChartColors.Green
                            "error" -> ChartColors.Pink
                            "click" -> ChartColors.Cyan
                            "view" -> ChartColors.Purple
                            else -> Color.White
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (active) activeColor else GlowBorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onTypeChange(type) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = type.uppercase(Locale.getDefault()),
                                color = if (active) activeColor else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main List of events
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Empty",
                        tint = ChartColors.TextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No real-time events match current filter",
                        color = ChartColors.TextColor,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("events_list_stream"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EngagementEventRow(
                        event = event,
                        onClick = { onEventSelect(event) }
                    )
                }
            }
        }
    }
}

@Composable
fun VisualChartsContent(events: List<EngagementEvent>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. REAL-TIME ACTIVITY SPARKLINE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, GlowBorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "REAL-TIME TRAFFIC (60S)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Sliding 5-second frequency volumes",
                    color = ChartColors.TextColor,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Group actual filtered events in 5-second bins over the last minute
                val sparklineData = remember(events) {
                    val now = System.currentTimeMillis()
                    val bins = MutableList(12) { 0f }
                    events.forEach { event ->
                        val diffMs = now - event.timestamp
                        val binIndex = (diffMs / 5000).toInt()
                        if (binIndex in 0..11) {
                            bins[11 - binIndex] += 1f
                        }
                    }
                    bins
                }

                RealTimeAreaChart(
                    data = sparklineData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        // 2. PLATFORM & EVENT TYPE BREAKDOWN SIDE BY SIDE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Platform Ring Card
            Card(
                modifier = Modifier.weight(1.1f),
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                border = BorderStroke(1.dp, GlowBorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PLATFORMS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val platformCounts = remember(events) {
                        val map = mutableMapOf("Android" to 0f, "iOS" to 0f, "Web" to 0f)
                        events.forEach {
                            if (it.platform in map) map[it.platform] = (map[it.platform] ?: 0f) + 1f
                        }
                        map
                    }

                    DistributionDonutChart(
                        data = platformCounts,
                        colors = mapOf(
                            "Android" to Color(0xFF3DDC84),
                            "iOS" to Color.White,
                            "Web" to Color(0xFF007ACC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. CATEGORY METRIC COLUMN BARS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, GlowBorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ENGAGEMENT EVENTS BY TYPE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))

                val typeCounts = remember(events) {
                    val map = mutableMapOf(
                        "VIEW" to 0f,
                        "CLICK" to 0f,
                        "BUY" to 0f,
                        "ERROR" to 0f
                    )
                    events.forEach {
                        when (it.eventType) {
                            "view" -> map["VIEW"] = (map["VIEW"] ?: 0f) + 1f
                            "click" -> map["CLICK"] = (map["CLICK"] ?: 0f) + 1f
                            "purchase" -> map["BUY"] = (map["BUY"] ?: 0f) + 1f
                            "error" -> map["ERROR"] = (map["ERROR"] ?: 0f) + 1f
                        }
                    }
                    map
                }

                MetricBarChart(
                    metrics = typeCounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    barColor = ChartColors.Cyan
                )
            }
        }

        // 4. TOP COUNTRY ACTIVITY LEADERBOARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, GlowBorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "TOP COUNTRY ACTIVITY",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                val topCountries = remember(events) {
                    events.groupBy { it.country }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(5)
                }

                if (topCountries.isEmpty()) {
                    Text("No country data logged", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    val maxCount = topCountries.firstOrNull()?.second?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    topCountries.forEach { (country, count) ->
                        val ratio = count.toFloat() / maxCount
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = country, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "$count events", color = ChartColors.TextColor, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Simple responsive capsule bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(ChartColors.Purple, ChartColors.Cyan)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaygroundContent(
    onTriggerActualEvent: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, GlowBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Playground",
                    tint = ChartColors.Cyan,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "INTERACTIVE INSTRUMENTATION",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap any action below to trigger actual system telemetry events. Your actions will be recorded and logged dynamically in the Live Stream in real-time!",
                    color = ChartColors.TextColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GlowBorderColor)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. TRIGGER SCREEN VIEWS",
                    color = ChartColors.Purple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HomeFeed", "SearchCatalogs", "ProductViewer", "CartCheck", "SecurityCenter").forEach { screen ->
                        Button(
                            onClick = { onTriggerActualEvent("view", screen) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2E)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(text = "View $screen", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "2. TRIGGER INTERACTIVE CLICKS",
                    color = ChartColors.Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SubscribeNow", "ApplyCoupon", "ShareStats", "CustomerSupport").forEach { action ->
                        Button(
                            onClick = { onTriggerActualEvent("click", action) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1E24)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(text = "Click $action", fontSize = 11.sp, color = ChartColors.Cyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "3. SIMULATE ERROR CORRUPTIONS",
                    color = ChartColors.Pink,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DatabaseLock", "GatewayTimeout", "SSLHandshakeFailed").forEach { err ->
                        Button(
                            onClick = { onTriggerActualEvent("error", err) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26101B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(text = "Crash $err", fontSize = 11.sp, color = ChartColors.Pink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailDialog(
    event: EngagementEvent,
    onDismiss: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }
    val timeStr = formatter.format(Date(event.timestamp))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("detail_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, GlowBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVENT ATOM RAW",
                        color = ChartColors.Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = GlowBorderColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Structured JSON format styling
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF07090D))
                        .padding(12.dp)
                ) {
                    Text(text = "{", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    JsonLine("id", event.id.toString(), isString = false)
                    JsonLine("userId", event.userId, isString = true)
                    JsonLine("eventType", event.eventType, isString = true)
                    JsonLine("elementName", event.elementName, isString = true)
                    JsonLine("platform", event.platform, isString = true)
                    JsonLine("country", event.country, isString = true)
                    JsonLine("timestamp", event.timestamp.toString(), isString = false)
                    JsonLine("timeReadable", timeStr, isString = true)
                    JsonLine("metricValue", event.value.toString(), isString = false, last = true)
                    Text(text = "}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_dialog_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Inspect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JsonLine(key: String, value: String, isString: Boolean, last: Boolean = false) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)) {
        Text(text = "\"$key\": ", color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(
            text = if (isString) "\"$value\"" else value,
            color = if (isString) Color(0xFF3DDC84) else Color(0xFFF59E0B),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        if (!last) {
            Text(text = ",", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

// Simple custom implementation of BorderStroke because androidx.compose.foundation.BorderStroke is standard
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return remember(width, color) {
        androidx.compose.foundation.BorderStroke(width, color)
    }
}
