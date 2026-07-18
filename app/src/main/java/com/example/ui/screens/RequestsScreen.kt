package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BloodRequest
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    viewModel: BloodConnectViewModel,
    onNavigateToFinder: () -> Unit
) {
    val context = LocalContext.current
    val requests by viewModel.allRequests.collectAsState()
    val donors by viewModel.allDonors.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val profile by viewModel.registeredProfile.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Active Queue, 1: Hospital Console
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedFulfillRequest by remember { mutableStateOf<BloodRequest?>(null) }

    // State parameters for Hospital Broadcast Tab
    val hospitalAffiliates = remember {
        listOf(
            HospitalPartnerInfo("Paavai Multi Speciality Hospital, Namakkal", "WHO-LIC-7842-PV01", "Namakkal"),
            HospitalPartnerInfo("Namakkal Government Headquarters Hospital", "IN-NMK-2025-GH04", "Namakkal"),
            HospitalPartnerInfo("Salem Government General Hospital", "IN-SLM-2024-GH09", "Salem"),
            HospitalPartnerInfo("Salem Blood Bank Trust", "IN-SLM-2026-BB12", "Salem")
        )
    }
    var selectedHospitalIndex by remember { mutableStateOf(0) }
    var hospitalPatientName by remember { mutableStateOf("Senthil Kumar (Emergency Case)") }
    var hospitalBloodGroup by remember { mutableStateOf("O-") }
    var hospitalUnitsRequired by remember { mutableStateOf("2") }
    var hospitalUrgencyLevel by remember { mutableStateOf("Critical") }
    var hospitalCoverageZone by remember { mutableStateOf("Namakkal") } // "Namakkal", "Salem", "Paavai Campus"
    var hospitalRadius by remember { mutableStateOf(5) } // km

    val hospitalLogs = remember { mutableStateListOf<String>() }
    var showBroadcastSuccessToast by remember { mutableStateOf(false) }
    var activeBroadcastJobNum by remember { mutableStateOf("") }

    var hospitalRequiredDate by remember { mutableStateOf("2026-07-18") }
    var hospitalSpecialInstructions by remember { mutableStateOf("Report to Emergency Block reception desk on arrival.") }
    var feedbackTargetRequest by remember { mutableStateOf<BloodRequest?>(null) }

    // Global matching forecasts accessible to both screen tabs and dialog overlays
    val compatibleGroups = remember(hospitalBloodGroup) {
        when (hospitalBloodGroup.uppercase()) {
            "O-" -> listOf("O-")
            "O+" -> listOf("O-", "O+")
            "A-" -> listOf("O-", "A-")
            "A+" -> listOf("O-", "O+", "A-", "A+")
            "B-" -> listOf("O-", "B-")
            "B+" -> listOf("O-", "O+", "B-", "B+")
            "AB-" -> listOf("O-", "A-", "B-", "AB-")
            "AB+" -> listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
            else -> listOf(hospitalBloodGroup)
        }
    }

    val matchedNearbyDonors = remember(donors, hospitalBloodGroup, hospitalCoverageZone) {
        donors.filter { donor ->
            val isCompatible = compatibleGroups.contains(donor.bloodGroup)
            val isEligible = donor.availability
            val locationMatches = when (hospitalCoverageZone) {
                "Namakkal Region" -> donor.location.contains("Namakkal", ignoreCase = true) || donor.location.contains("Campus", ignoreCase = true)
                "Salem Region" -> donor.location.contains("Salem", ignoreCase = true)
                "Erode Region" -> donor.location.contains("Erode", ignoreCase = true)
                else -> donor.location.contains("Campus", ignoreCase = true) || donor.location.isEmpty()
            }
            isCompatible && isEligible && locationMatches
        }
    }
    
    // Core Outer Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
    ) {
        // Shared Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emergency Requests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
                Text(
                    text = "Broadcast & coordinate rapid blood mobilization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightSlate
                )
            }
            
            // Allow manual requests triggers for administrative/volunteer overrides in Active Queue
            if (selectedTab == 0 && (userRole == "Volunteer" || userRole == "Admin")) {
                Button(
                    onClick = { showRequestDialog = true },
                    modifier = Modifier.testTag("create_request_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Request", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Beautiful Navigation Tabs for splitting concerns
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCharcoal,
            contentColor = Color.White,
            divider = { Divider(color = CardBorder) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active SOS Queue", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = BloodCrimson,
                unselectedContentColor = LightSlate
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Hospital Console", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = BloodCrimson,
                unselectedContentColor = LightSlate
            )
        }

        // Tab Content Router
        if (selectedTab == 0) {
            // ==========================================
            // TAB 0: PUBLIC EMERGENCIES QUEUE (ORIGINAL)
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Active Requests count ticker
                val activeCount = requests.count { !it.isFulfilled }
                val fulfilledCount = requests.count { it.isFulfilled }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Active Requests", fontSize = 11.sp, color = LightSlate)
                                Text("$activeCount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = BloodCrimson)
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(CardBorder)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Fulfilled Requests", fontSize = 11.sp, color = LightSlate)
                                Text("$fulfilledCount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                            }
                        }
                    }
                }

                // Requests Lists Header
                item {
                    Text(
                        text = "Recent Blood Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                if (requests.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = "Empty",
                                    tint = LightSlate,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No blood requests found", fontWeight = FontWeight.Bold, color = TextDark)
                                Text(
                                    text = "No clinical requests on catalog. Use the Request button to log an entry.",
                                    fontSize = 11.sp,
                                    color = LightSlate
                                )
                            }
                        }
                    }
                } else {
                    items(requests) { request ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_card_${request.id}"),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (request.urgencyLevel == "Critical" && !request.isFulfilled) BloodCrimson.copy(alpha = 0.5f) else CardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Blood circle
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    if (request.isFulfilled) Color(0xFFE8F5E9) else if (request.urgencyLevel == "Critical") RedLightBG else WarmSlate,
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = request.bloodGroup,
                                                color = if (request.isFulfilled) SuccessGreen else if (request.urgencyLevel == "Critical") BloodCrimson else TextDark,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = request.patientName,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark,
                                                    fontSize = 15.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                // Urgency badge
                                                Surface(
                                                    color = if (request.isFulfilled) SuccessGreen else if (request.urgencyLevel == "Critical") BloodCrimson else if (request.urgencyLevel == "High") PaavaiGold else InfoBlue,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.wrapContentSize()
                                                ) {
                                                    Text(
                                                        text = if(request.isFulfilled) "FULFILLED" else request.urgencyLevel.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Text(
                                                text = request.hospitalName,
                                                fontSize = 12.sp,
                                                color = LightSlate
                                            )
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                                // Info rows
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Units Needed: ${request.unitsRequired} Units",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "Posted: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(request.timestamp))}",
                                            fontSize = 10.sp,
                                            color = LightSlate
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Contact: ${request.contactName} (${request.contactNumber})",
                                                fontSize = 11.sp,
                                                color = TextDark
                                            )
                                            // Live CALL trigger
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Dial Phone Number",
                                                tint = SuccessGreen,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        try {
                                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                                data = Uri.parse("tel:${request.contactNumber}")
                                                            }
                                                            context.startActivity(dialIntent)
                                                        } catch (e: Exception) {
                                                            // ignore
                                                        }
                                                    }
                                            )
                                            // Live EMAIL trigger
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Send Email Inquiry",
                                                tint = InfoBlue,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        try {
                                                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                                data = Uri.parse("mailto:bloodconnect@paavai.edu.in")
                                                                putExtra(Intent.EXTRA_SUBJECT, "Blood Donation Request Concern")
                                                                putExtra(Intent.EXTRA_TEXT, "Hello ${request.contactName},\nRegarding the entry for ${request.bloodGroup} at ${request.hospitalName}, we can support blood mobilization.")
                                                            }
                                                            context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                                                        } catch (e: Exception) {
                                                            // ignore
                                                        }
                                                    }
                                            )
                                        }
                                    }

                                    // Matching Actions / Role Protection (Architectural Clarity)
                                    if (!request.isFulfilled) {
                                        if (userRole == "Volunteer" || userRole == "Admin") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // Fulfill trigger
                                                Button(
                                                    onClick = { selectedFulfillRequest = request },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "", modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Fulfill", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                
                                                // Match trigger
                                                IconButton(
                                                    onClick = {
                                                        viewModel.performAiMatching(request)
                                                        onNavigateToFinder() // Redirect to finder screen to see matchmaking
                                                    },
                                                    modifier = Modifier
                                                        .background(DeepMaroon, CircleShape)
                                                        .size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Psychology,
                                                        contentDescription = "Match",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            // Student Donor flow: Commit to Donate!
                                            val isUserEligible = profile?.let {
                                                viewModel.checkIfEligible(it.lastDonationDate, it.weight, it.gender, it.dob)
                                            } ?: true

                                            var hasCommitted by remember { mutableStateOf(false) }
                                            Button(
                                                onClick = {
                                                    if (isUserEligible) {
                                                        hasCommitted = true
                                                        try {
                                                            // Dial coordinator contact instantly for immediate coordination!
                                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                                data = Uri.parse("tel:${request.contactNumber}")
                                                            }
                                                            context.startActivity(dialIntent)
                                                        } catch (e: Exception) {
                                                            // ignore
                                                        }
                                                    }
                                                },
                                                enabled = isUserEligible,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (hasCommitted) SuccessGreen else if (!isUserEligible) Color.Gray else BloodCrimson,
                                                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                modifier = Modifier.testTag("commit_donate_btn_${request.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (hasCommitted) Icons.Default.Favorite else if (!isUserEligible) Icons.Default.Block else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Love",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (hasCommitted) "COMMITTED ♥" else if (!isUserEligible) "RECOVERY (90D)" else "COMMIT TO DONATE",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Topped-up",
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ===============================================
            // TAB 1: VERIFIED HOSPITAL EMERGENCY DISPATCHER
            // ===============================================
            val activeHospital = hospitalAffiliates[selectedHospitalIndex]
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // 1. Hospital Authorization & Verification Credentials Badge
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("hospital_auth_badge_card"),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        border = BorderStroke(1.2.dp, PaavaiGold),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Shield",
                                    tint = PaavaiGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUTHORIZED CLINICAL PORTAL NODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = PaavaiGold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Hospital affiliation dropdown
                            Text("Hospital Affiliation Identity:", fontSize = 11.sp, color = LightSlate, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var dropdownExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarmSlate, RoundedCornerShape(8.dp))
                                    .clickable { dropdownExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = activeHospital.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Licence: ${activeHospital.licenseNumber}", fontSize = 10.sp, color = LightSlate)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(color = SuccessGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                                Text("ACTIVE", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = LightSlate)
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    hospitalAffiliates.forEachIndexed { index, affiliate ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(affiliate.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("Code: ${affiliate.licenseNumber} • ${affiliate.region}", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            },
                                            onClick = {
                                                selectedHospitalIndex = index
                                                dropdownExpanded = false
                                                // Pre-populate correct region based on hospital
                                                hospitalCoverageZone = if (affiliate.region == "Salem") "Salem Region" else "Namakkal Region"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Target Parameters Customization Configuration
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "🎯 Configure targeted wireless SOS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            OutlinedTextField(
                                value = hospitalPatientName,
                                onValueChange = { hospitalPatientName = it },
                                label = { Text("Patient Case/Reference Code") },
                                modifier = Modifier.fillMaxWidth().testTag("hospital_patient_input"),
                                isError = hospitalPatientName.isEmpty(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = hospitalRequiredDate,
                                    onValueChange = { hospitalRequiredDate = it },
                                    label = { Text("Required By Date") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    modifier = Modifier.weight(1f).testTag("hospital_req_date"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = hospitalSpecialInstructions,
                                    onValueChange = { hospitalSpecialInstructions = it },
                                    label = { Text("Special Instructions") },
                                    placeholder = { Text("e.g. Room 402, Emergency Ward") },
                                    modifier = Modifier.weight(1.5f).testTag("hospital_instructions"),
                                    singleLine = true
                                )
                            }

                            // Blood group selector matrix
                            Text("Target Blood Group Required:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-").forEach { bg ->
                                    val active = bg == hospitalBloodGroup
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) BloodCrimson else WarmSlate)
                                            .clickable { hospitalBloodGroup = bg }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = bg,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else TextDark
                                        )
                                    }
                                }
                            }

                            // Units required input & urgency priority levels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = hospitalUnitsRequired,
                                    onValueChange = { hospitalUnitsRequired = it },
                                    label = { Text("Volume (Units)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Column(modifier = Modifier.weight(2f)) {
                                    Text("Emergency Priority Level:", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Normal", "High", "Critical").forEach { level ->
                                            val active = level == hospitalUrgencyLevel
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (active) {
                                                            when (level) {
                                                                "Critical" -> BloodCrimson
                                                                "High" -> PaavaiGold
                                                                else -> InfoBlue
                                                            }
                                                        } else WarmSlate
                                                    )
                                                    .clickable { hospitalUrgencyLevel = level }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(level, fontSize = 10.sp, color = if (active) Color.White else TextDark, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Coverage geo-filters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Geo-Coverage Zone:", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Namakkal Region", "Salem Region", "Paavai Campus").forEach { zone ->
                                            val active = zone == hospitalCoverageZone
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (active) DeepMaroon else WarmSlate)
                                                    .clickable { hospitalCoverageZone = zone }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(zone.replace(" Region", ""), fontSize = 9.sp, color = if (active) Color.White else TextDark, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Broadcast Range: $hospitalRadius km", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = hospitalRadius.toFloat(),
                                        onValueChange = { hospitalRadius = it.toInt() },
                                        valueRange = 2f..25f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = PaavaiGold,
                                            activeTrackColor = BloodCrimson,
                                            inactiveTrackColor = WarmSlate
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Real-Time Compatibility Match Forecast & Distance Matrix
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "📡 Compatibility Coverage Forecast",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Eligible matching donors within $hospitalRadius km range of Salem/Namakkal towers.",
                                        fontSize = 11.sp,
                                        color = LightSlate
                                    )
                                }
                                
                                Surface(
                                    color = if (matchedNearbyDonors.isNotEmpty()) SuccessGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "REACH INDEX: ${matchedNearbyDonors.size} DONORS",
                                        color = if (matchedNearbyDonors.isNotEmpty()) SuccessGreen else Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = CardBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Compatible Groups matching $hospitalBloodGroup: ${compatibleGroups.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = PaavaiGold,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // List compatible nearby donors dynamically
                            if (matchedNearbyDonors.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(WarmSlate, RoundedCornerShape(8.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = LightSlate, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("0 compatible active donors found nearby in $hospitalCoverageZone.", fontSize = 11.sp, color = LightSlate)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    matchedNearbyDonors.take(4).forEachIndexed { index, donor ->
                                        // estimate proximity based on index and radius seed
                                        val distanceEstimate = String.format(Locale.getDefault(), "%.1f", 0.5 + (index * 1.3) % (hospitalRadius - 0.5))
                                        val cleanLocationName = donor.location.ifEmpty { "Paavai main campus" }
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(WarmSlate)
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Blood bubble
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(RedLightBG, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(donor.bloodGroup, color = BloodCrimson, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(donor.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    Text("🎯 Location: $cleanLocationName ($distanceEstimate km nearby)", fontSize = 10.sp, color = LightSlate)
                                                }
                                            }
                                            
                                            Column(horizontalAlignment = Alignment.End) {
                                                Surface(color = SuccessGreen, shape = RoundedCornerShape(4.dp)) {
                                                    Text("READY", fontSize = 8.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.ExtraBold)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("ID: ${donor.registerNumber}", fontSize = 8.sp, color = LightSlate)
                                            }
                                        }
                                    }
                                    
                                    if (matchedNearbyDonors.size > 4) {
                                        Text("+ ${matchedNearbyDonors.size - 4} additional matched donor nodes registered nearby in local sector map.", fontSize = 10.sp, color = LightSlate, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Trigger SOS Broadcast Button & Live Progress
                item {
                    Button(
                        onClick = {
                            if (hospitalPatientName.trim().isEmpty()) {
                                return@Button
                            }

                            // Push actual requests into Room Database so users can see it instantly!
                            val vol = hospitalUnitsRequired.toIntOrNull() ?: 2
                            viewModel.submitBloodRequest(
                                bloodGroup = hospitalBloodGroup,
                                unitsRequired = vol,
                                hospitalName = activeHospital.name,
                                patientName = hospitalPatientName,
                                urgency = hospitalUrgencyLevel,
                                contactName = "Dr. S. K. Vasudevan (Emergency Node)",
                                contactPhone = "+91 9443210984",
                                requiredDate = hospitalRequiredDate,
                                specialInstructions = hospitalSpecialInstructions
                            )

                            // Populate real-time logger
                            activeBroadcastJobNum = "SOS-JOB-${(1000..9999).random()}"
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            
                            hospitalLogs.clear()
                            hospitalLogs.add("[$time] 📡 INITIALIZING EMERGENCY PROTOCOL $activeBroadcastJobNum...")
                            hospitalLogs.add("[$time] ✔ Authentication secured via license node ${activeHospital.licenseNumber}")
                            hospitalLogs.add("[$time] 🛰 Mapping area coverage limits: zone='$hospitalCoverageZone', range=${hospitalRadius}km")
                            hospitalLogs.add("[$time] 🔍 Located ${matchedNearbyDonors.size} healthy compatible target donors online...")
                            
                            if (matchedNearbyDonors.isNotEmpty()) {
                                hospitalLogs.add("[$time] ✉ SMS alerts drafted: 'URGENT: ${activeHospital.name} requires $hospitalBloodGroup blood immediately.'")
                                hospitalLogs.add("[$time] ⚡ SATELLITE DISPATCH: Push-Broadcasting wireless packet payload to ${matchedNearbyDonors.size} devices.")
                                hospitalLogs.add("[$time] 🟢 Cell network acknowledge: 100% telemetry resolution confirmed.")
                            } else {
                                hospitalLogs.add("[$time] ⚠️ No nearby sector matches. Cascaded request safely to public volunteer emergency logs as fail-safe.")
                            }
                            
                            showBroadcastSuccessToast = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trigger_hospital_sos_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "⚡ ACTIVATE WIRELESS SOS BROADCAST",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // 5. Visual Terminal for Simulated Real-Time Broadcast Telemetry Logs
                if (hospitalLogs.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("hospital_terminal_logs_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📡 TELEMETRY COMMAND LOGGER: $activeBroadcastJobNum",
                                        color = Color(0xFF047857),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(color = Color(0xFFDEF7EC), shape = RoundedCornerShape(4.dp)) {
                                        Text("LIVE STREAM", color = Color(0xFF03543F), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                hospitalLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        color = Color(0xFF1F2937),
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Hospital Requisition History & Feedback Section
                item {
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)
                    Text(
                        text = "📋 Requisition Collaboration History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(bottom = 4.dp).testTag("hospital_history_title")
                    )
                    Text(
                        text = "Track status of requests submitted by ${activeHospital.name} and provide responsiveness reviews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightSlate,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                val hospitalRequests = requests.filter { it.hospitalName.trim().lowercase() == activeHospital.name.trim().lowercase() }

                if (hospitalRequests.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("hospital_empty_history_card"),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Empty History",
                                    tint = LightSlate,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Requisitions Logged",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "This affiliate node has no past broadcast entries in the Room registry.",
                                    color = LightSlate,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    items(hospitalRequests) { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("hospital_history_card_${req.id}"),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            border = BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Request Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Patient: ${req.patientName}",
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Requisition ID: req-#${req.id}",
                                            fontSize = 11.sp,
                                            color = LightSlate
                                        )
                                    }
                                    // Urgency and overall state
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = if (req.urgencyLevel == "Critical") BloodCrimson else if (req.urgencyLevel == "High") PaavaiGold else InfoBlue,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = req.urgencyLevel.uppercase(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = if (req.isFulfilled) SuccessGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (req.isFulfilled) "FULFILLED" else "PENDING",
                                                color = if (req.isFulfilled) SuccessGreen else LightSlate,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                                // Details (Group, Units, Date, Instructions)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Blood Group Needed", fontSize = 10.sp, color = LightSlate)
                                        Text(req.bloodGroup, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BloodCrimson)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Units Required", fontSize = 10.sp, color = LightSlate)
                                        Text("${req.unitsRequired} Units", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text("Required By Date", fontSize = 10.sp, color = LightSlate)
                                        Text(req.requiredDate.ifEmpty { "Not specified" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                }

                                if (req.specialInstructions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Special Instructions:", fontSize = 10.sp, color = LightSlate, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = req.specialInstructions,
                                        fontSize = 11.sp,
                                        color = TextDark,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Real-Time Status Tracker Stepper
                                Text(
                                    text = "🔄 Track Collaboration Status:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val statusOptions = listOf("Requested", "Donor Found", "Blood Collected", "Delivered")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    statusOptions.forEach { statusOpt ->
                                        val isCurrent = req.status == statusOpt
                                        val isPassed = statusOptions.indexOf(statusOpt) < statusOptions.indexOf(req.status)
                                        val bg = if (isCurrent) BloodCrimson else if (isPassed) SuccessGreen.copy(alpha = 0.2f) else WarmSlate
                                        val tc = if (isCurrent) Color.White else if (isPassed) SuccessGreen else TextDark
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(bg)
                                                .clickable {
                                                    viewModel.updateRequestStatus(req.id, statusOpt)
                                                }
                                                .padding(vertical = 6.dp)
                                                .testTag("status_chip_${req.id}_$statusOpt"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = statusOpt,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tc
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Feedback & Ratings System Card Block
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = WarmSlate),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (req.donorRating > 0 || req.platformRating > 0) {
                                            Text("⭐ Hospital Feedback Review Submitted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaavaiGold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Column {
                                                    Text("Donor Responsiveness", fontSize = 9.sp, color = LightSlate)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Star, contentDescription = null, tint = PaavaiGold, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("${req.donorRating}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    }
                                                }
                                                Column {
                                                    Text("Platform Speed/Aid", fontSize = 9.sp, color = LightSlate)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Star, contentDescription = null, tint = PaavaiGold, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("${req.platformRating}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    }
                                                }
                                            }
                                            if (req.feedbackComment.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Comments: \"${req.feedbackComment}\"", fontSize = 11.sp, color = TextDark)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1.5f)) {
                                                    Text("Rate platform & donor speed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    Text("Help us optimize rapid Paavai mobilizations.", fontSize = 9.sp, color = LightSlate)
                                                }
                                                Button(
                                                    onClick = { feedbackTargetRequest = req },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f).testTag("hospital_submit_feedback_trigger_${req.id}")
                                                ) {
                                                    Text("Rate Node", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Success notification overlay as a prompt
    if (showBroadcastSuccessToast) {
        Dialog(onDismissRequest = { showBroadcastSuccessToast = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("hospital_broadcast_success_dialog"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, SuccessGreen)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.TapAndPlay, contentDescription = "Succeed", tint = SuccessGreen, modifier = Modifier.size(32.dp))
                    }
                    
                    Text(
                        text = "SOS Broadcast Casted!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )

                    Text(
                        text = "Emergency blood demand for $hospitalBloodGroup from ${hospitalAffiliates[selectedHospitalIndex].name} has been broadcasted. A total of ${matchedNearbyDonors.size} compatible nearby donors have been alerted in real-time.",
                        fontSize = 12.sp,
                        color = LightSlate,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Button(
                        onClick = { showBroadcastSuccessToast = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Acknowledge Node Confirmation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // New Request Dialog Form
    if (showRequestDialog) {
        var bloodGroup by remember { mutableStateOf("O-") }
        var unitsRequired by remember { mutableStateOf("1") }
        var patientName by remember { mutableStateOf("") }
        var hospitalName by remember { mutableStateOf("Paavai Multi Speciality Hospital, Namakkal") }
        var urgencyLevel by remember { mutableStateOf("Normal") }
        var contactName by remember { mutableStateOf(profile?.name ?: "") }
        var contactPhone by remember { mutableStateOf(profile?.mobileNumber ?: "") }
        var errorText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showRequestDialog = false }) {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Post Emergency Blood Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Enter patient specific, verified clinical details to find matches.",
                        fontSize = 11.sp,
                        color = LightSlate
                    )

                    Divider()

                    // Dropdown simulated for blood groups
                    Text("Blood Group:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+").forEach { bg ->
                            val active = bg == bloodGroup
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) DeepMaroon else WarmSlate)
                                        .clickable { bloodGroup = bg }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else TextDark
                                    )
                                }
                            }
                        }

                        // Urgency dropdown
                        Text("Urgency Priority:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Normal", "High", "Critical").forEach { urg ->
                                val active = urg == urgencyLevel
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (active) {
                                                when (urg) {
                                                    "Critical" -> BloodCrimson
                                                    "High" -> PaavaiGold
                                                    else -> InfoBlue
                                                }
                                            } else WarmSlate
                                        )
                                        .clickable { urgencyLevel = urg }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = urg,
                                        fontSize = 11.sp,
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
                            modifier = Modifier.fillMaxWidth().testTag("req_patient_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = hospitalName,
                            onValueChange = { hospitalName = it },
                            label = { Text("Hospital Name (Namakkal/Salem district)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = unitsRequired,
                                onValueChange = { unitsRequired = it },
                                label = { Text("Units Required") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = { contactName = it },
                                label = { Text("Contact Name") },
                                modifier = Modifier.weight(2f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Contact Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (errorText.isNotEmpty()) {
                            Text(text = errorText, color = BloodCrimson, fontSize = 11.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showRequestDialog = false }) {
                                Text("Cancel", color = LightSlate)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (patientName.trim().isEmpty() || contactPhone.trim().isEmpty() || contactName.trim().isEmpty()) {
                                        errorText = "Please fill in patient, contact and phone number."
                                        return@Button
                                    }
                                    val u = unitsRequired.toIntOrNull() ?: 1
                                    viewModel.submitBloodRequest(
                                        bloodGroup = bloodGroup,
                                        unitsRequired = u,
                                        hospitalName = hospitalName,
                                        patientName = patientName,
                                        urgency = urgencyLevel,
                                        contactName = contactName,
                                        contactPhone = contactPhone
                                    )
                                    showRequestDialog = false
                                },
                                modifier = Modifier.testTag("submit_request_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson)
                            ) {
                                Text("Post Request")
                            }
                        }
                    }
                }
            }
        }

    // Fulfill Request matching selection dialog
    selectedFulfillRequest?.let { req ->
        var selectedDonorNumber by remember { mutableStateOf("22104085") } // Default Saran Ramesh

        Dialog(onDismissRequest = { selectedFulfillRequest = null }) {
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
                    Text(
                        text = "Select Fulfilling Donor Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Record which student donor is completing this clinical request. This updates leaderboard and logs units.",
                        fontSize = 11.sp,
                        color = LightSlate
                    )

                    Divider()

                    // Horizontal simple dropdown
                    Text("Eligible Matching Donors:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    
                    val possibleDonors = donors.filter { it.bloodGroup == req.bloodGroup }
                    if (possibleDonors.isEmpty()) {
                        Text("No registered donors found with matching blood group in cache. Fulfilling as an anonymous external hero.", fontSize = 12.sp, color = LightSlate)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        val currentSelection = possibleDonors.find { it.registerNumber == selectedDonorNumber } ?: possibleDonors.first()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarmSlate, RoundedCornerShape(8.dp))
                                .clickable { expanded = !expanded }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${currentSelection.name} (${currentSelection.registerNumber})", fontSize = 12.sp, color = TextDark)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                possibleDonors.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.name} (${d.registerNumber})", fontSize = 12.sp) },
                                        onClick = {
                                            selectedDonorNumber = d.registerNumber
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedFulfillRequest = null }) {
                            Text("Cancel", color = LightSlate)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.fulfillRequest(
                                    requestId = req.id,
                                    donorRegNumber = if(possibleDonors.isNotEmpty()) selectedDonorNumber else null
                                )
                                selectedFulfillRequest = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Confirm Fulfillment")
                        }
                    }
                }
            }
        }
    }

    // Hospital feedback submission dialog
    feedbackTargetRequest?.let { req ->
        var donorRatingState by remember { mutableStateOf(5) }
        var platformRatingState by remember { mutableStateOf(5) }
        var commentState by remember { mutableStateOf("Excellent collaboration! The donor arrived very fast.") }

        Dialog(onDismissRequest = { feedbackTargetRequest = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("hospital_feedback_dialog"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(DarkCharcoal)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "⭐ Rate Requisition Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Submit responsiveness ratings for Requisition req-#${req.id} (Patient: ${req.patientName}).",
                        fontSize = 11.sp,
                        color = LightSlate
                    )

                    Divider()

                    // Donor Responsiveness Stars
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Donor Responsiveness & Speed:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { star ->
                                val active = star <= donorRatingState
                                Icon(
                                    imageVector = if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Donor Rating $star Star",
                                    tint = if (active) PaavaiGold else Color.Gray,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { donorRatingState = star }
                                        .testTag("donor_star_${star}")
                                )
                            }
                        }
                    }

                    // Platform Responsiveness Stars
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Platform Speed & Ease of Use:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { star ->
                                val active = star <= platformRatingState
                                Icon(
                                    imageVector = if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Platform Rating $star Star",
                                    tint = if (active) PaavaiGold else Color.Gray,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { platformRatingState = star }
                                        .testTag("platform_star_${star}")
                                )
                            }
                        }
                    }

                    // Comments input
                    OutlinedTextField(
                        value = commentState,
                        onValueChange = { commentState = it },
                        label = { Text("Written Comments (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("feedback_comments_input"),
                        singleLine = false,
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { feedbackTargetRequest = null }) {
                            Text("Cancel", color = LightSlate)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.submitRequestFeedback(
                                    requestId = req.id,
                                    donorRating = donorRatingState,
                                    platformRating = platformRatingState,
                                    feedbackComment = commentState
                                )
                                feedbackTargetRequest = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                            modifier = Modifier.testTag("submit_feedback_confirm_btn")
                        ) {
                            Text("Submit Review")
                        }
                    }
                }
            }
        }
    }
}

// Auxiliary holding class to maintain clean structure
data class HospitalPartnerInfo(
    val name: String,
    val licenseNumber: String,
    val region: String
)
