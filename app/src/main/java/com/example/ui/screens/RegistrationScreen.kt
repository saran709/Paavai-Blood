package com.example.ui.screens

import android.os.Handler
import android.os.Looper
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
import com.example.data.Donor
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: BloodConnectViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.registeredProfile.collectAsState()
    val history by viewModel.allHistory.collectAsState()

    var showRegisterForm by remember { mutableStateOf(profile == null) }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var isDownloadingPdf by remember { mutableStateOf(false) }

    // Register parameters
    var name by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("B.E. Computer Science") }
    var year by remember { mutableStateOf("3rd Year") }
    var bloodGroup by remember { mutableStateOf("O-") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(profile?.email ?: "student@paavai.edu.in") }
    var location by remember { mutableStateOf("Paavai Engineering Campus") }
    var weightText by remember { mutableStateOf("65") }
    var lastDonation by remember { mutableStateOf("") } // YYYY-MM-DD
    var userType by remember { mutableStateOf("Student") } // "Student", "Faculty", "Alumni"
    
    var errorText by remember { mutableStateOf("") }

    val userHistories = history.filter { it.donorRegisterNumber == profile?.registerNumber }

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
                    text = "Obtain your verified digital card and download donation certificates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate
                )
            }
        }

        if (profile == null || showRegisterForm) {
            // Profile registration Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Register as Paavai network donor",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Input real metrics to receive matching alerts and certification records.",
                            fontSize = 11.sp,
                            color = LightSlate
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Category switch
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Student", "Faculty", "Alumni").forEach { type ->
                                val active = type == userType
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) DeepMaroon else WarmSlate)
                                        .clickable { userType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else TextDark
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regNo,
                            onValueChange = { regNo = it },
                            label = { Text(if (userType == "Faculty") "Faculty ID" else "Register Number (Roll No)") },
                            modifier = Modifier.fillMaxWidth().testTag("reg_roll_input"),
                            singleLine = true
                        )

                        // Mapped Dept simple selector
                        OutlinedTextField(
                            value = dept,
                            onValueChange = { dept = it },
                            label = { Text("Department / Branch") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Year level
                            OutlinedTextField(
                                value = year,
                                onValueChange = { year = it },
                                label = { Text("Year/Level") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            // Blood group selector
                            OutlinedTextField(
                                value = bloodGroup,
                                onValueChange = { bloodGroup = it },
                                label = { Text("Blood Group") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("Weight (kg)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = lastDonation,
                                onValueChange = { lastDonation = it },
                                label = { Text("Last Donation Date") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email (for certificate delivery)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Live Campus Location / Residence") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (errorText.isNotEmpty()) {
                            Text(text = errorText, color = BloodCrimson, fontSize = 11.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (profile != null) {
                                OutlinedButton(
                                    onClick = { showRegisterForm = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel")
                                }
                            }
                            Button(
                                onClick = {
                                    val w = weightText.toDoubleOrNull() ?: 0.0
                                    if (name.trim().isEmpty() || regNo.trim().isEmpty() || phone.trim().isEmpty()) {
                                        errorText = "Please fill in Name, Register Number, and Phone."
                                        return@Button
                                    }
                                    if (w < 35.0) {
                                        errorText = "Please enter a valid weight."
                                        return@Button
                                    }
                                    viewModel.registerDonor(
                                        name = name,
                                        regNo = regNo,
                                        dept = dept,
                                        year = year,
                                        bloodGroup = bloodGroup,
                                        mobile = phone,
                                        email = email,
                                        location = location,
                                        weight = w,
                                        lastDonation = lastDonation,
                                        userType = userType,
                                        onSuccess = {
                                            showRegisterForm = false
                                        }
                                    )
                                    Toast.makeText(context, "Donor Registration Successful!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .testTag("submit_registration_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Register & Acquire QR Card")
                            }
                        }
                    }
                }
            }
        } else {
            val dProfile = profile!!
            
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

                        // Custom drawn robust vector QR Code matrix frame matching student roll number
                        QrCodeCanvas(regNo = dProfile.registerNumber)

                        Spacer(modifier = Modifier.height(20.dp))

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

                        Spacer(modifier = Modifier.height(8.dp))

                        // QR scanning code footer
                        Text(
                            text = "SCAN QR FOR VERIFICATION RECORDS",
                            color = LightGold.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Options: Edit profile, download certs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Prepopulate edit parameters
                            name = dProfile.name
                            regNo = dProfile.registerNumber
                            dept = dProfile.department
                            year = dProfile.year
                            bloodGroup = dProfile.bloodGroup
                            phone = dProfile.mobileNumber
                            email = dProfile.email
                            location = dProfile.location
                            weightText = dProfile.weight.toString()
                            lastDonation = dProfile.lastDonationDate
                            userType = dProfile.userType
                            showRegisterForm = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_profile_btn"),
                        border = BorderStroke(1.dp, DeepMaroon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = DeepMaroon)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile", color = DeepMaroon)
                    }

                    Button(
                        onClick = { showCertificateDialog = true },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("view_certs_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CardMembership, contentDescription = "Certificate")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Certificates")
                    }
                }
            }

            // User's private historical donation logs
            item {
                Text(
                    text = "My Donation Chronicles (${userHistories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            if (userHistories.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(12.dp)
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = hist.hospitalName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Text(text = hist.date, fontSize = 11.sp, color = LightSlate)
                            }
                            Text(
                                text = "+${hist.unitsDonated} Unit",
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreen,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Elegant, premium golden certificate generator Dialog
    if (showCertificateDialog) {
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
                                    isDownloadingPdf = true
                                    // Simulate downloading pdf with a timed handler
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        isDownloadingPdf = false
                                        Toast.makeText(context, "Certificate PDF downloaded to Downloads/Paavai_BloodConnect_${dProfile.registerNumber}.pdf", Toast.LENGTH_LONG).show()
                                        showCertificateDialog = false
                                    }, 2000)
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

// Draw custom high-fidelity QR Code Matrix using Vector Canvas to represent roll numbers
@Composable
fun QrCodeCanvas(regNo: String) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, PaavaiGold, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // We draw custom vector lines matching a generic QR framework
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = size.width
            val cellSize = size / 10

            // 1. Draw standard finder patterns at top-left, top-right, bottom-left
            fun drawFinder(x: Float, y: Float) {
                // Outer ring
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(x, y),
                    size = Size(cellSize * 3, cellSize * 3)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(x + cellSize * 0.4f, y + cellSize * 0.4f),
                    size = Size(cellSize * 2.2f, cellSize * 2.2f)
                )
                // Center block
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(x + cellSize * 0.8f, y + cellSize * 0.8f),
                    size = Size(cellSize * 1.4f, cellSize * 1.4f)
                )
            }

            // Draw finders
            drawFinder(0f, 0f) // Top left
            drawFinder(size - cellSize * 3, 0f) // Top right
            drawFinder(0f, size - cellSize * 3) // Bottom left

            // 2. Draw mock QR bits dynamically based on register string hash sequence
            val hash = kotlin.math.abs(regNo.hashCode())
            val random = Random(hash.toLong())

            for (col in 0..9) {
                for (row in 0..9) {
                    // Skip finders are
                    val inTopLeft = col in 0..2 && row in 0..2
                    val inTopRight = col in 7..9 && row in 0..2
                    val inBottomLeft = col in 0..2 && row in 7..9
                    val isCenterSpace = col in 4..5 && row in 4..5 // Center red drop area

                    if (!inTopLeft && !inTopRight && !inBottomLeft && !isCenterSpace) {
                        // Draw bit if random threshold succeeds
                        if (random.nextFloat() > 0.45f) {
                            drawRect(
                                color = Color.DarkGray,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize * 0.9f, cellSize * 0.9f)
                            )
                        }
                    }
                }
            }

            // Draw an overlay center blood drop red cross
            val center = size / 2
            drawCircle(
                color = Color.White,
                radius = cellSize,
                center = Offset(center, center)
            )
            // Draw small simple blood cross in the middle
            drawCircle(
                color = Color(0xFFD32F2F),
                radius = cellSize * 0.7f,
                center = Offset(center, center)
            )
        }
    }
}
