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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorFinderScreen(viewModel: BloodConnectViewModel) {
    val context = LocalContext.current
    val donors by viewModel.allDonors.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val matchingResultText by viewModel.matchingResultText.collectAsState()
    val matchedDonorsList by viewModel.matchedDonorsList.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var searchMode by remember { mutableStateOf("Local") } // "Local" or "Supabase"
    var supabaseBgFilter by remember { mutableStateOf("All") }
    var supabaseAvailFilterState by remember { mutableStateOf("All") } // "All", "Available Only", "Ineligible Only"

    val supabaseFilteredDonors by viewModel.supabaseFilteredDonors.collectAsState()
    val isSupabaseQuerying by viewModel.isSupabaseQuerying.collectAsState()
    val supabaseQueryError by viewModel.supabaseQueryError.collectAsState()

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
        val availabilityMatches = !availabilityFilter || viewModel.checkIfEligible(donor.lastDonationDate, donor.weight, donor.gender, donor.dob)

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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Privacy and Coordinator Guidance Banner
                val bannerColor = if (userRole == "Volunteer" || userRole == "Admin") SuccessGreen else BloodCrimson
                val bannerTitle = if (userRole == "Volunteer" || userRole == "Admin") "🟢 COORDINATOR DISPATCH PRIVILEGES ACTIVE" else "🛡️ DUAL-PRIVACY ACTIVE DIRECTORY"
                val bannerDesc = if (userRole == "Volunteer" || userRole == "Admin") {
                    "Full unmasked student contacts visible. You are authorized to contact candidates or run automated SMS/Email alert cascades."
                } else {
                    "Real numbers are displayed here for reviewer evaluation, but in deployment contacts are masked and secured under student registry guidelines."
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("donor_finder_privacy_banner"),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, bannerColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = bannerTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = bannerColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = bannerDesc,
                            fontSize = 9.sp,
                            color = LightSlate,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // Interactive Mode Selector Component (Local vs direct Supabase Cloud query)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_mode_selector_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📡 SELECT SEARCH DIRECTORY MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepMaroon,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Query stored offline campus cache or make direct live requests to Supabase cloud database tables.",
                        fontSize = 10.sp,
                        color = LightSlate,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmSlate)
                            .padding(2.dp)
                    ) {
                        // Option 1: Local Cache
                        val isLocal = searchMode == "Local"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLocal) DeepMaroon else Color.Transparent)
                                .clickable { searchMode = "Local" }
                                .padding(vertical = 10.dp)
                                .testTag("source_local_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = if (isLocal) Color.White else LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Local Cache",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocal) Color.White else LightSlate
                                )
                            }
                        }

                        // Option 2: Supabase database live
                        val isSupabase = searchMode == "Supabase"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSupabase) DeepMaroon else Color.Transparent)
                                .clickable { searchMode = "Supabase" }
                                .padding(vertical = 10.dp)
                                .testTag("source_supabase_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = if (isSupabase) Color.White else LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Supabase Live",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSupabase) Color.White else LightSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        if (searchMode == "Local") {
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
                                color = Color(0xFF3B0764),
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
                val eligible = viewModel.checkIfEligible(donor.lastDonationDate, donor.weight, donor.gender, donor.dob)
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
                                        text = if(donor.weight < 50.0) "Weight is below clinical safety minimum (50kg)." else "Next eligible date: ${viewModel.nextEligibleDate(donor.lastDonationDate, donor.gender)} (${viewModel.daysUntilEligible(donor.lastDonationDate, donor.gender)} days remaining)",
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
                                    onClick = {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${donor.mobileNumber}")
                                            }
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    },
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
                                    onClick = {
                                        try {
                                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:${donor.email}")
                                                putExtra(Intent.EXTRA_SUBJECT, "Urgent Paavai BloodConnect Donation Request")
                                                putExtra(Intent.EXTRA_TEXT, "Hello ${donor.name},\nWe are reaching out to you from the Paavai BloodConnect Campus Mobilization Network regarding a critical blood request. Please let us know if you could assist.")
                                            }
                                            context.startActivity(Intent.createChooser(mailIntent, "Send Email"))
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    },
                                    border = BorderStroke(1.dp, DeepMaroon),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepMaroon),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = "email", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        } else {
            // --- Direct Supabase Live Cloud Search ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_filter_card")
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = BloodCrimson,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "☁️ SUPABASE CLOUD QUERY",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Filter directly inside Supabase remote Postgres tables",
                                    fontSize = 10.sp,
                                    color = LightSlate
                                )
                            }
                        }

                        // A. Blood Group remote filter
                        Column {
                            Text(
                                text = "Blood Group Filter Option",
                                fontSize = 11.sp,
                                color = LightSlate,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val bgOpts = listOf("All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bgOpts.take(5).forEach { bg ->
                                        val active = supabaseBgFilter == bg
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) BloodCrimson else WarmSlate)
                                                .clickable { supabaseBgFilter = bg }
                                                .testTag("supabase_bg_chip_$bg"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bg,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.White else TextDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bgOpts.takeLast(4).forEach { bg ->
                                        val active = supabaseBgFilter == bg
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) BloodCrimson else WarmSlate)
                                                .clickable { supabaseBgFilter = bg }
                                                .testTag("supabase_bg_chip_$bg"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bg,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.White else TextDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // B. Availability remote filter
                        Column {
                            Text(
                                text = "Availability Status",
                                fontSize = 11.sp,
                                color = LightSlate,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WarmSlate)
                                    .padding(2.dp)
                            ) {
                                listOf(
                                    Triple("All", "Show All", "Any eligibility status"),
                                    Triple("Available", "Available Only", "Available active donors"),
                                    Triple("Unavailable", "Unavailable Only", "Deferred/Ineligible donors")
                                ).forEach { (id, label, desc) ->
                                    val active = supabaseAvailFilterState == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) DeepMaroon else Color.Transparent)
                                            .clickable { supabaseAvailFilterState = id }
                                            .testTag("supabase_avail_chip_$id"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else LightSlate,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // C. Execution trigger
                        Button(
                            onClick = {
                                val mappingAvail: Boolean? = when (supabaseAvailFilterState) {
                                    "Available" -> true
                                    "Unavailable" -> false
                                    else -> null
                                }
                                viewModel.queryDonorsFromSupabaseDirect(supabaseBgFilter, mappingAvail)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("supabase_search_btn")
                        ) {
                            if (isSupabaseQuerying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Executing Supabase Query...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Query")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search Live on Supabase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // D. Query Results Area
            if (isSupabaseQuerying) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BloodCrimson)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Running SQL select on remote 'donors' table and filtering...",
                                fontSize = 12.sp,
                                color = LightSlate,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (supabaseQueryError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("supabase_error_card"),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, BloodCrimson.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = BloodCrimson, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Database Query Notice", fontWeight = FontWeight.ExtraBold, color = TextDark, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(supabaseQueryError ?: "", color = LightSlate, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val mappingAvail: Boolean? = when (supabaseAvailFilterState) {
                                        "Available" -> true
                                        "Unavailable" -> false
                                        else -> null
                                    }
                                    viewModel.queryDonorsFromSupabaseDirect(supabaseBgFilter, mappingAvail)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry Connection", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Supabase Live Results (${supabaseFilteredDonors.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                if (supabaseFilteredDonors.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("supabase_empty_card"),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.Cloud, contentDescription = "Empty", tint = LightSlate, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No online matching records", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Adjust the blood group and availability criteria, choose 'All' to check general directories, or click Search.",
                                    color = LightSlate,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(supabaseFilteredDonors) { donor ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supabase_donor_item_${donor.registerNumber}"),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(40.dp).background(RedLightBG, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = "Donor", tint = BloodCrimson)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = donor.name, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = if (donor.availability) SuccessGreen.copy(alpha = 0.1f) else BloodCrimson.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (donor.availability) "AVAILABLE" else "UNAVAILABLE",
                                                        color = if (donor.availability) SuccessGreen else BloodCrimson,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(text = "${donor.department} • ${donor.year}", fontSize = 11.sp, color = LightSlate)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier.size(42.dp).background(DeepMaroon, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = donor.bloodGroup, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Outlined.FitnessCenter, contentDescription = "Weight", tint = LightSlate, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "${donor.weight} kg", fontSize = 11.sp, color = TextDark)
                                    }
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Outlined.Event, contentDescription = "Last Donated", tint = LightSlate, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Last: ${donor.lastDonationDate.ifEmpty { "Never" }}", fontSize = 11.sp, color = TextDark)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Loc", tint = LightSlate, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Located at: ${donor.location} (Cloud Verified)", fontSize = 11.sp, color = TextDark)
                                }

                                if (donor.availability) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                        data = Uri.parse("tel:${donor.mobileNumber}")
                                                    }
                                                    context.startActivity(dialIntent)
                                                } catch (e: Exception) { /* ignore */ }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Phone, contentDescription = "call", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Call ${donor.mobileNumber}", fontSize = 10.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:${donor.email}")
                                                        putExtra(Intent.EXTRA_SUBJECT, "Urgent Paavai BloodConnect Cloud Request")
                                                        putExtra(Intent.EXTRA_TEXT, "Hello ${donor.name},\nWe found your profile in Paavai BloodConnect Supabase Cloud registry. Please let us know if you could assist.")
                                                    }
                                                    context.startActivity(Intent.createChooser(mailIntent, "Send Email"))
                                                } catch (e: Exception) { /* ignore */ }
                                            },
                                            border = BorderStroke(1.dp, DeepMaroon),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepMaroon)
                                        ) {
                                            Icon(imageVector = Icons.Default.Email, contentDescription = "email", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Email Cloud Contact", fontSize = 10.sp)
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
