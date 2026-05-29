package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DonationCamp
import com.example.data.DonationHistory
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BloodConnectViewModel,
    onNavigateToFinder: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val donors by viewModel.allDonors.collectAsState()
    val requests by viewModel.allRequests.collectAsState()
    val camps by viewModel.allCamps.collectAsState()
    val histories by viewModel.allHistory.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val profile by viewModel.registeredProfile.collectAsState()
    val alertLogs by viewModel.smsAlertLogs.collectAsState()

    var showSosDialog by remember { mutableStateOf(false) }
    var selectedGroupTab by remember { mutableStateOf("Overall") } // "Overall", "Department-wise"

    // Stat aggregates
    val totalDonors = donors.size
    val activeRequests = requests.count { !it.isFulfilled }
    val completedDonations = histories.sumOf { it.unitsDonated }
    val livesSaved = completedDonations * 3 // Blood donation standard: 1 unit saves up to 3 lives

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Welcome and Role Switcher Module
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Paavai BloodConnect",
                                style = MaterialTheme.typography.titleMedium,
                                color = LightGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"Every Student Can Save a Life.\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Role Picker dropdown indicator
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = userRole,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Hello, ${profile?.name ?: "Paavai Member"}!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if(profile != null) "Logged in as ${profile?.userType} • ${profile?.bloodGroup}" else "Register to obtain your college QR Donor ID",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Toggles for Reviewer
                    Text(
                        text = "Switch role experience:",
                        color = LightGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf("Student Donor", "College Admin", "Hospital/Blood Bank")
                        roles.forEach { role ->
                            val isSelected = role == userRole
                            FilledTonalButton(
                                onClick = { viewModel.setRole(role) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("role_btn_${role.lowercase().replace(" ", "_")}"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) PaavaiGold else Color.White.copy(alpha = 0.15f),
                                    contentColor = if (isSelected) DarkCharcoal else Color.White
                                ),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(role.replace("/Blood Bank", ""), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Quick Impact Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Lives Impacted",
                    value = "$livesSaved",
                    icon = Icons.Default.Favorite,
                    color = BloodCrimson
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Requests",
                    value = "$activeRequests",
                    icon = Icons.Default.Warning,
                    color = if (activeRequests > 0) PaavaiGold else SuccessGreen
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Registered Donors",
                    value = "$totalDonors",
                    icon = Icons.Default.People,
                    color = InfoBlue
                )
            }
        }

        // One-Tap SOS Urgent Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("one_tap_sos_card")
                    .clickable { showSosDialog = true },
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BloodCrimson.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BloodCrimson.copy(alpha = 0.12f), DarkCharcoal)
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(BloodCrimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationImportant,
                            contentDescription = "SOS",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EMERGENCY BRIEF: SOS",
                            style = MaterialTheme.typography.titleMedium,
                            color = BloodCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "One-tap request for rapid blood dispatch. System automatically alerts closest compatible student records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightSlate
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Go",
                        tint = BloodCrimson
                    )
                }
            }
        }

        // Live alert logs (push notifications simulator)
        if (alertLogs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PaavaiGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Alert Alert",
                                tint = DeepMaroon
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Emergency Alert Dispatches",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepMaroon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        alertLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Divider(color = Color.Black.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }

        // Active Emergency requests Carousel Ticker
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Emergency Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                TextButton(onClick = onNavigateToRequests) {
                    Text("View All", color = DeepMaroon, fontWeight = FontWeight.Bold)
                }
            }
        }

        val unfulfilledRequests = requests.filter { !it.isFulfilled }
        if (unfulfilledRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All Clear",
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active emergency requests!",
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "All student requirements are currently met. Great work!",
                            fontSize = 12.sp,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(unfulfilledRequests.take(2)) { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRequests() },
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (request.urgencyLevel == "Critical") BloodCrimson.copy(alpha = 0.3f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Blood group indicator circle
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (request.urgencyLevel == "Critical") BloodCrimson else DeepMaroon,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = request.bloodGroup,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = request.patientName,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Badge
                                Surface(
                                    color = if (request.urgencyLevel == "Critical") BloodCrimson else PaavaiGold,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = request.urgencyLevel.uppercase(),
                                        color = if (request.urgencyLevel == "Critical") Color.White else TextDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = request.hospitalName,
                                fontSize = 12.sp,
                                color = LightSlate
                            )
                            Text(
                                text = "${request.unitsRequired} Units Required",
                                fontSize = 12.sp,
                                color = DeepMaroon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = LightSlate
                        )
                    }
                }
            }
        }

        // Upcoming Paavai Camps Section
        item {
            Text(
                text = "Upcoming Blood Donation Camps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (camps.isEmpty()) {
            item {
                Text(
                    text = "No scheduled camps at the moment.",
                    color = LightSlate,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(camps) { camp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = camp.title,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = camp.venue,
                                        fontSize = 12.sp,
                                        color = LightSlate
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Date",
                                        tint = InfoBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${camp.date} • ${camp.time}",
                                        fontSize = 11.sp,
                                        color = LightSlate
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(WarmSlate, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${camp.registeredCount}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepMaroon,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Registered",
                                        fontSize = 8.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }

                        Text(
                            text = camp.Description,
                            fontSize = 11.sp,
                            color = LightSlate,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Register button
                        Button(
                            onClick = {
                                if (profile != null) {
                                    viewModel.registerForCamp(
                                        camp.id,
                                        camp.title,
                                        profile!!.registerNumber,
                                        profile!!.name,
                                        profile!!.bloodGroup
                                    )
                                } else {
                                    onNavigateToRegister()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_camp_btn_${camp.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = "Reg",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (profile != null) "Register Commitment To Donate" else "Register Donor Profile First",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Blood Donation Leaderboard Tab
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Leaderboard",
                                tint = PaavaiGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Paavai Donor Leaderboard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        // Compact Segmented Tabs
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarmSlate)
                                .padding(2.dp)
                        ) {
                            listOf("Overall", "Dept-wise").forEach { name ->
                                val active = (name == "Overall" && selectedGroupTab == "Overall") || (name == "Dept-wise" && selectedGroupTab == "Department-wise")
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else LightSlate,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) DeepMaroon else Color.Transparent)
                                        .clickable { selectedGroupTab = if (name == "Overall") "Overall" else "Department-wise" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedGroupTab == "Overall") {
                        // Top individual donors (calculated dynamically or mapped from donors)
                        val sortedDonors = donors.sortedByDescending { it.totalDonations }.take(5)
                        sortedDonors.forEachIndexed { idx, d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Medals/Ranks
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = when (idx) {
                                                    0 -> PaavaiGold
                                                    1 -> Color(0xFFB0BEC5)
                                                    2 -> Color(0xFFCD7F32)
                                                    else -> WarmSlate
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (idx < 3) Color.DarkGray else LightSlate
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = d.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "${d.department} • ${d.year}",
                                            fontSize = 10.sp,
                                            color = LightSlate
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = RedLightBG,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${d.bloodGroup}",
                                            color = BloodCrimson,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${d.totalDonations} Donations",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepMaroon,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (idx < sortedDonors.size - 1) Divider(color = CardBorder)
                        }
                    } else {
                        // Department aggregates
                        val deptGroups = donors.groupBy { it.department }
                            .mapValues { entry -> entry.value.sumOf { it.totalDonations } }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)

                        deptGroups.forEachIndexed { idx, (dept, total) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${idx + 1}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepMaroon,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = dept,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                }
                                Text(
                                    text = "$total Units",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen,
                                    fontSize = 13.sp
                                )
                            }
                            if (idx < deptGroups.size - 1) Divider(color = CardBorder)
                        }
                    }
                }
            }
        }
    }

    // SOS Request Dialog Trigger
    if (showSosDialog) {
        var bloodGroup by remember { mutableStateOf("O-") }
        var patientName by remember { mutableStateOf("") }
        var hospitalName by remember { mutableStateOf("Paavai Multi Speciality Hospital, Namakkal") }
        var unitsNeeded by remember { mutableStateOf("1") }
        var contactName by remember { mutableStateOf(profile?.name ?: "") }
        var contactNumber by remember { mutableStateOf(profile?.mobileNumber ?: "") }
        var errorText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showSosDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(DarkCharcoal)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BloodCrimson, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Quick",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTANT ONE-TAP SOS",
                            style = MaterialTheme.typography.titleMedium,
                            color = BloodCrimson,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Instantly broadcast a highly critical, immediate need. Our system will contact nearby student clusters.",
                        fontSize = 11.sp,
                        color = LightSlate
                    )

                    Divider()

                    // Blood group dropdown simulator
                    Text("Blood Group Needed:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("O-", "O+", "A-", "A+", "B+", "AB+").forEach { bg ->
                            val active = bg == bloodGroup
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) BloodCrimson else WarmSlate)
                                    .clickable { bloodGroup = bg }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextDark
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        modifier = Modifier.fillMaxWidth().testTag("sos_patient_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital Name & Branch") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unitsNeeded,
                            onValueChange = { unitsNeeded = it },
                            label = { Text("Units") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text("Contact Mobile") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    if (errorText.isNotEmpty()) {
                        Text(text = errorText, color = BloodCrimson, fontSize = 11.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSosDialog = false }) {
                            Text("Cancel", color = LightSlate)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (patientName.trim().isEmpty() || contactNumber.trim().isEmpty()) {
                                    errorText = "Please fill in patient name and mobile number."
                                    return@Button
                                }
                                val u = unitsNeeded.toIntOrNull() ?: 1
                                viewModel.triggerSOS(
                                    bloodGroup = bloodGroup,
                                    patientName = patientName,
                                    hospitalName = hospitalName,
                                    units = u,
                                    contactName = contactName.ifEmpty { "Emergency Contact" },
                                    contactPhone = contactNumber
                                )
                                showSosDialog = false
                                onNavigateToFinder() // Send user to finder to see AI matches immediately!
                            },
                            modifier = Modifier.testTag("sos_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson)
                        ) {
                            Text("BROADCAST SOS ALERT")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.height(105.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                        )
                    }
                }
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark,
                    fontSize = 20.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = LightSlate,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
