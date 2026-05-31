package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: BloodConnectViewModel) {
    val donors by viewModel.allDonors.collectAsState()
    val requests by viewModel.allRequests.collectAsState()
    val camps by viewModel.allCamps.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val predictionReport by viewModel.predictionReport.collectAsState()

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E112C)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFFC084FC))
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
                                    .background(Color(0xFF581C87), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI PREDICT",
                                    tint = Color(0xFFE9D5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI Prediction Module",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFE9D5FF),
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Predict Demand Scarcity & Rare Shortages",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC084FC),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analyzes vacation semester breaks, upcoming operations, local road incidents, and registered blood group volumes to forecast future shortages.",
                        fontSize = 12.sp,
                        color = Color(0xFFE9D5FF),
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
                        Divider(color = Color(0xFFE9D5FF))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAiLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color(0xFFC084FC))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Gemini parsing analytical models...", color = Color(0xFFE9D5FF), fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = predictionReport ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF3E8FF),
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

        if (donors.isEmpty()) {
            item {
                Text("Registry empty", color = LightSlate, fontSize = 12.sp)
            }
        } else {
            items(donors) { donor ->
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
