package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Donor
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DonorRegistrationForm(
    initialProfile: Donor?,
    onCancel: (() -> Unit)? = null,
    onSubmit: (Donor) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var regNo by remember { mutableStateOf(initialProfile?.registerNumber ?: "") }
    var dept by remember { mutableStateOf(initialProfile?.department ?: "B.E. Computer Science") }
    var year by remember { mutableStateOf(initialProfile?.year ?: "3rd Year") }
    var bloodGroup by remember { mutableStateOf(initialProfile?.bloodGroup ?: "O-") }
    var phone by remember { mutableStateOf(initialProfile?.mobileNumber ?: "") }
    var email by remember { mutableStateOf(initialProfile?.email ?: "student@paavai.edu.in") }
    var location by remember { mutableStateOf(initialProfile?.location ?: "Paavai Engineering Campus") }
    var weightText by remember { mutableStateOf(initialProfile?.weight?.toString() ?: "55") }
    var lastDonation by remember { mutableStateOf(initialProfile?.lastDonationDate ?: "") }
    var userType by remember { mutableStateOf(initialProfile?.userType ?: "Student") }
    var gender by remember { mutableStateOf(initialProfile?.gender ?: "Male") }
    var dob by remember { mutableStateOf(initialProfile?.dob ?: "2005-01-01") }
    var address by remember { mutableStateOf(initialProfile?.address ?: "Namakkal, Tamil Nadu") }
    var emergencyContact by remember { mutableStateOf(initialProfile?.emergencyContact ?: "") }
    var profilePhoto by remember { mutableStateOf(initialProfile?.profilePhoto ?: "") }
    var errorText by remember { mutableStateOf("") }

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
                text = "Input real metrics to receive matching alerts and notification records.",
                fontSize = 11.sp,
                color = LightSlate
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = CardBorder)

            // Category switch
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Student", "Faculty").forEach { type ->
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

            OutlinedTextField(
                value = dept,
                onValueChange = { dept = it },
                label = { Text("Department / Branch") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year/Level") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group (e.g., O-, A+)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Text("Gender", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LightSlate)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmSlate)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("Male", "Female").forEach { g ->
                            val active = g == gender
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) DeepMaroon else Color.Transparent)
                                    .clickable { gender = g }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = g,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextDark
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("DOB (YYYY-MM-DD)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1.0f).testTag("reg_dob_input"),
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
                label = { Text("Email Address") },
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

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Full Residential Address") },
                modifier = Modifier.fillMaxWidth().testTag("reg_address_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("Emergency Contact (Name & Mobile)") },
                modifier = Modifier.fillMaxWidth().testTag("reg_emergency_contact_input"),
                singleLine = true
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WarmSlate.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                profilePhoto = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?fit=crop&w=120&h=120"
                                Toast.makeText(context, "Profile Photo Upload Simulated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            tint = PaavaiGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cloud Profile Photo Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(
                                text = if (profilePhoto.isNotEmpty()) "Simulated upload complete!" else "Upload your photo badge",
                                fontSize = 10.sp,
                                color = LightSlate
                            )
                        }
                    }
                    if (profilePhoto.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Checked",
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (errorText.isNotEmpty()) {
                Text(text = errorText, color = BloodCrimson, fontSize = 11.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onCancel != null) {
                    OutlinedButton(
                        onClick = { onCancel() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
                Button(
                    onClick = {
                        val w = weightText.toDoubleOrNull() ?: 0.0
                        if (name.trim().isEmpty() || regNo.trim().isEmpty() || phone.trim().isEmpty() || dob.trim().isEmpty() || address.trim().isEmpty() || emergencyContact.trim().isEmpty()) {
                            errorText = "Please fill in all fields (Name, Register No, DOB, Address, Emergency Contact)."
                            return@Button
                        }
                        if (w < 35.0) {
                            errorText = "Please enter a valid weight density."
                            return@Button
                        }
                        
                        val validBGs = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                        val normalizedBG = bloodGroup.trim().uppercase()
                        if (!validBGs.contains(normalizedBG)) {
                            errorText = "Invalid Blood Group. Must be one of: A+, A-, B+, B-, AB+, AB-, O+, O-."
                            return@Button
                        }

                        val dobParts = dob.split("-")
                        if (dobParts.size != 3 || dob.length != 10) {
                            errorText = "Date of Birth format must be YYYY-MM-DD (e.g., 2005-04-12)."
                            return@Button
                        }

                        errorText = ""
                        onSubmit(
                            Donor(
                                name = name,
                                registerNumber = regNo,
                                department = dept,
                                year = year,
                                bloodGroup = normalizedBG,
                                mobileNumber = phone,
                                email = email,
                                location = location,
                                weight = w,
                                lastDonationDate = lastDonation,
                                userType = userType,
                                gender = gender,
                                dob = dob,
                                address = address,
                                emergencyContact = emergencyContact,
                                profilePhoto = profilePhoto,
                                availability = initialProfile?.availability ?: true,
                                totalDonations = initialProfile?.totalDonations ?: 0
                            )
                        )
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
