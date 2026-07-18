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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: BloodConnectViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }

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
    var academicAffiliation by remember { mutableStateOf("Student") } // "Student", "Faculty"

    var errorText by remember { mutableStateOf("") }
    var bloodGroupExpanded by remember { mutableStateOf(false) }
    val bloodGroupsList = remember { listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-") }

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

                            Text(
                                text = "Academic Affiliation / Category:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightSlate
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                             ) {
                                listOf("Student", "Faculty").forEach { category ->
                                    val active = academicAffiliation == category
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
                                            .clickable { academicAffiliation = category }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else TextDark
                                        )
                                    }
                                }
                            }

                            if (academicAffiliation == "Student") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Current Academic Year:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightSlate
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("1st Year", "2nd Year", "3rd Year", "4th Year").forEach { currentYear ->
                                        val active = year == currentYear
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (active) BloodCrimson else WarmSlate)
                                                .border(
                                                    1.dp,
                                                    if (active) BloodCrimson else CardBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { year = currentYear }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currentYear,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.White else TextDark
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

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
                                label = { 
                                    Text(
                                        when (academicAffiliation) {
                                            "Faculty" -> "Staff / Faculty ID Number"
                                            else -> "Register Number (Roll No)"
                                        }
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = "", tint = LightSlate) },
                                modifier = Modifier.fillMaxWidth().testTag("signup_reg_no"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1.2f)) {
                                    OutlinedTextField(
                                        value = bloodGroup,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Blood Group") },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { bloodGroupExpanded = !bloodGroupExpanded },
                                                modifier = Modifier.testTag("blood_group_dropdown_trigger")
                                            ) {
                                                Icon(
                                                    imageVector = if (bloodGroupExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                    contentDescription = "Expand blood group",
                                                    tint = LightSlate
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { bloodGroupExpanded = true }
                                            .testTag("signup_blood_group"),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon, focusedLabelColor = DeepMaroon)
                                    )
                                    DropdownMenu(
                                        expanded = bloodGroupExpanded,
                                        onDismissRequest = { bloodGroupExpanded = false },
                                        modifier = Modifier.background(Color.White).width(120.dp)
                                    ) {
                                        bloodGroupsList.forEach { group ->
                                            DropdownMenuItem(
                                                text = { Text(group, color = TextDark, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    bloodGroup = group
                                                    bloodGroupExpanded = false
                                                },
                                                modifier = Modifier.testTag("blood_group_option_$group")
                                            )
                                        }
                                    }
                                }
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

                        val isEmailNotEmpty = email.trim().isNotEmpty()
                        val isPaavaiDomain = email.trim().endsWith("@paavai.edu.in", ignoreCase = true)
                        val isEmailRegexValid = remember(email) {
                            "^[A-Za-z0-9._%+-]+@paavai\\.edu\\.in$".toRegex(RegexOption.IGNORE_CASE).matches(email.trim())
                        }

                        if (isEmailNotEmpty) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEmailRegexValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified Paavai Academic Email Domain",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("email_validation_success")
                                    )
                                } else if (isPaavaiDomain) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Incomplete",
                                        tint = LightGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Incomplete email prefix (e.g., username@paavai.edu.in)",
                                        color = LightGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("email_validation_warning")
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Invalid",
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Restricted to @paavai.edu.in domain",
                                        color = BloodCrimson,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("email_validation_error")
                                    )
                                }
                            }
                        }

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
                                val trimEmail = email.trim().lowercase()
                                val paavaiRegex = "^[A-Za-z0-9._%+-]+@paavai\\.edu\\.in$".toRegex(RegexOption.IGNORE_CASE)
                                if (!paavaiRegex.matches(trimEmail)) {
                                    errorText = "Invalid email. Access restricted. Please enter a valid @paavai.edu.in academic email."
                                    return@Button
                                }
                                if (isSignUp) {
                                    if (name.trim().isEmpty() || regNo.trim().isEmpty() || phone.trim().isEmpty()) {
                                        errorText = "Information missing. Fill all signup boxes."
                                        return@Button
                                    }
                                    if (bloodGroup.trim().uppercase() !in bloodGroupsList) {
                                        errorText = "Invalid blood group selected. Please select a valid option from the dropdown menu."
                                        return@Button
                                    }
                                    val finalYear = when (academicAffiliation) {
                                        "Faculty" -> "Faculty"
                                        else -> year
                                    }
                                    val finalUserType = when (academicAffiliation) {
                                        "Faculty" -> "Faculty"
                                        else -> "Student"
                                    }
                                    val ok = viewModel.signup(
                                        name = name,
                                        email = email,
                                        pass = password,
                                        regNo = regNo,
                                        dept = dept,
                                        year = finalYear,
                                        bloodGroup = bloodGroup,
                                        phone = phone,
                                        role = role,
                                        userType = finalUserType
                                    )
                                    if (ok) {
                                        Toast.makeText(context, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        errorText = "Account already exists with this Email."
                                    }
                                } else {
                                    isLoggingIn = true
                                    errorText = ""
                                    coroutineScope.launch {
                                        val ok = viewModel.loginLive(email, password)
                                        isLoggingIn = false
                                        if (ok) {
                                            Toast.makeText(context, "Sign In Successful!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        } else {
                                            errorText = "Incorrect email address or password combination (Checked Offline & Live Backend)."
                                        }
                                    }
                                }
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("auth_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoggingIn) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "VERIFYING LIVE BACKEND...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            } else {
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
            }


        }
    }
}
