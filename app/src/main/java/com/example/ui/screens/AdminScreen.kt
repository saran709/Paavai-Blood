package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BloodRequest
import com.example.data.DonationCamp
import com.example.data.Donor
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: BloodConnectViewModel) {
    val donors by viewModel.allDonors.collectAsState()
    val requests by viewModel.allRequests.collectAsState()
    val camps by viewModel.allCamps.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val predictionReport by viewModel.predictionReport.collectAsState()
    val histories by viewModel.allHistory.collectAsState(initial = emptyList())

    var showExportDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Registry", "Camps", "Requests")

    // State for Donor Dialog
    var showDonorDialog by remember { mutableStateOf(false) }
    var selectedDonor by remember { mutableStateOf<Donor?>(null) } // null means Create, non-null means Update

    // State for Camp Dialog
    var showCampDialog by remember { mutableStateOf(false) }
    var selectedCamp by remember { mutableStateOf<DonationCamp?>(null) }

    // State for Request Dialog
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<BloodRequest?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Admin Hub",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepMaroon
                        )
                        Text(
                            text = "Paavai BloodConnect System Controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightSlate
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showExportDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = DarkCharcoal, contentColor = DeepMaroon),
                            modifier = Modifier.size(42.dp).testTag("admin_csv_export_center_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV", tint = DeepMaroon)
                        }

                        IconButton(
                            onClick = {
                                when (selectedTab) {
                                    0 -> {
                                        selectedDonor = null
                                        showDonorDialog = true
                                    }
                                    1 -> {
                                        selectedCamp = null
                                        showCampDialog = true
                                    }
                                    2 -> {
                                        selectedRequest = null
                                        showRequestDialog = true
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = BloodCrimson, contentColor = Color.White),
                            modifier = Modifier.size(42.dp).testTag("admin_quick_add_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = DeepMaroon
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = DeepMaroon,
                            unselectedContentColor = LightSlate
                        )
                    }
                }
            }
        },
        containerColor = WarmSlate
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DonorsTabContent(
                    donors = donors,
                    viewModel = viewModel,
                    isAiLoading = isAiLoading,
                    predictionReport = predictionReport,
                    onEditDonor = {
                        selectedDonor = it
                        showDonorDialog = true
                    },
                    onDeleteDonor = { viewModel.deleteDonor(it) }
                )
                1 -> CampsTabContent(
                    camps = camps,
                    onEditCamp = {
                        selectedCamp = it
                        showCampDialog = true
                    },
                    onDeleteCamp = { viewModel.deleteCampAdmin(it) }
                )
                2 -> RequestsTabContent(
                    requests = requests,
                    onEditRequest = {
                        selectedRequest = it
                        showRequestDialog = true
                    },
                    onDeleteRequest = { id -> viewModel.deleteRequestAdmin(id) },
                    onFulfillRequest = { id -> viewModel.fulfillRequest(id) }
                )
            }
        }
    }

    // Donor Dialog (Create & Update)
    if (showDonorDialog) {
        DonorFormDialog(
            donor = selectedDonor,
            onDismiss = { showDonorDialog = false },
            onSave = { name, regNo, dept, year, bg, phone, email, loc, weight, lastDonDate, userType, avail, totalD ->
                if (selectedDonor == null) {
                    viewModel.insertDonorAdmin(
                        Donor(
                            name = name,
                            registerNumber = regNo,
                            department = dept,
                            year = year,
                            bloodGroup = bg,
                            mobileNumber = phone,
                            email = email,
                            location = loc,
                            weight = weight,
                            lastDonationDate = lastDonDate,
                            userType = userType,
                            availability = avail,
                            totalDonations = totalD
                        )
                    )
                } else {
                    viewModel.updateDonorDetails(
                        selectedDonor!!.copy(
                            name = name,
                            registerNumber = regNo,
                            department = dept,
                            year = year,
                            bloodGroup = bg,
                            mobileNumber = phone,
                            email = email,
                            location = loc,
                            weight = weight,
                            lastDonationDate = lastDonDate,
                            userType = userType,
                            availability = avail,
                            totalDonations = totalD
                        )
                    )
                }
                showDonorDialog = false
            }
        )
    }

    // Camp Dialog (Create & Update)
    if (showCampDialog) {
        CampFormDialog(
            camp = selectedCamp,
            onDismiss = { showCampDialog = false },
            onSave = { title, date, time, venue, desc, regCount ->
                if (selectedCamp == null) {
                    viewModel.insertCampAdmin(
                        DonationCamp(
                            title = title,
                            date = date,
                            time = time,
                            venue = venue,
                            Description = desc,
                            registeredCount = regCount
                        )
                    )
                } else {
                    viewModel.updateCampAdmin(
                        selectedCamp!!.copy(
                            title = title,
                            date = date,
                            time = time,
                            venue = venue,
                            Description = desc,
                            registeredCount = regCount
                        )
                    )
                }
                showCampDialog = false
            }
        )
    }

    // Request Dialog (Create & Update)
    if (showRequestDialog) {
        RequestFormDialog(
            request = selectedRequest,
            onDismiss = { showRequestDialog = false },
            onSave = { bg, units, hosp, patient, urg, contact, phone, fulfilled ->
                if (selectedRequest == null) {
                    viewModel.insertRequestAdmin(
                        BloodRequest(
                            bloodGroup = bg,
                            unitsRequired = units,
                            hospitalName = hosp,
                            patientName = patient,
                            urgencyLevel = urg,
                            contactName = contact,
                            contactNumber = phone,
                            isFulfilled = fulfilled
                        )
                    )
                } else {
                    viewModel.updateRequestAdmin(
                        selectedRequest!!.copy(
                            bloodGroup = bg,
                            unitsRequired = units,
                            hospitalName = hosp,
                            patientName = patient,
                            urgencyLevel = urg,
                            contactName = contact,
                            contactNumber = phone,
                            isFulfilled = fulfilled
                        )
                    )
                }
                showRequestDialog = false
            }
        )
    }

    if (showExportDialog) {
        CSVExportCenterDialog(
            histories = histories,
            requests = requests,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun DonorsTabContent(
    donors: List<Donor>,
    viewModel: BloodConnectViewModel,
    isAiLoading: Boolean,
    predictionReport: String?,
    onEditDonor: (Donor) -> Unit,
    onDeleteDonor: (Donor) -> Unit
) {
    val totalCount = donors.size
    val requests by viewModel.allRequests.collectAsState()
    val camps by viewModel.allCamps.collectAsState()
    val activeCount = requests.count { !it.isFulfilled }
    val campsCount = camps.size

    val bloodDistribution = donors.groupBy { it.bloodGroup }.mapValues { it.value.size }
    val bloodGroupsMax = bloodDistribution.values.maxOrNull() ?: 1

    // Search and Filtering states for Admin Dashboard
    var searchQuery by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("All") }
    var selectedDepartment by remember { mutableStateOf("All") }
    var selectedLocation by remember { mutableStateOf("All") }

    val departmentsList = remember(donors) {
        listOf("All") + donors.map { it.department.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    
    val locationsList = remember(donors) {
        listOf("All") + donors.map { it.location.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    
    val bloodGroupsList = listOf("All", "O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")

    val filteredDonors = remember(donors, searchQuery, selectedBloodGroup, selectedDepartment, selectedLocation) {
        donors.filter { donor ->
            val matchQuery = searchQuery.isEmpty() ||
                donor.name.contains(searchQuery, ignoreCase = true) ||
                donor.registerNumber.contains(searchQuery, ignoreCase = true) ||
                donor.department.contains(searchQuery, ignoreCase = true) ||
                donor.location.contains(searchQuery, ignoreCase = true)
            
            val matchBg = selectedBloodGroup == "All" || donor.bloodGroup.trim().equals(selectedBloodGroup.trim(), ignoreCase = true)
            val matchDept = selectedDepartment == "All" || donor.department.trim().equals(selectedDepartment.trim(), ignoreCase = true)
            val matchLoc = selectedLocation == "All" || donor.location.trim().equals(selectedLocation.trim(), ignoreCase = true)
            
            matchQuery && matchBg && matchDept && matchLoc
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Summary Blocks Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatBlock(
                    modifier = Modifier.weight(1f),
                    title = "Verified Donors",
                    count = "$totalCount",
                    sub = "Students on file",
                    color = DeepMaroon,
                    icon = Icons.Default.VerifiedUser
                )
                AdminStatBlock(
                    modifier = Modifier.weight(1f),
                    title = "Pending Fulfills",
                    count = "$activeCount",
                    sub = "Active requirements",
                    color = BloodCrimson,
                    icon = Icons.Default.PendingActions
                )
                AdminStatBlock(
                    modifier = Modifier.weight(1f),
                    title = "Campus Drives",
                    count = "$campsCount",
                    sub = "Scheduled camps",
                    color = PaavaiGold,
                    icon = Icons.Default.Campaign
                )
            }
        }

        // Beautiful Graphical Chart: Blood Stock Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Chart",
                                tint = DeepMaroon,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Paavai Group Stock Index",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Surface(
                            color = RedLightBG,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "Live counts",
                                color = DeepMaroon,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (donors.isEmpty()) {
                        Text("No data to display in chart.", color = LightSlate, fontSize = 12.sp)
                    } else {
                        val groups = listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
                        groups.forEach { bg ->
                            val count = bloodDistribution[bg] ?: 0
                            val barPercentage = if (bloodGroupsMax > 0) count.toFloat() / bloodGroupsMax else 0f
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = bg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.width(36.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(WarmSlate)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(barPercentage.coerceAtLeast(0.04f))
                                            .background(
                                                if (bg == "O-") BloodCrimson else DeepMaroon
                                            )
                                    )
                                }

                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark,
                                    modifier = Modifier.width(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Prediction Module Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE9D5FF))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF3E8FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI PREDICT",
                                    tint = Color(0xFF7E22CE),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI Prediction Module",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Predict Demand Scarcity & Rare Shortages",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7E22CE),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analyzes vacation semester breaks, upcoming operations, local road incidents, and registered blood group volumes to forecast future shortages.",
                        fontSize = 12.sp,
                        color = LightSlate,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.performAiPrediction() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_compile_prediction_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = "run prediction")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate AI 30-Day Scarcity Forecast", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (isAiLoading || predictionReport != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = CardBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAiLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color(0xFF7E22CE))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Gemini parsing analytical models...", color = Color(0xFF7E22CE), fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = predictionReport ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Manage Student Registries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Search and Filter utility card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_donor_search_filter_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_donor_search_input"),
                        placeholder = { Text("Search by name or register number...", fontSize = 12.sp, color = LightSlate) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = LightSlate,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Search",
                                        tint = LightSlate,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepMaroon,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedLabelColor = DeepMaroon,
                            unfocusedLabelColor = LightSlate,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )

                    // Drops layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Blood group dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            var bgExpanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { bgExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("filter_blood_group_btn"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedBloodGroup != "All") DeepMaroon else CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedBloodGroup != "All") RedLightBG else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedBloodGroup == "All") "Blood: All" else "Blood: $selectedBloodGroup",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBloodGroup != "All") DeepMaroon else TextDark,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = if (bgExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (selectedBloodGroup != "All") DeepMaroon else LightSlate,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = bgExpanded,
                                onDismissRequest = { bgExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                bloodGroupsList.forEach { bg ->
                                    DropdownMenuItem(
                                        text = { Text(bg, fontSize = 12.sp, color = if (selectedBloodGroup == bg) DeepMaroon else TextDark, fontWeight = if (selectedBloodGroup == bg) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedBloodGroup = bg
                                            bgExpanded = false
                                        },
                                        modifier = Modifier.testTag("filter_blood_group_item_$bg")
                                    )
                                }
                            }
                        }

                        // Dept dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            var deptExpanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { deptExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("filter_department_btn"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedDepartment != "All") DeepMaroon else CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedDepartment != "All") RedLightBG else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedDepartment == "All") "Dept: All" else "Dept: $selectedDepartment",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDepartment != "All") DeepMaroon else TextDark,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = if (deptExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (selectedDepartment != "All") DeepMaroon else LightSlate,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = deptExpanded,
                                onDismissRequest = { deptExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                departmentsList.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept, fontSize = 12.sp, color = if (selectedDepartment == dept) DeepMaroon else TextDark, fontWeight = if (selectedDepartment == dept) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedDepartment = dept
                                            deptExpanded = false
                                        },
                                        modifier = Modifier.testTag("filter_department_item_$dept")
                                    )
                                }
                            }
                        }

                        // Location dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            var locExpanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { locExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("filter_location_btn"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedLocation != "All") DeepMaroon else CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedLocation != "All") RedLightBG else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedLocation == "All") "Loc: All" else "Loc: $selectedLocation",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedLocation != "All") DeepMaroon else TextDark,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = if (locExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (selectedLocation != "All") DeepMaroon else LightSlate,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = locExpanded,
                                onDismissRequest = { locExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                locationsList.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc, fontSize = 12.sp, color = if (selectedLocation == loc) DeepMaroon else TextDark, fontWeight = if (selectedLocation == loc) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedLocation = loc
                                            locExpanded = false
                                        },
                                        modifier = Modifier.testTag("filter_location_item_$loc")
                                    )
                                }
                            }
                        }
                    }

                    // Reset and Info bar
                    if (searchQuery.isNotEmpty() || selectedBloodGroup != "All" || selectedDepartment != "All" || selectedLocation != "All") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtered down to ${filteredDonors.size} registries",
                                fontSize = 11.sp,
                                color = LightSlate,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedBloodGroup = "All"
                                    selectedDepartment = "All"
                                    selectedLocation = "All"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp).testTag("admin_reset_filters_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    modifier = Modifier.size(12.dp),
                                    tint = DeepMaroon
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Filters", color = DeepMaroon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (donors.isEmpty()) {
            item {
                Text("Registry empty", color = LightSlate, fontSize = 12.sp)
            }
        } else if (filteredDonors.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "No Results", tint = LightSlate, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching donors on file",
                            color = TextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Try searching with different options or clear all filters.",
                            color = LightSlate,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedBloodGroup = "All"
                                selectedDepartment = "All"
                                selectedLocation = "All"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Reset Search", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            items(filteredDonors) { donor ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = donor.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                Text(text = "${donor.department} • Year: ${donor.year}", fontSize = 11.sp, color = LightSlate)
                                Text(text = "Reg: ${donor.registerNumber} • Mob: ${donor.mobileNumber}", fontSize = 11.sp, color = LightSlate)
                                Text(text = "Loc: ${donor.location} • Weight: ${donor.weight}kg", fontSize = 11.sp, color = LightSlate)
                                Text(
                                    text = "Last Donated: ${donor.lastDonationDate.ifEmpty { "Never" }} (${donor.totalDonations} total)",
                                    fontSize = 11.sp,
                                    color = InfoBlue
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DeepMaroon, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = donor.bloodGroup,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = if (donor.weight >= 45) SuccessGreen.copy(alpha = 0.1f) else BloodCrimson.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if(donor.weight >= 45) "HEALTHY" else "UNDERWEIGHT",
                                        color = if(donor.weight >= 45) SuccessGreen else BloodCrimson,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CardBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onEditDonor(donor) },
                                modifier = Modifier.testTag("edit_donor_btn_${donor.registerNumber}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Specs")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onDeleteDonor(donor) },
                                colors = ButtonDefaults.textButtonColors(contentColor = BloodCrimson),
                                modifier = Modifier.testTag("delete_donor_btn_${donor.registerNumber}")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CampsTabContent(
    camps: List<DonationCamp>,
    onEditCamp: (DonationCamp) -> Unit,
    onDeleteCamp: (DonationCamp) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Manage Camp Drives",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        if (camps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = LightSlate, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No scheduled campus blood camps.", color = LightSlate)
                    }
                }
            }
        } else {
            items(camps) { camp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = camp.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = PaavaiGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = camp.date, fontSize = 12.sp, color = LightSlate)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = PaavaiGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = camp.time, fontSize = 12.sp, color = LightSlate)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = PaavaiGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = camp.venue, fontSize = 12.sp, color = LightSlate)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = camp.Description, fontSize = 12.sp, color = TextDark)
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = PaavaiGold.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${camp.registeredCount} Volunteer commitments",
                                        color = PaavaiGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CardBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onEditCamp(camp) },
                                modifier = Modifier.testTag("edit_camp_btn_${camp.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Camp Details")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onDeleteCamp(camp) },
                                colors = ButtonDefaults.textButtonColors(contentColor = BloodCrimson),
                                modifier = Modifier.testTag("delete_camp_btn_${camp.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsTabContent(
    requests: List<BloodRequest>,
    onEditRequest: (BloodRequest) -> Unit,
    onDeleteRequest: (Int) -> Unit,
    onFulfillRequest: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Emergency Requests Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        if (requests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, tint = LightSlate, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active requests.", color = LightSlate)
                    }
                }
            }
        } else {
            items(requests) { request ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = request.patientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextDark
                                    )
                                    Surface(
                                        color = if (request.isFulfilled) SuccessGreen.copy(alpha = 0.1f) else BloodCrimson.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (request.isFulfilled) "FULFILLED" else "PENDING",
                                            color = if (request.isFulfilled) SuccessGreen else BloodCrimson,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Hosp: ${request.hospitalName}", fontSize = 12.sp, color = LightSlate)
                                Text(text = "Required Units: ${request.unitsRequired} Units", fontSize = 12.sp, color = LightSlate)
                                Text(text = "Priority Level: ${request.urgencyLevel}", fontSize = 12.sp, color = if (request.urgencyLevel == "Critical") BloodCrimson else PaavaiGold)
                                Text(text = "Contact: ${request.contactName} (${request.contactNumber})", fontSize = 12.sp, color = LightSlate)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (request.isFulfilled) SuccessGreen else BloodCrimson, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = request.bloodGroup,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CardBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!request.isFulfilled) {
                                TextButton(
                                    onClick = { onFulfillRequest(request.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = SuccessGreen)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Fulfill", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fulfill Now")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            TextButton(
                                onClick = { onEditRequest(request) },
                                modifier = Modifier.testTag("edit_request_btn_${request.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Parameters")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onDeleteRequest(request.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = BloodCrimson),
                                modifier = Modifier.testTag("delete_request_btn_${request.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatBlock(
    modifier: Modifier,
    title: String,
    count: String,
    sub: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier.height(120.dp),
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
                Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LightSlate, maxLines = 1)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }

            Column {
                Text(
                    text = count,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = sub,
                    fontSize = 8.sp,
                    color = LightSlate,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorFormDialog(
    donor: Donor?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        regNo: String,
        dept: String,
        year: String,
        bloodGroup: String,
        phone: String,
        email: String,
        location: String,
        weight: Double,
        lastDonationDate: String,
        userType: String,
        availability: Boolean,
        totalDonations: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(donor?.name ?: "") }
    var regNo by remember { mutableStateOf(donor?.registerNumber ?: "") }
    var dept by remember { mutableStateOf(donor?.department ?: "BE CSE") }
    var year by remember { mutableStateOf(donor?.year ?: "3rd Year") }
    var bloodGroup by remember { mutableStateOf(donor?.bloodGroup ?: "O-") }
    var phone by remember { mutableStateOf(donor?.mobileNumber ?: "") }
    var email by remember { mutableStateOf(donor?.email ?: "") }
    var location by remember { mutableStateOf(donor?.location ?: "Paavai Engineering Campus") }
    var weightText by remember { mutableStateOf(donor?.weight?.toString() ?: "65") }
    var lastDonationDate by remember { mutableStateOf(donor?.lastDonationDate ?: "") }
    var userType by remember { mutableStateOf(donor?.userType ?: "Student") }
    var availability by remember { mutableStateOf(donor?.availability ?: true) }
    var totalDonationsText by remember { mutableStateOf(donor?.totalDonations?.toString() ?: "0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (donor == null) "Add Student Registry" else "Modify Student Specs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("donor_name_field")
                )

                OutlinedTextField(
                    value = regNo,
                    onValueChange = { regNo = it },
                    label = { Text("Register Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("donor_reg_field")
                )

                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text("Department") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("donor_dept_field")
                )

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Academic Year") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Campus/Location") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastDonationDate,
                    onValueChange = { lastDonationDate = it },
                    label = { Text("Last Donation Date (YYYY-MM-DD)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalDonationsText,
                    onValueChange = { totalDonationsText = it },
                    label = { Text("Total Lives Saved / Donations") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = availability,
                        onCheckedChange = { availability = it }
                    )
                    Text("Eligible / Available for Emergency Calls", color = TextDark, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LightSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: 60.0
                            val tD = totalDonationsText.toIntOrNull() ?: 0
                            onSave(name, regNo, dept, year, bloodGroup, phone, email, location, w, lastDonationDate, userType, availability, tD)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                        modifier = Modifier.testTag("save_donor_btn")
                    ) {
                        Text("Save Specs", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampFormDialog(
    camp: DonationCamp?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        date: String,
        time: String,
        venue: String,
        description: String,
        registeredCount: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(camp?.title ?: "") }
    var date by remember { mutableStateOf(camp?.date ?: "") }
    var time by remember { mutableStateOf(camp?.time ?: "09:00 AM - 04:00 PM") }
    var venue by remember { mutableStateOf(camp?.venue ?: "") }
    var description by remember { mutableStateOf(camp?.Description ?: "") }
    var registeredCountText by remember { mutableStateOf(camp?.registeredCount?.toString() ?: "0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (camp == null) "Schedule New Blood Camp" else "Update Drive Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Camp Theme/Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("camp_title_field")
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("camp_date_field")
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Scheduled Hours") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Center name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Drive Directives") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = registeredCountText,
                    onValueChange = { registeredCountText = it },
                    label = { Text("Commitment Count") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LightSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val count = registeredCountText.toIntOrNull() ?: 0
                            onSave(title, date, time, venue, description, count)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaavaiGold),
                        modifier = Modifier.testTag("save_camp_btn")
                    ) {
                        Text("Publish Drive", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFormDialog(
    request: BloodRequest?,
    onDismiss: () -> Unit,
    onSave: (
        bloodGroup: String,
        unitsRequired: Int,
        hospitalName: String,
        patientName: String,
        urgencyLevel: String,
        contactName: String,
        contactNumber: String,
        isFulfilled: Boolean
    ) -> Unit
) {
    var bloodGroup by remember { mutableStateOf(request?.bloodGroup ?: "O-") }
    var unitsRequiredText by remember { mutableStateOf(request?.unitsRequired?.toString() ?: "1") }
    var hospitalName by remember { mutableStateOf(request?.hospitalName ?: "") }
    var patientName by remember { mutableStateOf(request?.patientName ?: "") }
    var urgencyLevel by remember { mutableStateOf(request?.urgencyLevel ?: "Normal") }
    var contactName by remember { mutableStateOf(request?.contactName ?: "") }
    var contactNumber by remember { mutableStateOf(request?.contactNumber ?: "") }
    var isFulfilled by remember { mutableStateOf(request?.isFulfilled ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (request == null) "Deploy Emergency Call" else "Update Request Specs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Required Type") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("req_bg_field")
                )

                OutlinedTextField(
                    value = unitsRequiredText,
                    onValueChange = { unitsRequiredText = it },
                    label = { Text("Volume (Units)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth().testTag("req_patient_field")
                )

                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Hospital Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = urgencyLevel,
                    onValueChange = { urgencyLevel = it },
                    label = { Text("Priority Level (Critical, High, Normal)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Liaison Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Liaison Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedLabelColor = PaavaiGold, unfocusedLabelColor = LightSlate),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isFulfilled,
                        onCheckedChange = { isFulfilled = it }
                    )
                    Text("Mark emergency fulfilled / finished", color = TextDark, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LightSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val u = unitsRequiredText.toIntOrNull() ?: 1
                            onSave(bloodGroup, u, hospitalName, patientName, urgencyLevel, contactName, contactNumber, isFulfilled)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                        modifier = Modifier.testTag("save_req_btn")
                    ) {
                        Text("Deploy Request", color = Color.White)
                    }
                }
            }
        }
    }
}

// =========================================================================
// CSV ADMINISTRATIVE REPORTING UTILITIES & DIALOG
// =========================================================================

fun generateDonationRecordsCsv(histories: List<com.example.data.DonationHistory>): String {
    val sb = java.lang.StringBuilder()
    sb.append("ID,Donor Name,Register Number,Date,Blood Group,Units Donated,Hospital Name\n")
    histories.forEach { history ->
        val escapedName = history.donorName.replace("\"", "\"\"")
        val escapedHospital = history.hospitalName.replace("\"", "\"\"")
        sb.append("${history.id},\"$escapedName\",${history.donorRegisterNumber},${history.date},${history.bloodGroup},${history.unitsDonated},\"$escapedHospital\"\n")
    }
    return sb.toString()
}

fun generateEmergencyStatsCsv(requests: List<com.example.data.BloodRequest>): String {
    val sb = java.lang.StringBuilder()
    sb.append("ID,Patient Name,Blood Group,Units Required,Hospital,Urgency,Contact,Contact Number,Fulfilled,Timestamp\n")
    requests.forEach { req ->
        val escapedPatient = req.patientName.replace("\"", "\"\"")
        val escapedHospital = req.hospitalName.replace("\"", "\"\"")
        val escapedContact = req.contactName.replace("\"", "\"\"")
        val isFulfilledStr = if (req.isFulfilled) "Yes" else "No"
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(req.timestamp))
        sb.append("${req.id},\"$escapedPatient\",${req.bloodGroup},${req.unitsRequired},\"$escapedHospital\",${req.urgencyLevel},\"$escapedContact\",${req.contactNumber},$isFulfilledStr,\"$dateStr\"\n")
    }
    return sb.toString()
}

@Composable
fun CSVExportCenterDialog(
    histories: List<com.example.data.DonationHistory>,
    requests: List<com.example.data.BloodRequest>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedDataSet by remember { mutableStateOf(0) } // 0 = Donation History, 1 = Emergency Requests

    val activeCsvContent = remember(selectedDataSet, histories, requests) {
        if (selectedDataSet == 0) {
            generateDonationRecordsCsv(histories)
        } else {
            generateEmergencyStatsCsv(requests)
        }
    }

    val activeFilename = remember(selectedDataSet) {
        if (selectedDataSet == 0) "paavai_donation_history.csv" else "paavai_emergency_statistics.csv"
    }

    val previewRows = remember(activeCsvContent) {
        activeCsvContent.split("\n").take(8).filter { it.isNotBlank() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("csv_export_center_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(RedLightBG, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Reporting icon",
                            tint = DeepMaroon,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Admin Report Center",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark
                        )
                        Text(
                            text = "Download records & emergency stats as CSV",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                    }
                }

                Divider(color = CardBorder, thickness = 0.5.dp)

                Text(
                    text = "Select a dataset target below, review the tabular syntax preview, then trigger file-save pipelines or copy raw strings straight to clipboard.",
                    fontSize = 11.sp,
                    color = LightSlate,
                    lineHeight = 15.sp
                )

                // Tab Selector Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { selectedDataSet = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDataSet == 0) Color.White else Color.Transparent,
                            contentColor = if (selectedDataSet == 0) DeepMaroon else LightSlate
                        ),
                        elevation = null,
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("export_select_donations"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Donation Logs (${histories.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedDataSet == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { selectedDataSet = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDataSet == 1) Color.White else Color.Transparent,
                            contentColor = if (selectedDataSet == 1) DeepMaroon else LightSlate
                        ),
                        elevation = null,
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("export_select_emergencies"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Emergency Stats (${requests.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedDataSet == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Table Attributes Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    border = BorderStroke(0.5.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DATA TABLE METRICS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepMaroon,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Format", fontSize = 9.sp, color = LightSlate)
                                Text("RFC 4180 CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Column {
                                Text("Rows Count", fontSize = 9.sp, color = LightSlate)
                                Text(
                                    text = "${if (selectedDataSet == 0) histories.size else requests.size} records",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column {
                                Text("Filename", fontSize = 9.sp, color = LightSlate)
                                Text(activeFilename, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                    }
                }

                // Code Live Preview Window (The Slate Colored Terminal-style container)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live CSV Row-by-Row Syntax Sample",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightSlate
                        )
                        Text(
                            text = "First ${previewRows.size} lines shown",
                            fontSize = 9.sp,
                            color = LightSlate
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(previewRows) { rowString ->
                                Text(
                                    text = rowString,
                                    color = if (rowString.startsWith("ID")) InfoBlue else TextDark,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            if (previewRows.isEmpty()) {
                                item {
                                    Text(
                                        text = "[No records available to preview yet]",
                                        color = LightSlate,
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Buttons Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy RAW CSV
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(activeCsvContent))
                            android.widget.Toast.makeText(context, "$activeFilename text copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("csv_btn_copy"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DeepMaroon),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepMaroon)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save And Download/Share Csv File
                    Button(
                        onClick = {
                            try {
                                val file = java.io.File(context.cacheDir, activeFilename)
                                file.writeText(activeCsvContent)

                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )

                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Paavai BloodConnect Export: $activeFilename")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(android.content.Intent.createChooser(intent, "Share/Save Report CSV via"))
                                android.widget.Toast.makeText(context, "Exporting $activeFilename file...", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "File save error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("csv_btn_download"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save file", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Share File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("csv_btn_close"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = LightSlate)
                ) {
                    Text("Close Center", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
