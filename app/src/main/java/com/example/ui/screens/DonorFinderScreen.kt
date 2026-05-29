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
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Donor
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorFinderScreen(viewModel: BloodConnectViewModel) {
    val donors by viewModel.allDonors.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val matchingResultText by viewModel.matchingResultText.collectAsState()
    val matchedDonorsList by viewModel.matchedDonorsList.collectAsState()

    var searchBloodGroup by remember { mutableStateOf("O-") }
    var selectedDeptFilter by remember { mutableStateOf("All") }
    var selectedYearFilter by remember { mutableStateOf("All") }
    var availabilityFilter by remember { mutableStateOf(false) } // true to show only eligible available

    // Available filters
    val departments = listOf("All", "B.E. Computer Science", "MCA (Alumni)", "Bio-Technology", "B.Tech Information Tech", "B.E. Electronics & Comm", "B.Tech Artificial Intelligence")
    val years = listOf("All", "1st Year", "2nd Year", "3rd Year", "4th Year", "Faculty", "Alumni")

    // Filter local list based on user choices
    val filteredDonors = donors.filter { donor ->
        val groupMatches = donor.bloodGroup.uppercase() == searchBloodGroup.uppercase()
        val deptMatches = selectedDeptFilter == "All" || donor.department == selectedDeptFilter
        val yearMatches = selectedYearFilter == "All" || donor.year == selectedYearFilter
        val availabilityMatches = !availabilityFilter || viewModel.checkIfEligible(donor.lastDonationDate, donor.weight)

        groupMatches && deptMatches && yearMatches && availabilityMatches
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Quick Title
        item {
            Column {
                Text(
                    text = "Smart Donor Finder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
                Text(
                    text = "Locate compatible blood donors inside Paavai network instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate
                )
            }
        }

        // Blood group selections
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Select Blood Group Needed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bloodGroups.take(4).forEach { bg ->
                            val isSelected = bg == searchBloodGroup
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BloodCrimson else WarmSlate)
                                    .clickable { searchBloodGroup = bg }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bg,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextDark,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bloodGroups.takeLast(4).forEach { bg ->
                            val isSelected = bg == searchBloodGroup
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BloodCrimson else WarmSlate)
                                    .clickable { searchBloodGroup = bg }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bg,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextDark,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Advanced local filters
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "2. Refine Search Filters (Optional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    // Dept filter dropdown simulator (horizontal row of filters for convenience)
                    Column {
                        Text("Department:", fontSize = 11.sp, color = LightSlate, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var deptExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarmSlate, RoundedCornerShape(8.dp))
                                    .clickable { deptExpanded = !deptExpanded }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = selectedDeptFilter, fontSize = 12.sp, color = TextDark)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = deptExpanded,
                                    onDismissRequest = { deptExpanded = false }
                                ) {
                                    departments.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text(d, fontSize = 12.sp) },
                                            onClick = {
                                                selectedDeptFilter = d
                                                deptExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Year Filter selection
                    Column {
                        Text("Year / Role Type:", fontSize = 11.sp, color = LightSlate, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var yearExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarmSlate, RoundedCornerShape(8.dp))
                                    .clickable { yearExpanded = !yearExpanded }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = selectedYearFilter, fontSize = 12.sp, color = TextDark)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false }
                                ) {
                                    years.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y, fontSize = 12.sp) },
                                            onClick = {
                                                selectedYearFilter = y
                                                yearExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Eligibility checker filter toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Show Eligible Donors Only",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Filters out weights <45kg or donations within 90 days",
                                fontSize = 10.sp,
                                color = LightSlate
                            )
                        }
                        Switch(
                            checked = availabilityFilter,
                            onCheckedChange = { availabilityFilter = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BloodCrimson, checkedTrackColor = RedLightBG),
                            modifier = Modifier.testTag("eligibility_toggle")
                        )
                    }

                    // Trigger AI Matching button
                    Button(
                        onClick = {
                            // Construct a mock or real request parameter
                            val simulatedRequest = com.example.data.BloodRequest(
                                bloodGroup = searchBloodGroup,
                                unitsRequired = 1,
                                hospitalName = "Nearby Paavai Campus Clinic / Hospital",
                                patientName = "Routine Search Query",
                                urgencyLevel = "Normal",
                                contactName = "Searcher",
                                contactNumber = "+91 9191919191"
                            )
                            viewModel.performAiMatching(simulatedRequest)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_ai_match_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = "AI")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate AI Emergency Suitability & Match Rank")
                    }
                }
            }
        }

        // Live AI Evaluation Section
        if (isAiLoading || matchingResultText != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFC084FC).copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI MATCH",
                                tint = Color(0xFF9333EA)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Strategic Match Assessment",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF7E22CE),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAiLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color(0xFF9333EA))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Gemini is analyzing donor history and parameters...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        } else {
                            Text(
                                text = matchingResultText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        // Result donors list header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Matching Registry Candidates (${filteredDonors.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        }

        if (filteredDonors.isEmpty()) {
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
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "Empty",
                            tint = LightSlate,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No donors found",
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Try broadening your department or eligibility filters.",
                            fontSize = 12.sp,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredDonors) { donor ->
                val eligible = viewModel.checkIfEligible(donor.lastDonationDate, donor.weight)
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
                                // Red blood icon or envelope
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(RedLightBG, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Donor",
                                        tint = BloodCrimson
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = donor.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (eligible) SuccessGreen.copy(alpha = 0.1f) else BloodCrimson.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (eligible) "AVAILABLE" else "INELIGIBLE",
                                                color = if (eligible) SuccessGreen else BloodCrimson,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${donor.department} • ${donor.year}",
                                        fontSize = 11.sp,
                                        color = LightSlate
                                    )
                                }
                            }

                            // Big Blood Group circle
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(DeepMaroon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = donor.bloodGroup,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                        // Donor specifications grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FitnessCenter,
                                    contentDescription = "Weight",
                                    tint = LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${donor.weight} kg",
                                    fontSize = 12.sp,
                                    color = TextDark
                                )
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = "Last Donated",
                                    tint = LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Last: ${donor.lastDonationDate.ifEmpty { "Never" }}",
                                    fontSize = 11.sp,
                                    color = TextDark
                                )
                            }
                        }

                        // Physical Location Spec
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Loc",
                                tint = LightSlate,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Located at: ${donor.location}",
                                fontSize = 11.sp,
                                color = TextDark
                            )
                        }

                        // Next eligibility date warning if not eligible
                        if (!eligible) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = RedLightBG,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Ineligible Warning",
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if(donor.weight < 45.0) "Weight is below clinical safety minimum (45kg)." else "Next eligible date: ${viewModel.nextEligibleDate(donor.lastDonationDate)} (${viewModel.daysUntilEligible(donor.lastDonationDate)} days remaining)",
                                        color = BloodCrimson,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Contact Button Row
                        if (eligible) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { /* Simulated Call call */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = "call", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call ${donor.mobileNumber}", fontSize = 10.sp)
                                }
                                OutlinedButton(
                                    onClick = { /* Simulated SMS SMS */ },
                                    border = BorderStroke(1.dp, DeepMaroon),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepMaroon),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = "email", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Email Alerts", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
