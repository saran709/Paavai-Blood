package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: BloodConnectViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }

    // Forms Inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Signup specific fields
    var name by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("B.E. Computer Science") }
    var year by remember { mutableStateOf("3rd Year") }
    var bloodGroup by remember { mutableStateOf("O-") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Student Donor") } // "Student Donor", "Volunteer", "Admin"

    var errorText by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmSlate
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(DeepMaroon, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bloodtype,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Paavai BloodConnect",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepMaroon
                    )
                    Text(
                        text = "Bridging emergency donors with Namakkal hospitals",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightSlate,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Tab Selection (Sign In vs Sign Up)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCharcoal)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isSignUp) DeepMaroon else Color.Transparent)
                            .clickable { 
                                isSignUp = false 
                                errorText = ""
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SIGN IN",
                            color = if (!isSignUp) Color.White else LightSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSignUp) DeepMaroon else Color.Transparent)
                            .clickable { 
                                isSignUp = true 
                                errorText = ""
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SIGN UP",
                            color = if (isSignUp) Color.White else LightSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Main Input Fields Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSignUp) {
                            // Signup Header and role selection
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            Text(
                                text = "Register Role:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightSlate
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Student Donor", "Volunteer").forEach { currentRole ->
                                    val active = role == currentRole
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) DeepMaroon else WarmSlate)
                                            .border(
                                                1.dp,
                                                if (active) DeepMaroon else CardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { role = currentRole }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentRole.replace("Student ", ""),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else TextDark
                                        )
                                    }
                                }
                            }

                            Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "", tint = LightSlate) },
                                modifier = Modifier.fillMaxWidth().testTag("signup_name"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                            )

                            OutlinedTextField(
                                value = regNo,
                                onValueChange = { regNo = it },
                                label = { Text("Register Number / Staff ID") },
                                leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = "", tint = LightSlate) },
                                modifier = Modifier.fillMaxWidth().testTag("signup_reg_no"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bloodGroup,
                                    onValueChange = { bloodGroup = it },
                                    label = { Text("Blood Group") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                                )
                                OutlinedTextField(
                                    value = dept,
                                    onValueChange = { dept = it },
                                    label = { Text("Dept / Branch") },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                                )
                            }

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Mobile Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "", tint = LightSlate) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                            )
                        } else {
                            Text(
                                text = "Sign Into Credentials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        // Shared Login Credentials
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "", tint = LightSlate) },
                            modifier = Modifier.fillMaxWidth().testTag("login_email"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "", tint = LightSlate) },
                            modifier = Modifier.fillMaxWidth().testTag("login_password"),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )

                        if (errorText.isNotEmpty()) {
                            Text(
                                text = errorText,
                                color = BloodCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (email.trim().isEmpty() || password.trim().isEmpty()) {
                                    errorText = "Please enter both Email and Password."
                                    return@Button
                                }
                                if (isSignUp) {
                                    if (name.trim().isEmpty() || regNo.trim().isEmpty() || phone.trim().isEmpty()) {
                                        errorText = "Information missing. Fill all signup boxes."
                                        return@Button
                                    }
                                    val ok = viewModel.signup(
                                        name = name,
                                        email = email,
                                        pass = password,
                                        regNo = regNo,
                                        dept = dept,
                                        year = year,
                                        bloodGroup = bloodGroup,
                                        phone = phone,
                                        role = role
                                    )
                                    if (ok) {
                                        Toast.makeText(context, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        errorText = "Account already exists with this Email."
                                    }
                                } else {
                                    val ok = viewModel.login(email, password)
                                    if (ok) {
                                        Toast.makeText(context, "Sign In Successful!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        errorText = "Incorrect email address or password combination."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("auth_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "CREATE ACCOUNT & JOIN" else "SECURE SIGN IN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Quick Tester Credentials Sandbox Panel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCharcoal.copy(alpha = 0.5f))
                        .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🛠️ DEMO TESTING PRESET CREDENTIALS",
                        color = PaavaiGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap any role to pre-populate mock college credentials immediately:",
                        color = LightSlate,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Admin preset
                        Button(
                            onClick = {
                                email = "blood@paavai.com"
                                password = "blood@123"
                                isSignUp = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSlate, contentColor = TextDark),
                            border = BorderStroke(1.dp, DeepMaroon.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("As Admin", fontSize = 10.sp)
                        }

                        // Volunteer preset
                        Button(
                            onClick = {
                                email = "volunteer@paavai.edu.in"
                                password = "vol123"
                                isSignUp = false
                            },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSlate, contentColor = TextDark),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("As Volunteer", fontSize = 10.sp)
                        }

                        // Student preset
                        Button(
                            onClick = {
                                email = "student@paavai.edu.in"
                                password = "stud123"
                                isSignUp = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSlate, contentColor = TextDark),
                            border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("As Student", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
