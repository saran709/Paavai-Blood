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

    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedFulfillRequest by remember { mutableStateOf<BloodRequest?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency Requests",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                    Text(
                        text = "Create or fulfill blood requirements inside the college network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightSlate
                    )
                }
                
                if (userRole == "Volunteer" || userRole == "Admin") {
                    Button(
                        onClick = { showRequestDialog = true },
                        modifier = Modifier.testTag("create_request_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Text("Request", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

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
                text = "Recent Dispatch Logs",
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

                            // Matching actions
                            if (!request.isFulfilled) {
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
                                        Text("Fulfill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Fulfilled",
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
}
