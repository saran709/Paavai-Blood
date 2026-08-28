package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.Donor
import com.example.data.DonationHistory
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import java.util.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: BloodConnectViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val profile by viewModel.registeredProfile.collectAsState()
    val history by viewModel.allHistory.collectAsState()
    val dProfile = profile
    val scope = rememberCoroutineScope()

    if (userRole == "Admin") {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }
        var adminErrorText by remember { mutableStateOf("") }
        var adminSuccessText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmSlate)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin",
                tint = DeepMaroon,
                modifier = Modifier.size(64.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Admin Security Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Update the master authentication password for administrative control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CHANGE PASSWORD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = DeepMaroon
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { 
                            newPassword = it
                            adminErrorText = ""
                            adminSuccessText = ""
                        },
                        label = { Text("New Security Password") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            adminErrorText = ""
                            adminSuccessText = ""
                        },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_confirm_password_input"),
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        }
                    )

                    if (adminErrorText.isNotEmpty()) {
                        Text(
                            text = adminErrorText,
                            color = BloodCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (adminSuccessText.isNotEmpty()) {
                        Text(
                            text = adminSuccessText,
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (newPassword.isEmpty()) {
                                adminErrorText = "Password cannot be empty."
                                return@Button
                            }
                            if (newPassword.length < 6) {
                                adminErrorText = "Password must be at least 6 characters."
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                adminErrorText = "Passwords do not match."
                                return@Button
                            }

                            viewModel.changePassword(
                                email = currentUserEmail.ifEmpty { "admin@paavai.edu.in" },
                                newPass = newPassword,
                                onSuccess = {
                                    adminSuccessText = "Admin password updated successfully!"
                                    newPassword = ""
                                    confirmPassword = ""
                                    Toast.makeText(context, "Password Changed successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    adminErrorText = it
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                        modifier = Modifier.fillMaxWidth().testTag("admin_change_password_submit_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("UPDATE PASSWORD", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("profile_logout_btn"),
                border = BorderStroke(1.dp, DeepMaroon),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = DeepMaroon)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Logout Admin Session", color = DeepMaroon)
            }

            Spacer(modifier = Modifier.weight(1f))
        }
        return
    }

    var showRegisterForm by remember { mutableStateOf(dProfile == null) }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var isDownloadingPdf by remember { mutableStateOf(false) }

    val isCheckingAiEligibility by viewModel.isCheckingAiEligibility.collectAsState()
    val aiEligibilityResult by viewModel.aiEligibilityResult.collectAsState()

    val symptomOptions = listOf(
        "Recent tattoo or piercing (past 6 months)",
        "Undergoing antibiotic treatment",
        "Recent cold, fever, or flu (past 1 week)",
        "Low hemoglobin or history of anemia",
        "Severe sleep deprivation (past 24h)",
        "Active dental surgery or extraction"
    )
    val checkedSymptoms = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(Unit) {
        symptomOptions.forEach { if (!checkedSymptoms.containsKey(it)) checkedSymptoms[it] = false }
    }

    val userHistories = history.filter { it.donorRegisterNumber == dProfile?.registerNumber }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Welcome and intro
        item {
            Column {
                Text(
                    text = "QR Donor ID & Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
                Text(
                    text = "Obtain your verified digital card and manage your donor profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate
                )
            }
        }

        if (dProfile == null || showRegisterForm) {
            item {
                DonorRegistrationForm(
                    initialProfile = dProfile,
                    onCancel = if (dProfile != null) { { showRegisterForm = false } } else null,
                    onSubmit = { profile ->
                        viewModel.registerDonor(
                            name = profile.name,
                            regNo = profile.registerNumber,
                            dept = profile.department,
                            year = profile.year,
                            bloodGroup = profile.bloodGroup,
                            mobile = profile.mobileNumber,
                            email = profile.email,
                            location = profile.location,
                            weight = profile.weight,
                            lastDonation = profile.lastDonationDate,
                            userType = profile.userType,
                            gender = profile.gender,
                            dob = profile.dob,
                            address = profile.address,
                            emergencyContact = profile.emergencyContact,
                            profilePhoto = profile.profilePhoto,
                            onSuccess = {
                                showRegisterForm = false
                                Toast.makeText(context, "Donor Registration Successful!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        } else {
            // Already smartcast to non-null because dProfile == null is false!
            
            // Highlight: Digitized QR Identification Card with beautiful college branding
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(DeepMaroon, DarkCharcoal)
                                )
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge branding
                        Text(
                            text = "PAAVAI EMERGENCY DONOR CARD",
                            color = LightGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "PAAVAI INSTITUTIONS",
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = dProfile.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Id: ${dProfile.registerNumber}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${dProfile.department}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${dProfile.year} • ${dProfile.userType}",
                                    color = LightGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Big Blood badge
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, PaavaiGold, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dProfile.bloodGroup,
                                        color = BloodCrimson,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "GROUP",
                                        color = LightSlate,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CONTACT",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dProfile.mobileNumber,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "WEIGHT METRIC",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${dProfile.weight} KG",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 0. Quick Status & Availability Controller Card
            item {
                var quickPhone by remember { mutableStateOf(dProfile.mobileNumber) }
                var quickEmail by remember { mutableStateOf(dProfile.email) }
                var quickLocation by remember { mutableStateOf(dProfile.location) }

                // Synchronize when the overarching database profile changes
                LaunchedEffect(dProfile.mobileNumber, dProfile.email, dProfile.location) {
                    quickPhone = dProfile.mobileNumber
                    quickEmail = dProfile.email
                    quickLocation = dProfile.location
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_status_control_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Title / Header area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = BloodCrimson,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Quick Status & Contact Control",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            
                            // Visual indicator of current state
                            Surface(
                                color = if (dProfile.availability) SuccessGreen.copy(alpha = 0.15f) else BloodCrimson.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (dProfile.availability) SuccessGreen else BloodCrimson, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (dProfile.availability) "ACTIVE DONOR" else "PAUSED STATUS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (dProfile.availability) SuccessGreen else BloodCrimson
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Toggle your real-time directory listing state and refine contact details instantly.",
                            fontSize = 11.sp,
                            color = LightSlate,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                        // A. Interactive Availability Toggle Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmSlate.copy(alpha = 0.5f))
                                .clickable {
                                    val updatedDonor = dProfile.copy(availability = !dProfile.availability)
                                    viewModel.updateDonorDetails(updatedDonor)
                                    val statusMsg = if (updatedDonor.availability) "You are now active and searchable in directory!" else "You have paused donation search listings."
                                    Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("toggle_availability_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (dProfile.availability) Icons.Default.Campaign else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (dProfile.availability) SuccessGreen else LightSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Visible in Live Directory",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (dProfile.availability) "Campus patients can find and reach you" else "Hidden from directory queries",
                                        fontSize = 10.sp,
                                        color = LightSlate
                                    )
                                }
                            }
                            
                            Switch(
                                checked = dProfile.availability,
                                onCheckedChange = { isChecked ->
                                    val updatedDonor = dProfile.copy(availability = isChecked)
                                    viewModel.updateDonorDetails(updatedDonor)
                                    val statusMsg = if (isChecked) "You are now active and searchable in directory!" else "You have paused donation search listings."
                                    Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = LightSlate,
                                    uncheckedTrackColor = WarmSlate
                                ),
                                modifier = Modifier.testTag("quick_availability_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // B. Quick Contact Update Section Title
                        Text(
                            text = "QUICK CONTACT AND DETAILS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightGold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Mobile input field
                        OutlinedTextField(
                            value = quickPhone,
                            onValueChange = { quickPhone = it },
                            label = { Text("Quick Mobile Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_mobile_input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Email input field
                        OutlinedTextField(
                            value = quickEmail,
                            onValueChange = { quickEmail = it },
                            label = { Text("Quick Email ID") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_email_input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Location input field
                        OutlinedTextField(
                            value = quickLocation,
                            onValueChange = { quickLocation = it },
                            label = { Text("Quick Campus Location") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_location_input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger Button
                        Button(
                            onClick = {
                                if (quickPhone.trim().isEmpty() || quickEmail.trim().isEmpty() || quickLocation.trim().isEmpty()) {
                                    Toast.makeText(context, "Contact fields cannot be left empty.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val updatedDonor = dProfile.copy(
                                    mobileNumber = quickPhone.trim(),
                                    email = quickEmail.trim(),
                                    location = quickLocation.trim()
                                )
                                viewModel.updateDonorDetails(updatedDonor)
                                Toast.makeText(context, "Contact credentials updated and synced successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_quick_contact_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Live Contact Info", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. AI Eligibility Status Panel
            item {
                val eligible = viewModel.checkIfEligible(dProfile.lastDonationDate, dProfile.weight, dProfile.gender, dProfile.dob)
                val age = viewModel.calculateAge(dProfile.dob)
                val daysLeft = viewModel.daysUntilEligible(dProfile.lastDonationDate, dProfile.gender)
                val nextEligDate = viewModel.nextEligibleDate(dProfile.lastDonationDate, dProfile.gender)
                val percentage = viewModel.calculateEligibilityPercentage(dProfile.lastDonationDate, dProfile.weight, dProfile.gender, dProfile.dob)
                
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("eligibility_checker_panel"),
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
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = "Health",
                                    tint = if (eligible) SuccessGreen else BloodCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Eligibility Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            
                            Surface(
                                color = if (eligible) SuccessGreen.copy(alpha = 0.15f) else BloodCrimson.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (eligible) "ELIGIBLE NOW" else "NOT ELIGIBLE",
                                    color = if (eligible) SuccessGreen else BloodCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Eligibility metrics checklist progress
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Age Check
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Age Requirement (≥18)", fontSize = 12.sp, color = LightSlate)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$age Years Old", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (age >= 18) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = "",
                                        tint = if (age >= 18) SuccessGreen else BloodCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // Weight Check
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weight Requirement (≥50kg)", fontSize = 12.sp, color = LightSlate)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${dProfile.weight} kg", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (dProfile.weight >= 50.0) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = "",
                                        tint = if (dProfile.weight >= 50.0) SuccessGreen else BloodCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // Last Donation Gap
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val gapReq = if (dProfile.gender.lowercase() == "female") 120 else 90
                                Text("Donation gap ($gapReq Days for ${dProfile.gender})", fontSize = 12.sp, color = LightSlate)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (daysLeft == 0) "Gap Met" else "$daysLeft Days Left",
                                        fontSize = 12.sp,
                                        color = if (daysLeft == 0) SuccessGreen else BloodCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (daysLeft == 0) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                        contentDescription = "",
                                        tint = if (daysLeft == 0) SuccessGreen else BloodCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Next eligible date warning
                        if (daysLeft > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = BloodCrimson.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "",
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Next Eligible Donation Date: $nextEligDate",
                                        fontSize = 10.sp,
                                        color = BloodCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = CardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Gauge Percentage Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Clinical Eligibility Percentage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LightSlate)
                            Text("$percentage%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (percentage == 100) SuccessGreen else PaavaiGold)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = percentage.toFloat() / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (percentage == 100) SuccessGreen else PaavaiGold,
                            trackColor = WarmSlate
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = CardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        var isAdvancedOpen by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isAdvancedOpen = !isAdvancedOpen }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PaavaiGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Deep Clinical AI Screening",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaavaiGold
                                )
                            }
                            Icon(
                                imageVector = if (isAdvancedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = LightSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isAdvancedOpen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Flag any temporary or long-term clinical health conditions below. Gemini will perform real-time medical-style compatibility analysis.",
                                fontSize = 11.sp,
                                color = LightSlate,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // List of symptoms checkboxes
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                symptomOptions.forEach { symptom ->
                                    val isChecked = checkedSymptoms[symptom] ?: false
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { checkedSymptoms[symptom] = !isChecked }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checkedSymptoms[symptom] = it ?: false },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = BloodCrimson,
                                                uncheckedColor = LightSlate,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(symptom, fontSize = 11.sp, color = TextDark)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.performAiEligibilityCheck(
                                        lastDonationDate = dProfile.lastDonationDate,
                                        weight = dProfile.weight,
                                        gender = dProfile.gender,
                                        dob = dProfile.dob,
                                        bloodGroup = dProfile.bloodGroup,
                                        symptoms = checkedSymptoms.toMap()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("run_ai_eligibility_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isCheckingAiEligibility
                            ) {
                                if (isCheckingAiEligibility) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing health metrics...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze with Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (aiEligibilityResult != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("ai_eligibility_result_card"),
                                    colors = CardDefaults.cardColors(containerColor = WarmSlate),
                                    border = BorderStroke(1.dp, PaavaiGold.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MedicalServices,
                                                contentDescription = null,
                                                tint = BloodCrimson,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Clinical AI Diagnostic Report",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = aiEligibilityResult ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextDark,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Smart Donor Score & Milestone Panel
            item {
                val totalDonations = dProfile.totalDonations
                val responseRate = 95 // Simulated base rate
                val emergencyparticipation = if (totalDonations > 0) 1 else 0
                val smartScore = (totalDonations * 35) + (responseRate * 0.4).toInt() + (emergencyparticipation * 25)
                
                val (rank, medalColor, medalIcon) = when {
                    smartScore > 85 -> Triple("Platinum Donor", Color(0xFFE5E4E2), Icons.Default.WorkspacePremium)
                    smartScore > 60 -> Triple("Gold Donor", PaavaiGold, Icons.Default.WorkspacePremium)
                    smartScore > 30 -> Triple("Silver Donor", Color(0xFFC0C0C0), Icons.Default.WorkspacePremium)
                    else -> Triple("Bronze Donor", Color(0xFFCD7F32), Icons.Default.Stars)
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("smart_donor_score_panel"),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(medalColor.copy(alpha = 0.15f), CircleShape)
                                .border(2.dp, medalColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = medalIcon,
                                contentDescription = "Medal",
                                tint = medalColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rank,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF7E22CE).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$smartScore XP",
                                        color = Color(0xFFC084FC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Based on $totalDonations donations • $responseRate% response rate • $emergencyparticipation emergency drives",
                                fontSize = 10.sp,
                                color = LightSlate,
                                lineHeight = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            // Simple visual progress to next milestone
                            val targetLeft = if (smartScore < 30) 30 - smartScore else if (smartScore < 60) 60 - smartScore else if (smartScore < 85) 85 - smartScore else 0
                            val nextRank = if (smartScore < 30) "Silver" else if (smartScore < 60) "Gold" else if (smartScore < 85) "Platinum" else "Max Level"
                            if (targetLeft > 0) {
                                Text(
                                    text = "Need $targetLeft XP more to qualify for $nextRank Status",
                                    fontSize = 9.sp,
                                    color = PaavaiGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Options: Edit profile
            item {
                OutlinedButton(
                    onClick = {
                        showRegisterForm = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_btn"),
                    border = BorderStroke(1.dp, DeepMaroon),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = DeepMaroon)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profile", color = DeepMaroon)
                }
            }

            // Secure session log out button
            item {
                Button(
                    onClick = {
                        viewModel.logout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                        .testTag("profile_logout_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSlate),
                    border = BorderStroke(1.dp, BloodCrimson.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Log Out Token", tint = BloodCrimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Log Out", color = BloodCrimson, fontWeight = FontWeight.Bold)
                }
            }

            // User's private historical donation logs
            item {
                var showAddHistoryDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 8.dp)
                ) {
                    Divider(color = CardBorder, modifier = Modifier.padding(bottom = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Donation History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.testTag("donation_history_section_title")
                            )
                            Text(
                                text = "Track your past life-saving contributions to the community",
                                fontSize = 11.sp,
                                color = LightSlate
                            )
                        }
                        
                        if (dProfile != null) {
                            FilledTonalButton(
                                onClick = { showAddHistoryDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DeepMaroon,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("log_past_donation_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Past Date", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Contribution", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showAddHistoryDialog && dProfile != null) {
                    var inputDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
                    var hospitalNameInput by remember { mutableStateOf("Paavai Blood Camp") }
                    var validationError by remember { mutableStateOf("") }

                    Dialog(onDismissRequest = { showAddHistoryDialog = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("add_history_dialog"),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, PaavaiGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Log Past Contribution Date",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark
                                )

                                Text(
                                    text = "Enter the details of your previous blood donation. This will update your total count and clinical eligibility status dynamically.",
                                    fontSize = 11.sp,
                                    color = LightSlate
                                )

                                OutlinedTextField(
                                    value = inputDate,
                                    onValueChange = { inputDate = it },
                                    label = { Text("Donations Date (YYYY-MM-DD)") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_history_date_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = hospitalNameInput,
                                    onValueChange = { hospitalNameInput = it },
                                    label = { Text("Hospital or Camp Location") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_history_location_input"),
                                    singleLine = true
                                )

                                if (validationError.isNotEmpty()) {
                                    Text(text = validationError, color = BloodCrimson, fontSize = 11.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showAddHistoryDialog = false }) {
                                        Text("Cancel", color = LightSlate)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val parts = inputDate.split("-")
                                            if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2 ||
                                                parts[0].toIntOrNull() == null || parts[1].toIntOrNull() == null || parts[2].toIntOrNull() == null) {
                                                validationError = "Please use exact YYYY-MM-DD pattern (e.g. 2026-03-01)"
                                                return@Button
                                            }
                                            val mMonth = parts[1].toInt()
                                            val mDay = parts[2].toInt()
                                            if (mMonth < 1 || mMonth > 12 || mDay < 1 || mDay > 31) {
                                                validationError = "Please enter a valid month/day combination."
                                                return@Button
                                            }

                                            viewModel.addManualDonationHistory(
                                                donorRegisterNumber = dProfile.registerNumber,
                                                date = inputDate,
                                                units = 1,
                                                hospitalName = hospitalNameInput.ifEmpty { "Paavai Blood Camp" }
                                            )
                                            showAddHistoryDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                        modifier = Modifier.testTag("submit_manual_history_btn")
                                    ) {
                                        Text("Save Log", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (dProfile != null) {
                item {
                    PersonalDonationStatsChart(
                        userHistories = userHistories,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }

            if (dProfile != null && userHistories.isNotEmpty()) {
                item {
                    val totalUnits = userHistories.sumOf { it.unitsDonated }
                    val estimatedLivesSaved = totalUnits * 3
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("donation_history_stats_card"),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "COMMUNITY IMPACT METRIC",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightGold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your blood donations are directly helping our local community in Namakkal.",
                                    fontSize = 11.sp,
                                    color = LightSlate,
                                    lineHeight = 15.sp
                                )
                            }
                            
                            Row(
                                modifier = Modifier.padding(start = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$totalUnits",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BloodCrimson
                                    )
                                    Text(
                                        text = "Units",
                                        fontSize = 10.sp,
                                        color = LightSlate,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$estimatedLivesSaved",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Lives Saved",
                                        fontSize = 10.sp,
                                        color = LightSlate,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (userHistories.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("donation_history_empty_card"),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No registered physical donations recorded yet.",
                                fontSize = 12.sp,
                                color = LightSlate
                            )
                            Text(
                                "Fulfill active requests under 'Requests' or participate in camps to build certificate counts!",
                                fontSize = 10.sp,
                                color = LightSlate,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(userHistories) { hist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("donation_history_item_${hist.id}"),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BloodCrimson.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Contribution",
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = hist.hospitalName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = LightSlate,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = hist.date, fontSize = 11.sp, color = LightSlate)
                                    }
                                }
                            }
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "+${hist.unitsDonated} Unit",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Elegant, premium golden certificate generator Dialog (Deactivated)
    if (false) {
        val dProfile = profile ?: return
        
        Dialog(onDismissRequest = { if (!isDownloadingPdf) showCertificateDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFFFFFDE7)) // Light Gold Canvas Color
                        .border(
                            BorderStroke(4.dp, Brush.radialGradient(colors = listOf(PaavaiGold, DeepMaroon))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Certificate Headers
                    Text(
                        text = "Certificate of Appreciation",
                        color = DeepMaroon,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "PAAVAI BLOODCONNECT CELL",
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "PROUDLY PRESENTED TO",
                        color = LightSlate,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = dProfile.name.uppercase(),
                        color = TextDark,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Register No: ${dProfile.registerNumber} | ${dProfile.department}",
                        color = LightSlate,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "In recognition of their voluntary and selfless contribution of life-saving ${dProfile.bloodGroup} blood under the Paavai college community donor loop.",
                        color = TextDark,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Seal and signatures mock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Dr. C. Shanthi",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 10.sp
                            )
                            Divider(color = LightSlate, modifier = Modifier.width(60.dp))
                            Text(text = "RRC Program Officer", fontSize = 8.sp, color = LightSlate)
                        }

                        // Gold Seal icon
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(PaavaiGold, CircleShape)
                                .border(1.dp, DeepMaroon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Seal",
                                tint = DeepMaroon,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 10.sp
                            )
                            Divider(color = LightSlate, modifier = Modifier.width(60.dp))
                            Text(text = "Date of Generation", fontSize = 8.sp, color = LightSlate)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    if (isDownloadingPdf) {
                        CircularProgressIndicator(color = DeepMaroon)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Compiling PDF and certificates metadata...", fontSize = 10.sp, color = DeepMaroon)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCertificateDialog = false },
                                border = BorderStroke(1.dp, DeepMaroon),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepMaroon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Close")
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        isDownloadingPdf = true
                                        kotlinx.coroutines.delay(2000)
                                        isDownloadingPdf = false
                                        Toast.makeText(context, "Certificate PDF downloaded to Downloads/Paavai_BloodConnect_${dProfile.registerNumber}.pdf", Toast.LENGTH_LONG).show()
                                        showCertificateDialog = false
                                    }
                                },
                                modifier = Modifier.testTag("download_pdf_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalDonationStatsChart(
    userHistories: List<DonationHistory>,
    modifier: Modifier = Modifier
) {
    val sortedHistories = remember(userHistories) {
        userHistories.sortedBy { it.date }
    }

    var isImpactMode by remember { mutableStateOf(false) }
    
    val finalPoints = remember(sortedHistories, isImpactMode) {
        if (sortedHistories.isEmpty()) {
            listOf(
                StudentDonationChartPoint("Term 1", 0f, 0f),
                StudentDonationChartPoint("Term 2", 1f, 3f),
                StudentDonationChartPoint("Term 3", 2f, 6f),
                StudentDonationChartPoint("Term 4", 3f, 9f)
            )
        } else {
            var cumulativeUnits = 0f
            sortedHistories.mapIndexed { idx, history ->
                cumulativeUnits += history.unitsDonated
                val yVal = if (isImpactMode) cumulativeUnits * 3f else (idx + 1).toFloat()
                val label = try {
                    val parts = history.date.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].substring(2)
                        val month = parts[1].toInt()
                        val monthStr = when(month) {
                            1 -> "Jan"
                            2 -> "Feb"
                            3 -> "Mar"
                            4 -> "Apr"
                            5 -> "May"
                            6 -> "Jun"
                            7 -> "Jul"
                            8 -> "Aug"
                            9 -> "Sep"
                            10 -> "Oct"
                            11 -> "Nov"
                            12 -> "Dec"
                            else -> "M"
                        }
                        "$monthStr, '$year"
                    } else {
                        history.date
                    }
                } catch (e: Exception) {
                    history.date
                }
                StudentDonationChartPoint(label, (idx + 1).toFloat(), yVal)
            }
        }
    }

    var selectedPointIndex by remember(finalPoints) { mutableStateOf(finalPoints.size - 1) }
    val activePoint = finalPoints.getOrNull(selectedPointIndex)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("personal_donation_stats_chart_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PERSONAL IMPACT ANALYTICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightGold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isImpactMode) "Cumulative Community Impact" else "Donation Frequency & Timeline",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Row(
                    modifier = Modifier
                        .background(CardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Frequency",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isImpactMode) Color.White else LightSlate,
                        modifier = Modifier
                            .background(if (!isImpactMode) DeepMaroon else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { isImpactMode = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("stats_frequency_toggle")
                    )
                    Text(
                        text = "Impact",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isImpactMode) Color.White else LightSlate,
                        modifier = Modifier
                            .background(if (isImpactMode) DeepMaroon else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { isImpactMode = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("stats_impact_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(finalPoints) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val stepX = if (finalPoints.size > 1) width / (finalPoints.size - 1).toFloat() else width.toFloat()
                                val tapIndex = (offset.x / stepX).roundToInt().coerceIn(0, finalPoints.size - 1)
                                selectedPointIndex = tapIndex
                            }
                        }
                        .testTag("personal_impact_canvas")
                ) {
                    val width = size.width
                    val height = size.height
                    val pointCount = finalPoints.size
                    
                    if (pointCount > 0) {
                        val maxVal = finalPoints.maxOf { it.pointYValue }.coerceAtLeast(1f)
                        val stepX = if (pointCount > 1) width / (pointCount - 1).toFloat() else width

                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val yCoord = height * (i / gridLines.toFloat())
                            drawLine(
                                color = CardBorder.copy(alpha = 0.3f),
                                strokeWidth = 1f,
                                start = Offset(0f, yCoord),
                                end = Offset(width, yCoord)
                            )
                        }

                        val pts = finalPoints.mapIndexed { idx, pt ->
                            val x = if (pointCount > 1) idx * stepX else width / 2f
                            val y = height - (pt.pointYValue / maxVal * (height - 30.dp.toPx())) - 15.dp.toPx()
                            Offset(x, y)
                        }

                        val path = Path()
                        val areaPath = Path()

                        path.moveTo(pts[0].x, pts[0].y)
                        areaPath.moveTo(pts[0].x, height)
                        areaPath.lineTo(pts[0].x, pts[0].y)

                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val curr = pts[i]
                            val cp1X = prev.x + (curr.x - prev.x) / 2f
                            val cp1Y = prev.y
                            val cp2X = prev.x + (curr.x - prev.x) / 2f
                            val cp2Y = curr.y

                            path.cubicTo(cp1X, cp1Y, cp2X, cp1Y, curr.x, curr.y)
                            areaPath.cubicTo(cp1X, cp1Y, cp2X, cp1Y, curr.x, curr.y)
                        }

                        areaPath.lineTo(pts.last().x, height)
                        areaPath.close()

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BloodCrimson.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )

                        drawPath(
                            path = path,
                            color = if (sortedHistories.isEmpty()) LightSlate.copy(alpha = 0.5f) else DeepMaroon,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        if (sortedHistories.isEmpty()) {
                            val idealDottedPath = Path().apply {
                                moveTo(0f, height - 15.dp.toPx())
                                lineTo(width, 15.dp.toPx())
                            }
                            drawPath(
                                path = idealDottedPath,
                                color = SuccessGreen.copy(alpha = 0.4f),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }

                        pts.forEachIndexed { index, pt ->
                            val isSelected = index == selectedPointIndex
                            if (isSelected) {
                                drawCircle(
                                    color = LightGold,
                                    radius = 6.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = DeepMaroon,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )
                            } else {
                                drawCircle(
                                    color = BloodCrimson.copy(alpha = 0.8f),
                                    radius = 3.2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                finalPoints.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedPointIndex
                    Text(
                        text = pt.xLabel,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DeepMaroon else LightSlate,
                        modifier = Modifier
                            .clickable { selectedPointIndex = idx }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            activePoint?.let { pt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isImpactMode) Icons.Default.Favorite else Icons.Default.ShowChart,
                            contentDescription = "",
                            tint = if (isImpactMode) SuccessGreen else BloodCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isImpactMode) {
                                "Estimated Impact: ~${pt.pointYValue.toInt()} Lives Saved"
                            } else {
                                "Donation Event #${pt.pointYValue.toInt()}: Verified"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (sortedHistories.isEmpty()) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
                    ) {
                        Text(
                            text = if (sortedHistories.isEmpty()) "Blueprint View" else "Logged Log",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (sortedHistories.isEmpty()) DeepMaroon else SuccessGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (sortedHistories.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Ideal Trend Model Displayed. Each formal blood donation yields 1 Unit (saving up to 3 local patients). Start logging your historical donations to render your actual personal telemetry live!",
                    fontSize = 10.sp,
                    color = LightSlate,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

data class StudentDonationChartPoint(
    val xLabel: String,
    val xIndex: Float,
    val pointYValue: Float
)
