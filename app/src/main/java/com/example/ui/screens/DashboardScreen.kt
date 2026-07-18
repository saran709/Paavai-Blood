package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DonationCamp
import com.example.data.DonationHistory
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel
import androidx.compose.foundation.gestures.detectDragGestures
import android.content.Intent
import android.net.Uri
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.lazy.LazyRow
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BloodConnectViewModel,
    onNavigateToFinder: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val donors by viewModel.allDonors.collectAsState()
    val requests by viewModel.allRequests.collectAsState()
    val camps by viewModel.allCamps.collectAsState()
    val histories by viewModel.allHistory.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val profile by viewModel.registeredProfile.collectAsState()
    val alertLogs by viewModel.smsAlertLogs.collectAsState()
    val activeNotifications by viewModel.activeNotifications.collectAsState()

    var showSosDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var selectedGroupTab by remember { mutableStateOf("Overall") } // "Overall", "Department-wise"
    var selectedFaqTab by remember { mutableStateOf("Guidelines") } // "Guidelines", "PostCare", "Myths"
    var expandedFaqId by remember { mutableStateOf<String?>(null) }

    val unreadCount = activeNotifications.count { !it.isRead }

    // Live Emergency Alert banner memoized matching state
    val recentEmergencyAlert = remember(activeNotifications) {
        activeNotifications.firstOrNull { !it.isRead && (it.urgencyLevel == "Critical" || it.urgencyLevel == "High") }
    }

    // Stat aggregates
    val totalDonors = donors.size
    val activeRequests = requests.count { !it.isFulfilled }
    val completedDonations = histories.sumOf { it.unitsDonated }
    val livesSaved = completedDonations * 3 // Blood donation standard: 1 unit saves up to 3 lives

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {


        // Supabase Cloud Sync Control Module (Live Cloud Sync Enabled)
        item {
            val syncStatus by viewModel.supabaseSyncStatus.collectAsState()
            val lastSyncTime by viewModel.supabaseLastSyncTime.collectAsState()
            val syncErrorMessage by viewModel.supabaseSyncErrorMessage.collectAsState()
            var showInstructionsDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("supabase_sync_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp, 
                    when (syncStatus) {
                        com.example.data.SupabaseSyncStatus.SUCCESS -> SuccessGreen.copy(alpha = 0.5f)
                        com.example.data.SupabaseSyncStatus.ERROR -> BloodCrimson.copy(alpha = 0.5f)
                        com.example.data.SupabaseSyncStatus.SYNCING -> PaavaiGold.copy(alpha = 0.5f)
                        else -> LightSlate.copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = when (syncStatus) {
                                    com.example.data.SupabaseSyncStatus.SUCCESS -> SuccessGreen
                                    com.example.data.SupabaseSyncStatus.ERROR -> BloodCrimson
                                    com.example.data.SupabaseSyncStatus.SYNCING -> PaavaiGold
                                    else -> LightSlate
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Supabase Cloud Backend",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Badge representation of status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (syncStatus) {
                                com.example.data.SupabaseSyncStatus.SUCCESS -> SuccessGreen.copy(alpha = 0.2f)
                                com.example.data.SupabaseSyncStatus.ERROR -> BloodCrimson.copy(alpha = 0.2f)
                                com.example.data.SupabaseSyncStatus.SYNCING -> PaavaiGold.copy(alpha = 0.2f)
                                else -> Color.White.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = when (syncStatus) {
                                    com.example.data.SupabaseSyncStatus.SUCCESS -> "CONNECTED"
                                    com.example.data.SupabaseSyncStatus.ERROR -> "SYNC ERROR"
                                    com.example.data.SupabaseSyncStatus.SYNCING -> "SYNCING..."
                                    else -> "OFFLINE MODE"
                                },
                                color = when (syncStatus) {
                                    com.example.data.SupabaseSyncStatus.SUCCESS -> SuccessGreen
                                    com.example.data.SupabaseSyncStatus.ERROR -> BloodCrimson
                                    com.example.data.SupabaseSyncStatus.SYNCING -> PaavaiGold
                                    else -> LightSlate
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = when (syncStatus) {
                            com.example.data.SupabaseSyncStatus.SUCCESS -> "Clinical database and SOS dispatches are live compiled in your Supabase PostgreSQL cloud."
                            com.example.data.SupabaseSyncStatus.ERROR -> "Cloud sync interrupted: ${syncErrorMessage ?: "Connection refused"}"
                            com.example.data.SupabaseSyncStatus.SYNCING -> "Synchronizing Room SQLite database with Supabase database cluster..."
                            else -> "Running in standard offline-first local mode."
                        },
                        fontSize = 11.sp,
                        color = LightSlate,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (lastSyncTime != null) {
                        Text(
                            text = "Last Synced: $lastSyncTime",
                            fontSize = 10.sp,
                            color = LightSlate.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showInstructionsDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = PaavaiGold),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Database Setup SQL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (com.example.data.SupabaseClient.isConfigured()) {
                            Button(
                                onClick = { viewModel.syncWithSupabase() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (syncStatus == com.example.data.SupabaseSyncStatus.ERROR) BloodCrimson else PaavaiGold,
                                    contentColor = DarkCharcoal
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sync_now_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Database setup instructional Dialog modal with SQL query commands
            if (showInstructionsDialog) {
                Dialog(onDismissRequest = { showInstructionsDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f),
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PaavaiGold.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = PaavaiGold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Supabase SQL Schema Setup",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                IconButton(onClick = { showInstructionsDialog = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LightSlate)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Run this SQL script in your Supabase SQL Editor to provision the required PostgreSQL tables, then configure secrets on Google AI Studio's Secrets Panel to start syncing.",
                                fontSize = 10.5.sp,
                                color = LightSlate,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Scrollable Code Block
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    item {
                                        Text(
                                            text = """-- 0. GRANT CREATE PRIVILEGES ON THE PUBLIC SCHEMA (Resolves PG15+ Permission Denied errors)
-- In modern Supabase instances (PostgreSQL 15+), the default permissions of the `public` schema has CREATE revoked for the public.
-- To ensure the `postgres` administrator role has explicit permissions to create tables, we grant USAGE and CREATE:
grant usage on schema public to postgres;
grant create on schema public to postgres;

-- Restore standard schema-level permissions of the public schema to essential database roles
grant all on schema public to postgres;
grant all on schema public to anon;
grant all on schema public to authenticated;
grant all on schema public to service_role;
grant all on schema public to public;

-- Ensure default privileges are correctly granted for any new objects created hereafter
alter default privileges in schema public grant all on tables to postgres, anon, authenticated, service_role;
alter default privileges in schema public grant all on sequences to postgres, anon, authenticated, service_role;
alter default privileges in schema public grant all on functions to postgres, anon, authenticated, service_role;

-- 1. Create Donors Table
create table if not exists donors (
  id bigint generated by default as identity primary key,
  name text not null,
  "registerNumber" text unique not null,
  department text not null,
  year text not null,
  "bloodGroup" text not null,
  "mobileNumber" text not null,
  email text not null,
  location text not null,
  weight double precision not null,
  "lastDonationDate" text not null,
  "userType" text not null default 'Student',
  availability boolean not null default true,
  "totalDonations" integer not null default 0,
  gender text not null default 'Male',
  dob text not null default '2005-01-01',
  address text not null default 'Namakkal, Tamil Nadu',
  "emergencyContact" text not null default '+91 9900998877',
  "profilePhoto" text not null default ''
);

-- 2. Create Blood Requests Table
create table if not exists blood_requests (
  id bigint generated by default as identity primary key,
  "bloodGroup" text not null,
  "unitsRequired" integer not null,
  "hospitalName" text not null,
  "patientName" text not null,
  "urgencyLevel" text not null,
  "contactName" text not null,
  "contactNumber" text not null,
  timestamp bigint not null,
  "isFulfilled" boolean not null default false,
  "simulatedAlertsSent" boolean not null default false
);

-- 3. Create Donation Camps Table
create table if not exists donation_camps (
  id bigint generated by default as identity primary key,
  title text not null,
  date text not null,
  time text not null,
  venue text not null,
  "Description" text not null,
  "registeredCount" integer not null default 0
);

-- 4. Create Donation History Table
create table if not exists donation_history (
  id bigint generated by default as identity primary key,
  "donorName" text not null,
  "donorRegisterNumber" text not null,
  date text not null,
  "bloodGroup" text not null,
  "unitsDonated" integer not null default 1,
  "hospitalName" text not null default 'Paavai Blood Camp'
);

-- 5. Create User Accounts Table
create table if not exists user_accounts (
  email text primary key,
  password text not null,
  name text not null,
  "registerNumber" text not null,
  role text not null,
  department text not null,
  year text not null,
  "bloodGroup" text not null,
  phone text not null
);

-- 6. Disable Row Level Security (RLS) on all tables for easy API sync
alter table donors disable row level security;
alter table blood_requests disable row level security;
alter table donation_camps disable row level security;
alter table donation_history disable row level security;
alter table user_accounts disable row level security;""",
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showInstructionsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = PaavaiGold, contentColor = DarkCharcoal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Got it, setup completed!", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Welcome and Role Switcher Module
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Paavai BloodConnect",
                                style = MaterialTheme.typography.titleMedium,
                                color = LightGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"Every Student Can Save a Life.\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Bell Icon with Unread Badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                    .clickable { showNotificationDialog = true }
                                    .testTag("notification_bell_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications Inbox",
                                    tint = if (unreadCount > 0) PaavaiGold else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(BloodCrimson, CircleShape)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                            .testTag("notification_badge_count")
                                    ) {
                                        Text(
                                            text = unreadCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Active Badge in Welcome card
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = userRole,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Hello, ${profile?.name ?: "Paavai Member"}!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if(profile != null) "Category: ${profile?.userType} • ${profile?.bloodGroup}" else "Complete your profile below to register as a donor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )


                }
            }
        }

        // Quick Impact Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Lives Impacted",
                    value = "$livesSaved",
                    icon = Icons.Default.Favorite,
                    color = BloodCrimson
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Requests",
                    value = "$activeRequests",
                    icon = Icons.Default.Warning,
                    color = if (activeRequests > 0) PaavaiGold else SuccessGreen
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Registered Donors",
                    value = "$totalDonors",
                    icon = Icons.Default.People,
                    color = InfoBlue
                )
            }
        }

        // Donation Impact Section (D3 visual design language translated to native high performance Canvas)
        item {
            DonationImpactCard(histories = histories)
        }

        // --- SPECIFIC ROLE ACTION BENCHES (Architectural Clarity) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("role_desk_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, when(userRole) {
                    "Admin" -> PaavaiGold.copy(alpha = 0.5f)
                    "Volunteer" -> SuccessGreen.copy(alpha = 0.5f)
                    else -> BloodCrimson.copy(alpha = 0.5f)
                })
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when(userRole) {
                                "Admin" -> Icons.Default.AdminPanelSettings
                                "Volunteer" -> Icons.Default.Campaign
                                else -> Icons.Default.ContactPage
                            },
                            contentDescription = null,
                            tint = when(userRole) {
                                "Admin" -> PaavaiGold
                                "Volunteer" -> SuccessGreen
                                else -> BloodCrimson
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when(userRole) {
                                "Admin" -> "ADMINISTRATOR STRATEGIC HUB"
                                "Volunteer" -> "RED CROSS COORDINATOR DESK"
                                else -> "STUDENT DONOR CHECK-IN DESK"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = when(userRole) {
                                "Admin" -> PaavaiGold
                                "Volunteer" -> SuccessGreen
                                else -> BloodCrimson
                            },
                            letterSpacing = 0.5.sp
                        )
                    }

                    Divider(color = CardBorder)

                    when (userRole) {
                        "Student Donor" -> {
                            if (profile != null) {
                                val isEligible = viewModel.checkIfEligible(
                                    profile!!.lastDonationDate,
                                    profile!!.weight,
                                    profile!!.gender,
                                    profile!!.dob
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .border(4.dp, if (isEligible) SuccessGreen else BloodCrimson, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.Block,
                                            contentDescription = null,
                                            tint = if (isEligible) SuccessGreen else BloodCrimson,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEligible) "Ready to Donate Today" else "Clinical Recovery Period Active",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = if (isEligible) "Bring your college identity card and smartphone to the active camp." else "Next recommendation date: ${viewModel.nextEligibleDate(profile!!.lastDonationDate, profile!!.gender)}",
                                            fontSize = 11.sp,
                                            color = LightSlate
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Unregistered Live Profile",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PaavaiGold
                                    )
                                    Text(
                                        text = "Complete your quick clinical enrollment to receive points and redeem level badges.",
                                        fontSize = 11.sp,
                                        color = LightSlate,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = onNavigateToRegister,
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                        modifier = Modifier.fillMaxWidth().testTag("register_donor_desk_btn"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("👉 START CLINICAL REGISTRATION")
                                    }
                                }
                            }
                        }

                        "Volunteer" -> {
                            Text(
                                text = "Liaise between students, clinic stations, and Salem area district hospitals. Dispatch emergency notifications or locate matching candidates instantly.",
                                fontSize = 11.sp,
                                color = LightSlate
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToFinder,
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.weight(1f).testTag("coordinator_finder_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Smart Finder", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onNavigateToRequests,
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                    modifier = Modifier.weight(1f).testTag("coordinator_new_req_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Post Request", fontSize = 11.sp)
                                }
                            }
                        }

                        "Admin" -> {
                            Text(
                                text = "System directory controls activated. You have full root rights to manipulate database records of students, schedule camps, and check AI analytics reports.",
                                fontSize = 11.sp,
                                color = LightSlate
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        viewModel.performAiPrediction()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                    modifier = Modifier.weight(1f).testTag("admin_scarcity_ai_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Predict Scarcity", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // One-Tap SOS Urgent Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("one_tap_sos_card")
                    .clickable { showSosDialog = true },
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BloodCrimson.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BloodCrimson.copy(alpha = 0.12f), DarkCharcoal)
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(BloodCrimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationImportant,
                            contentDescription = "SOS",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EMERGENCY BRIEF: SOS",
                            style = MaterialTheme.typography.titleMedium,
                            color = BloodCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "One-tap request for rapid blood dispatch. System automatically alerts closest compatible student records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightSlate
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Go",
                        tint = BloodCrimson
                    )
                }
            }
        }

        // Interactive Emergency Map & Coordinate Finder
        item {
            EmergencyMapCard()
        }

        // Live alert logs (push notifications simulator)
        if (alertLogs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PaavaiGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Alert Alert",
                                tint = DeepMaroon
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Emergency Alert Dispatches",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepMaroon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        alertLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Divider(color = Color.Black.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }

        // Active Emergency requests Carousel Ticker
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Emergency Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                TextButton(onClick = onNavigateToRequests) {
                    Text("View All", color = DeepMaroon, fontWeight = FontWeight.Bold)
                }
            }
        }

        val unfulfilledRequests = requests.filter { !it.isFulfilled }
        if (unfulfilledRequests.isEmpty()) {
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All Clear",
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active emergency requests!",
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "All student requirements are currently met. Great work!",
                            fontSize = 12.sp,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(unfulfilledRequests.take(2)) { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRequests() },
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (request.urgencyLevel == "Critical") BloodCrimson.copy(alpha = 0.3f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Blood group indicator circle
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (request.urgencyLevel == "Critical") BloodCrimson else DeepMaroon,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = request.bloodGroup,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
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
                                // Badge
                                Surface(
                                    color = if (request.urgencyLevel == "Critical") BloodCrimson else PaavaiGold,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Text(
                                        text = request.urgencyLevel.uppercase(),
                                        color = if (request.urgencyLevel == "Critical") Color.White else TextDark,
                                        fontSize = 9.sp,
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
                            Text(
                                text = "${request.unitsRequired} Units Required",
                                fontSize = 12.sp,
                                color = DeepMaroon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = ">",
                            color = LightSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // Upcoming Paavai Camps Section
        item {
            Text(
                text = "Upcoming Blood Donation Camps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (camps.isEmpty()) {
            item {
                Text(
                    text = "No scheduled camps at the moment.",
                    color = LightSlate,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(camps) { camp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = camp.title,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = camp.venue,
                                        fontSize = 12.sp,
                                        color = LightSlate
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Date",
                                        tint = InfoBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${camp.date} • ${camp.time}",
                                        fontSize = 11.sp,
                                        color = LightSlate
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(WarmSlate, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${camp.registeredCount}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepMaroon,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Registered",
                                        fontSize = 8.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }

                        Text(
                            text = camp.Description,
                            fontSize = 11.sp,
                            color = LightSlate,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        val currentProfile = profile
                        // Register button
                        Button(
                            onClick = {
                                if (currentProfile != null) {
                                    viewModel.registerForCamp(
                                        camp.id,
                                        camp.title,
                                        currentProfile.registerNumber,
                                        currentProfile.name,
                                        currentProfile.bloodGroup
                                    )
                                } else {
                                    onNavigateToRegister()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_camp_btn_${camp.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = "Reg",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (profile != null) "Register Commitment To Donate" else "Register Donor Profile First",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Blood Donation Leaderboard Tab
        item {
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
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Leaderboard",
                                tint = PaavaiGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Paavai Donor Leaderboard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        // Compact Segmented Tabs
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarmSlate)
                                .padding(2.dp)
                        ) {
                            listOf("Overall", "Dept-wise").forEach { name ->
                                val active = (name == "Overall" && selectedGroupTab == "Overall") || (name == "Dept-wise" && selectedGroupTab == "Department-wise")
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else LightSlate,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) DeepMaroon else Color.Transparent)
                                        .clickable { selectedGroupTab = if (name == "Overall") "Overall" else "Department-wise" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedGroupTab == "Overall") {
                        // Top individual donors (calculated dynamically or mapped from donors)
                        val sortedDonors = donors.sortedByDescending { it.totalDonations }.take(5)
                        sortedDonors.forEachIndexed { idx, d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Medals/Ranks
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = when (idx) {
                                                    0 -> PaavaiGold
                                                    1 -> Color(0xFFB0BEC5)
                                                    2 -> Color(0xFFCD7F32)
                                                    else -> WarmSlate
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (idx < 3) Color.DarkGray else LightSlate
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = d.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "${d.department} • ${d.year}",
                                            fontSize = 10.sp,
                                            color = LightSlate
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = RedLightBG,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${d.bloodGroup}",
                                            color = BloodCrimson,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${d.totalDonations} Donations",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepMaroon,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (idx < sortedDonors.size - 1) Divider(color = CardBorder)
                        }
                    } else {
                        // Department aggregates
                        val deptGroups = donors.groupBy { it.department }
                            .mapValues { entry -> entry.value.sumOf { it.totalDonations } }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)

                        deptGroups.forEachIndexed { idx, (dept, total) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${idx + 1}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepMaroon,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = dept,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                }
                                Text(
                                    text = "$total Units",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen,
                                    fontSize = 13.sp
                                )
                            }
                            if (idx < deptGroups.size - 1) Divider(color = CardBorder)
                        }
                    }
                }
            }
        }

        // --- DONOR EDUCATION & FAQ HUB SECTION ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("donor_faq_hub_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = DeepMaroon,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "💡 DONOR EDUCATION & FAQ HUB",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Prepare yourself, bust common myths, and recover safely",
                                fontSize = 10.sp,
                                color = LightSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // FAQ Categories Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmSlate)
                            .padding(2.dp)
                            .border(0.5.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        listOf(
                            Triple("Guidelines", "📋 Pre-Donation", "Pre-Donation Guidelines"),
                            Triple("PostCare", "🍎 Post-Care", "Post-Donation Care"),
                            Triple("Myths", "🛡️ Myths & Facts", "Donation Myths & Facts")
                        ).forEach { (id, label, desc) ->
                            val active = selectedFaqTab == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) DeepMaroon else Color.Transparent)
                                    .clickable {
                                        selectedFaqTab = id
                                        expandedFaqId = null // collapse questions on tab change
                                    }
                                    .padding(vertical = 8.dp),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // FAQ List based on Selected Category
                    val faqList = when (selectedFaqTab) {
                        "Guidelines" -> listOf(
                            FaqItem(
                                "g1",
                                "How should I prepare on the day of donation?",
                                "• Hydrate beforehand: drink at least 500-700ml of water or refreshing fluids.\n• Eat a healthy, low-fat meal 2-3 hours before your appointment. Never donate on an empty stomach!\n• Get a good night's sleep (at least 7-8 hours) before donating.\n• Wear secure sleeves that can easily be rolled up past your elbow.\n• Bring a physical or digital campus ID card."
                            ),
                            FaqItem(
                                "g2",
                                "What are the basic clinical eligibility requirements?",
                                "• Age limit: must be between 18 and 65 years old.\n• Weight limit: must weigh at least 50 kg (110 lbs).\n• Donation interval: minimum of 90 days (12 weeks) for males, and 120 days (16 weeks) for females.\n• Hemoglobin: must be at least 12.5 g/dL (checked at the campus clinic registration)."
                            ),
                            FaqItem(
                                "g3",
                                "Can I donate if I recently got a tattoo or body piercing?",
                                "• Deferral standard: you must wait a minimum of 6 months (180 days) from the date of getting any tattoo, body piercing, cosmetic tattooing, or ear piercing before you are eligible to donate blood."
                            ),
                            FaqItem(
                                "g4",
                                "Are there medications or substances I must avoid?",
                                "• Alcohol: refrain from drinking alcoholic beverages for at least 24 hours prior to donation.\n• Aspirin: avoid taking aspirin or pain relievers for 48 hours beforehand if you plan to donate platelets.\n• Antibiotics: if you are taking antibiotics for an active bacterial infection, you must wait 24-48 hours after your last dose before donating."
                            )
                        )
                        "PostCare" -> listOf(
                            FaqItem(
                                "p1",
                                "How long should I rest immediately after donating?",
                                "• Recovery Lounge: sit or lie down restfully in the campus recovery area for 10-15 minutes.\n• Do not stand up or walk away immediately! Sip refreshments (juice, water) and eat biscuits provided by volunteers to stabilize blood pressure and sugar levels."
                            ),
                            FaqItem(
                                "p2",
                                "What guidelines apply to physical activities?",
                                "• Rest period: avoid heavy lifting, strenuous exercise, gym workouts, or high-intensity athletic activities for the next 24 hours.\n• If you feel lightheaded, sit or lie down with your legs elevated until the feeling passes. Do not ride a vehicle/bike if feeling dizzy."
                            ),
                            FaqItem(
                                "p3",
                                "What should I eat and drink post-donation?",
                                "• Boost Fluids: drink plenty of extra water, coconut water, or fresh juices over the next 24 to 48 hours to replace the volume lost (about 350-450ml).\n• Focus on iron-rich food: consume iron-rich diets such as dates, pomegranates, green leafy vegetables (spinach), pulses, or iron-fortified cereals to assist red cell regeneration."
                            ),
                            FaqItem(
                                "p4",
                                "What if my arm starts bruising or bleeding?",
                                "• Direct Pressure: keep the strip bandage on for at least 4-6 hours. If bleeding occurs from the puncture site, raise your arm straight up and apply firm, direct pressure for 3-5 minutes.\n• Cold Compress: if bruising or swelling develops, apply a cold compress or ice pack wrapped in a clean cloth for 10-15 minutes periodically during the first 24 hours."
                            )
                        )
                        else -> listOf(
                            FaqItem(
                                "m1",
                                "Myth: Donating blood makes you physically weak or sick.",
                                "• FACT: Incredibly false! Only about 1 unit (350-450ml) is taken. Your body holds around 5 liters. The fluid portion is fully replaced within 24 hours, and red blood cells are safely regenerated in a few weeks. Donating actually prompts bone marrow to produce fresh, healthy blood cells."
                            ),
                            FaqItem(
                                "m2",
                                "Myth: Blood donation is extremely painful and takes hours.",
                                "• FACT: The actual needle prick lasts for only a single second (like a tiny ant bite). The entire blood drawing process takes just 8 to 10 minutes. The remaining time is simply registration and quick post-donation resting."
                            ),
                            FaqItem(
                                "m3",
                                "Myth: I can contract infections or diseases from donating.",
                                "• FACT: The process is absolutely sterile and safe. Every single needle, syringe, and collection bag is brand new, sterile, single-use, and disposed of immediately after your donation. There is zero risk of contracting any virus or infection."
                            ),
                            FaqItem(
                                "m4",
                                "Myth: People on medication or with high blood pressure can't donate.",
                                "• FACT: If your blood pressure is stable and well-controlled with standard medications, you are perfectly clear to donate! Many common blood pressure, thyroid, and asthma medications do not disqualify you. Ask the campus registration doctor for certification."
                            )
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        faqList.forEach { item ->
                            val isExpanded = expandedFaqId == item.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmSlate)
                                    .border(0.5.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        expandedFaqId = if (isExpanded) null else item.id
                                    }
                                    .padding(12.dp)
                                    .testTag("faq_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.question,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = DeepMaroon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = CardBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.answer,
                                        fontSize = 11.sp,
                                        color = LightSlate,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SOS Request Dialog Trigger
    if (showSosDialog) {
        var bloodGroup by remember { mutableStateOf("O-") }
        var patientName by remember { mutableStateOf("") }
        var hospitalName by remember { mutableStateOf("Paavai Multi Speciality Hospital, Namakkal") }
        var unitsNeeded by remember { mutableStateOf("1") }
        var contactName by remember { mutableStateOf(profile?.name ?: "") }
        var contactNumber by remember { mutableStateOf(profile?.mobileNumber ?: "") }
        var errorText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showSosDialog = false }) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BloodCrimson, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Quick",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTANT ONE-TAP SOS",
                            style = MaterialTheme.typography.titleMedium,
                            color = BloodCrimson,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Instantly broadcast a highly critical, immediate need. Our system will contact nearby student clusters.",
                        fontSize = 11.sp,
                        color = LightSlate
                    )

                    Divider()

                    // Blood group dropdown simulator
                    Text("Blood Group Needed:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("O-", "O+", "A-", "A+", "B+", "AB+").forEach { bg ->
                            val active = bg == bloodGroup
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) BloodCrimson else WarmSlate)
                                    .clickable { bloodGroup = bg }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bg,
                                    fontSize = 12.sp,
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
                        modifier = Modifier.fillMaxWidth().testTag("sos_patient_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital Name & Branch") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unitsNeeded,
                            onValueChange = { unitsNeeded = it },
                            label = { Text("Units") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text("Contact Mobile") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    if (errorText.isNotEmpty()) {
                        Text(text = errorText, color = BloodCrimson, fontSize = 11.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSosDialog = false }) {
                            Text("Cancel", color = LightSlate)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (patientName.trim().isEmpty() || contactNumber.trim().isEmpty()) {
                                    errorText = "Please fill in patient name and mobile number."
                                    return@Button
                                }
                                val u = unitsNeeded.toIntOrNull() ?: 1
                                viewModel.triggerSOS(
                                    bloodGroup = bloodGroup,
                                    patientName = patientName,
                                    hospitalName = hospitalName,
                                    units = u,
                                    contactName = contactName.ifEmpty { "Emergency Contact" },
                                    contactPhone = contactNumber
                                )
                                showSosDialog = false
                                onNavigateToFinder() // Send user to finder to see AI matches immediately!
                            },
                            modifier = Modifier.testTag("sos_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson)
                        ) {
                            Text("BROADCAST SOS ALERT")
                        }
                    }
                }
            }
        }
    }

    // Interactive Notification Inbox Dialog Modal
    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 12.dp)
                    .testTag("notifications_inbox_dialog"),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BloodCrimson,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Donor Alerts Inbox",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showNotificationDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = LightSlate)
                        }
                    }

                    Divider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    if (activeNotifications.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeNotifications.size} received alerts",
                                color = LightSlate,
                                fontSize = 11.sp
                            )
                            TextButton(
                                onClick = { viewModel.markAllNotificationsAsRead() },
                                colors = ButtonDefaults.textButtonColors(contentColor = SuccessGreen)
                            ) {
                                Text("Mark all read", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(activeNotifications) { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("notification_item_${alert.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (alert.isRead) WarmSlate.copy(alpha = 0.5f) else WarmSlate
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (alert.isRead) Color.Transparent else BloodCrimson.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    color = if (alert.urgencyLevel == "Critical") BloodCrimson else PaavaiGold,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = alert.urgencyLevel.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Match Group: ${alert.bloodGroup}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark
                                                )
                                            }

                                            Text(
                                                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(alert.timestamp)),
                                                color = LightSlate,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = alert.content,
                                            color = TextDark,
                                            fontSize = 12.sp,
                                            fontWeight = if (alert.isRead) FontWeight.Normal else FontWeight.Medium
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Commit Action
                                                if (!alert.isRead) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.markNotificationAsRead(alert.id)
                                                            showNotificationDialog = false
                                                            onNavigateToRequests()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp).testTag("alert_action_commit_${alert.id}")
                                                    ) {
                                                        Text("COMMIT NOW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                    
                                                    OutlinedButton(
                                                        onClick = { viewModel.markNotificationAsRead(alert.id) },
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp).testTag("alert_read_btn_${alert.id}")
                                                    ) {
                                                        Text("Mark Read", fontSize = 9.sp, color = TextDark)
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteNotification(alert.id) },
                                                modifier = Modifier.size(24.dp).testTag("alert_delete_btn_${alert.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Alert",
                                                    tint = LightSlate.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.clearNotifications() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear all alerts", color = LightSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Namakkal Sector Clear",
                                color = TextDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your blood group (${profile?.bloodGroup ?: "O-"}) is on stand-by. No active emergency broadcasts match your vicinity profile. Thank you for staying ready!",
                                color = LightSlate,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.height(105.dp),
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
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                        )
                    }
                }
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark,
                    fontSize = 20.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = LightSlate,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==========================================
// D3-Inspired Donation Impact Dashboard Components
// ==========================================

data class MonthlyImpactBaseline(
    val monthCode: String,
    val monthLabel: String,
    val baseDonations: Int,
    val labelShort: String
)

data class ChartPoint(
    val monthCode: String,
    val monthLabel: String,
    val labelShort: String,
    val donations: Int,
    val units: Int,
    val livesSaved: Int
)

@Composable
fun DonationImpactCard(histories: List<DonationHistory>) {
    var isBarChart by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(10) } // May 2026 preselected

    val monthsList = remember {
        listOf(
            MonthlyImpactBaseline("2025-07", "July 2025", 22, "Jul"),
            MonthlyImpactBaseline("2025-08", "August 2025", 25, "Aug"),
            MonthlyImpactBaseline("2025-09", "September 2025", 30, "Sep"),
            MonthlyImpactBaseline("2025-10", "October 2025", 38, "Oct"),
            MonthlyImpactBaseline("2025-11", "November 2025", 45, "Nov"),
            MonthlyImpactBaseline("2025-12", "December 2025", 28, "Dec"),
            MonthlyImpactBaseline("2026-01", "January 2026", 32, "Jan"),
            MonthlyImpactBaseline("2026-02", "February 2026", 40, "Feb"),
            MonthlyImpactBaseline("2026-03", "March 2026", 48, "Mar"),
            MonthlyImpactBaseline("2026-04", "April 2026", 55, "Apr"),
            MonthlyImpactBaseline("2026-05", "May 2026", 62, "May"),
            MonthlyImpactBaseline("2026-06", "June 2026", 15, "Jun")
        )
    }

    val finalData = remember(histories) {
        monthsList.map { month ->
            val matchedHistories = histories.filter { it.date.startsWith(month.monthCode) }
            val additionalDonationsCount = matchedHistories.size
            val additionalUnits = matchedHistories.sumOf { it.unitsDonated }
            
            val totalCount = month.baseDonations + additionalDonationsCount
            val totalUnits = month.baseDonations + additionalUnits
            val livesSaved = totalUnits * 3
            
            ChartPoint(
                monthCode = month.monthCode,
                monthLabel = month.monthLabel,
                labelShort = month.labelShort,
                donations = totalCount,
                units = totalUnits,
                livesSaved = livesSaved
            )
        }
    }

    val maxVal = remember(finalData) {
        (finalData.maxOfOrNull { it.donations } ?: 60) + 12
    }

    val totalAnnualUnits = remember(finalData) {
        finalData.sumOf { it.units }
    }
    
    val totalAnnualDonations = remember(finalData) {
        finalData.sumOf { it.donations }
    }

    val activePoint = finalData.getOrNull(selectedIndex) ?: finalData.last()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("donation_impact_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BloodCrimson, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PAAVAI DONATION IMPACT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepMaroon,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Annual Community Velocity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                // D3 Spline or D3 Bar render Toggle
                Row(
                    modifier = Modifier
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { isBarChart = false },
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (!isBarChart) Color.White else Color.Transparent,
                                CircleShape
                            ).testTag("select_curve_chart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Spline Chart",
                            tint = if (!isBarChart) DeepMaroon else LightSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { isBarChart = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isBarChart) Color.White else Color.Transparent,
                                CircleShape
                            ).testTag("select_bar_chart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bar Chart",
                            tint = if (isBarChart) DeepMaroon else LightSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "Total cumulative blood volume and hospital dispatches gathered by student and faculty bodies across campus zones since July 2025.",
                fontSize = 11.sp,
                color = LightSlate,
                lineHeight = 15.sp
            )

            // Dynamic Canvas View (Native translation of D3 area/interpolated charts)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(0.5.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Actual Canvas Chart drawing
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .pointerInput(finalData) {
                                detectTapGestures { offset ->
                                    val count = finalData.size
                                    if (count > 1) {
                                        val stepX = size.width / (count - 1).toFloat()
                                        val idx = (offset.x / stepX)
                                            .plus(0.5f)
                                            .toInt()
                                            .coerceIn(0, count - 1)
                                        selectedIndex = idx
                                    }
                                }
                            }
                            .testTag("donation_d3_canvas")
                    ) {
                        val width = size.width
                        val height = size.height
                        val count = finalData.size
                        if (count > 0) {
                            val stepX = width / (count - 1).toFloat()

                            // Reference guidelines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val yCoord = height * (i / gridLines.toFloat())
                                drawLine(
                                    color = Color(0xFFF3F4F6),
                                    strokeWidth = 1f,
                                    start = Offset(0f, yCoord),
                                    end = Offset(width, yCoord)
                                )
                            }

                            val points = finalData.mapIndexed { idx, pt ->
                                val x = idx * stepX
                                val y = height - (pt.donations.toFloat() / maxVal.toFloat() * height)
                                Offset(x, y)
                            }

                            if (isBarChart) {
                                // Dynamic column rendered
                                val barWidth = (stepX * 0.55f).coerceAtLeast(10f)
                                points.forEachIndexed { index, pt ->
                                    val barHeight = (height - pt.y).coerceAtLeast(4f)
                                    val isSelected = index == selectedIndex
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = if (isSelected) {
                                                listOf(DeepMaroon, BloodCrimson)
                                            } else {
                                                listOf(BloodCrimson.copy(alpha = 0.5f), BloodCrimson.copy(alpha = 0.2f))
                                            }
                                        ),
                                        topLeft = Offset(pt.x - barWidth / 2f, pt.y),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )

                                    if (isSelected) {
                                        drawRoundRect(
                                            color = LightGold,
                                            topLeft = Offset(pt.x - barWidth / 2f - 1.dp.toPx(), pt.y - 1.dp.toPx()),
                                            size = Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                }
                            } else {
                                // Smooth area Bezier spline curve representation
                                val path = Path()
                                val areaPath = Path()

                                path.moveTo(points[0].x, points[0].y)
                                areaPath.moveTo(points[0].x, height)
                                areaPath.lineTo(points[0].x, points[0].y)

                                for (i in 1 until points.size) {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    val cp1X = prev.x + (curr.x - prev.x) / 2f
                                    val cp1Y = prev.y
                                    val cp2X = prev.x + (curr.x - prev.x) / 2f
                                    val cp2Y = curr.y

                                    path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
                                    areaPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
                                }

                                areaPath.lineTo(points.last().x, height)
                                areaPath.close()

                                // Area path gradient below spline curve
                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            BloodCrimson.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )

                                // Actual spline border curve
                                drawPath(
                                    path = path,
                                    color = DeepMaroon,
                                    style = Stroke(
                                        width = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Individual nodes
                                points.forEachIndexed { index, pt ->
                                    val isSelected = index == selectedIndex
                                    if (isSelected) {
                                        drawCircle(
                                            color = LightGold,
                                            radius = 7.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = DeepMaroon,
                                            radius = 4.dp.toPx(),
                                            center = pt
                                        )
                                    } else {
                                        drawCircle(
                                            color = BloodCrimson.copy(alpha = 0.8f),
                                            radius = 3.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom list values
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        finalData.forEachIndexed { index, pt ->
                            val isSelected = index == selectedIndex
                            Text(
                                text = pt.labelShort,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DeepMaroon else LightSlate,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedIndex = index }
                            )
                        }
                    }
                }
            }

            // Interactive focus panel below chart displaying exact statistics for the selected month
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Focus: ${activePoint.monthLabel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RedLightBG
                        ) {
                            Text(
                                text = "${activePoint.donations} Blood Bags",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepMaroon,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat Column 1
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Units Donated", fontSize = 9.sp, color = LightSlate)
                            Text(
                                text = "${activePoint.units} Units",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        // Stat Column 2
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Potential Lives Saved", fontSize = 9.sp, color = LightSlate)
                            Text(
                                text = "~${activePoint.livesSaved} Lives",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }

                    Divider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Camp/Vibe Campaign Details
                    Text(
                        text = when(activePoint.monthCode) {
                            "2025-07" -> "☀️ Summer camp drives across Paavai engineering campus sectors."
                            "2025-08" -> "🎒 Orientation blood registry recruitment for first year students."
                            "2025-09" -> "🍂 Red Cross national donation awareness seminar."
                            "2025-10" -> "🎃 Autumn auxiliary drives & holiday standby rosters preparation."
                            "2025-11" -> "🏅 Grand Stadium donation campsite event overachieved with Sneha Srinivasan."
                            "2025-12" -> "❄️ Winter break emergency supply networks maintained."
                            "2026-01" -> "⚡ Semestral pledge campaign - active faculty contributions leading."
                            "2026-02" -> "❤️ Valentine's week 'Share Love, Save Lives' campaign with Saran Ramesh."
                            "2026-03" -> "🌱 Technical Symposia matching blood donation booth."
                            "2026-04" -> "🌸 Annual Paavai Youth Red Cross elite awards ceremony with Ramesh Kumar."
                            "2026-05" -> "🎓 Farewell session outgoing student elite blood registry camp."
                            "2026-06" -> "🔄 Current Standby emergency readiness & summer backups."
                            else -> "🩸 Community solidarity drive saving lives."
                        },
                        fontSize = 11.sp,
                        color = LightSlate,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Overall annual goal completion status
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Paavai Annual 500-Unit Milestone",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "$totalAnnualUnits / 500 Units (${(totalAnnualUnits * 100 / 500)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepMaroon
                    )
                }

                LinearProgressIndicator(
                    progress = (totalAnnualUnits / 500.0f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = BloodCrimson,
                    trackColor = CardBorder
                )

                Text(
                    text = "🌟 You have collected $totalAnnualDonations donations over the last year! The community has saved ~${totalAnnualUnits * 3} lives.",
                    fontSize = 10.sp,
                    color = LightSlate,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class FaqItem(val id: String, val question: String, val answer: String)


// === EMERGENCY MAP MODEL ===
private data class HospitalLocation(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val type: String, // "Hospital" or "Blood Bank"
    val dist: String,
    val phone: String,
    val address: String,
    val availabilityStatus: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyMapCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Dataset of Emergency Facilities with real Coordinates in Namakkal / Salem, TN region
    val locations = remember {
        listOf(
            HospitalLocation(
                id = "paavai_ms",
                name = "Paavai Multi Speciality Hospital",
                lat = 11.3790,
                lng = 78.1504,
                type = "Hospital",
                dist = "0.8 km",
                phone = "+91 4286 243038",
                address = "NH-44, Paavai Educational Institutions Campus, Namakkal, TN",
                availabilityStatus = "24/7 ICU & Active Blood Storage"
            ),
            HospitalLocation(
                id = "namakkal_govt",
                name = "Namakkal Government Headquarters Hospital",
                lat = 11.2236,
                lng = 78.1633,
                type = "Hospital",
                dist = "12.5 km",
                phone = "+91 4286 231201",
                address = "Government Hospital Road, Namakkal, Tamil Nadu",
                availabilityStatus = "Emergency ICU & Rotary Blood Center"
            ),
            HospitalLocation(
                id = "salem_govt",
                name = "Salem Government Mohan Kumaramangalam Hospital",
                lat = 11.6660,
                lng = 78.1402,
                type = "Hospital",
                dist = "38.0 km",
                phone = "+91 427 221 1666",
                address = "Collectorate Road, Salem, Tamil Nadu",
                availabilityStatus = "Major Regional Blood Bank & Trauma Center"
            ),
            HospitalLocation(
                id = "namakkal_bb",
                name = "Namakkal District Blood Bank (Government HQ)",
                lat = 11.2240,
                lng = 78.1628,
                type = "Blood Bank",
                dist = "12.6 km",
                phone = "+91 4286 231201",
                address = "GH Complex, Trichy Road, Namakkal, TN",
                availabilityStatus = "All Groups Available - Government Stocked"
            ),
            HospitalLocation(
                id = "rotary_bb",
                name = "Rotary Club Trust Blood Bank",
                lat = 11.2185,
                lng = 78.1560,
                type = "Blood Bank",
                dist = "13.1 km",
                phone = "+91 4286 226999",
                address = "Senniappa Towers, Mohanur Road, Namakkal, TN",
                availabilityStatus = "High Reserves available - 24/7 Dispatch"
            ),
            HospitalLocation(
                id = "thangam_h",
                name = "Thangam Hospital & Blood Bank",
                lat = 11.2295,
                lng = 78.1691,
                type = "Hospital",
                dist = "11.2 km",
                phone = "+91 4286 227707",
                address = "Mullai Nagar, Namakkal, Tamil Nadu",
                availabilityStatus = "Active Cardiac Unit & Platelets Storage"
            ),
            HospitalLocation(
                id = "paavai_campus_bb",
                name = "Paavai College Emergency Dispatch Desk",
                lat = 11.3789,
                lng = 78.1512,
                type = "Blood Bank",
                dist = "0.1 km",
                phone = "+91 9900998877",
                address = "Admin Block Wing B, Paavai Engineering Campus, TN",
                availabilityStatus = "Red Cross Student Coordinator Led"
            )
        )
    }

    // State bindings
    var filterType by remember { mutableStateOf("All") } // "All", "Hospital", "Blood Bank"
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<HospitalLocation?>(locations.first()) }
    var zoomLevel by remember { mutableStateOf(1.2f) }
    var panOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Active filtered subset matches search + type conditions
    val filteredList = remember(searchQuery, filterType) {
        locations.filter { loc ->
            val typeMatch = filterType == "All" || loc.type == filterType
            val textMatch = searchQuery.isEmpty() ||
                    loc.name.contains(searchQuery, ignoreCase = true) ||
                    loc.address.contains(searchQuery, ignoreCase = true)
            typeMatch && textMatch
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("emergency_map_and_coordinates_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Title Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = BloodCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Emergency Map Finder",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 15.sp,
                            modifier = Modifier.testTag("emergency_map_title")
                        )
                        Text(
                            text = "Nearby hospitals and blood bank locations with coordinates",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                    }
                }
                
                Surface(
                    color = SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "LIVE DISTANCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search hospitals, blood banks...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LightSlate,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BloodCrimson,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter FilterChips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Hospital", "Blood Bank").forEach { category ->
                    val isSelected = filterType == category
                    val label = when (category) {
                        "Hospital" -> "🏥 Hospitals"
                        "Blood Bank" -> "🩸 Blood Banks"
                        else -> "🌐 All Places"
                    }
                    
                    Surface(
                        modifier = Modifier
                            .clickable { filterType = category }
                            .testTag("filter_chip_$category"),
                        color = if (isSelected) BloodCrimson else WarmSlate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else CardBorder)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else LightSlate,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // THE INTERACTIVE VECTOR MAP CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1115))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            ) {
                var canvasWidth by remember { mutableStateOf(800f) }
                var canvasHeight by remember { mutableStateOf(800f) }

                // The Map Canvas rendering all routes, grids, center points, and marker points
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredList, zoomLevel, panOffset) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panOffset = Offset(panOffset.x + dragAmount.x, panOffset.y + dragAmount.y)
                            }
                        }
                        .pointerInput(filteredList, zoomLevel, panOffset) {
                            detectTapGestures { tapOffset ->
                                var closest: HospitalLocation? = null
                                var closestDist = Float.MAX_VALUE
                                filteredList.forEach { loc ->
                                    val pt = getOffsetForCoordinates(
                                        lat = loc.lat,
                                        lng = loc.lng,
                                        width = canvasWidth,
                                        height = canvasHeight,
                                        zoom = zoomLevel,
                                        pan = panOffset
                                    )
                                    val screenDistance = (tapOffset - pt).getDistance()
                                    if (screenDistance < 40f && screenDistance < closestDist) {
                                        closest = loc
                                        closestDist = screenDistance
                                    }
                                }
                                if (closest != null) {
                                    selectedLocation = closest
                                }
                            }
                        }
                        .testTag("interactive_emergency_vector_map")
                ) {
                    canvasWidth = size.width
                    canvasHeight = size.height

                    // 1. Draw elegant tactical slate radar grids as background
                    val gridSpace = 40f
                    val gridColor = Color(0xFF1F242E)
                    for (x in 0..size.width.toInt() step gridSpace.toInt()) {
                        drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                    }
                    for (y in 0..size.height.toInt() step gridSpace.toInt()) {
                        drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                    }

                    // 2. Draw national routes & college crossroad paths
                    val referenceCenter = getOffsetForCoordinates(11.3789, 78.1512, size.width, size.height, zoomLevel, panOffset)
                    
                    // Draw NH-44 highway bypassing campus (North-South route)
                    val routePath1 = Path().apply {
                        moveTo(referenceCenter.x - 100f * zoomLevel, 0f)
                        quadraticTo(referenceCenter.x - 80f * zoomLevel, size.height / 2, referenceCenter.x - 120f * zoomLevel, size.height)
                    }
                    drawPath(routePath1, Color(0xFF2C3242), style = Stroke(width = 8f * zoomLevel, cap = StrokeCap.Round))
                    drawPath(routePath1, Color(0xFFE0E0E0).copy(alpha = 0.15f), style = Stroke(width = 2f * zoomLevel, cap = StrokeCap.Round))

                    // Draw Salem-Namakkal State Link Hwy (Diagonal route)
                    val routePath2 = Path().apply {
                        moveTo(0f, referenceCenter.y + 40f * zoomLevel)
                        lineTo(size.width, referenceCenter.y - 120f * zoomLevel)
                    }
                    drawPath(routePath2, Color(0xFF332727), style = Stroke(width = 6f * zoomLevel, cap = StrokeCap.Round))

                    // 3. Draw Concentric Distance Coordinates (Tactical rings indicating distances)
                    val scaleFactor = 12f * zoomLevel
                    val distances = listOf(
                        Triple(1.0, "1 km Range", Color.White.copy(alpha = 0.08f)),
                        Triple(12.5, "12 km Town Limit", Color(0xFFD32F2F).copy(alpha = 0.05f)),
                        Triple(38.0, "District Boundary (38 km)", Color(0xFFFFA000).copy(alpha = 0.03f))
                    )
                    distances.forEach { (distKm, label, color) ->
                        val radiusPixels = (distKm * scaleFactor).toFloat()
                        drawCircle(
                            color = color,
                            radius = radiusPixels,
                            center = referenceCenter,
                            style = Stroke(width = 1.5f, miter = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        )
                    }

                    // 4. Draw Campus Location Hub pulsing star / circle
                    drawCircle(Color(0xFF00E5FF).copy(alpha = 0.12f), radius = 24f * zoomLevel, center = referenceCenter)
                    drawCircle(Color(0xFF00E5FF).copy(alpha = 0.3f), radius = 12f * zoomLevel, center = referenceCenter)
                    drawCircle(Color(0xFF00E5FF), radius = 4f * zoomLevel, center = referenceCenter)

                    // 5. Drawing Facility Location Markers
                    filteredList.forEach { loc ->
                        val locPt = getOffsetForCoordinates(loc.lat, loc.lng, size.width, size.height, zoomLevel, panOffset)
                        val isSelected = selectedLocation?.id == loc.id
                        val markerColor = if (loc.type == "Hospital") BloodCrimson else PaavaiGold

                        // Pulse animation ring if highlighted
                        if (isSelected) {
                            drawCircle(
                                color = markerColor.copy(alpha = 0.2f),
                                radius = 18f * zoomLevel,
                                center = locPt
                            )
                            drawCircle(
                                color = markerColor.copy(alpha = 0.4f),
                                radius = 11f * zoomLevel,
                                center = locPt
                            )
                        }

                        // Drawing pin base circle
                        drawCircle(
                            color = markerColor,
                            radius = 6.5f * zoomLevel,
                            center = locPt
                        )
                        // Tiny core inner bead
                        drawCircle(
                            color = Color.White,
                            radius = 2f * zoomLevel,
                            center = locPt
                        )
                    }
                }

                // GPS Indicator Overlays top left
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Campus GPS Ref: 11.3789°N, 78.1512°E",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Interactive zoom controllers overlay column (Top Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoom In button
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(4.5f) }
                            .testTag("zoom_in_btn"),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Zoom Out button
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.4f) }
                            .testTag("zoom_out_btn"),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Reset Center calibration button
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clickable {
                                panOffset = Offset(0f, 0f)
                                zoomLevel = 1.2f
                                selectedLocation = locations.firstOrNull()
                                Toast.makeText(context, "Centered map view on Paavai College campus", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("recenter_map_btn"),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter", tint = Color.Cyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Quick Tip Toast indicator at bottom left of map box
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Drag canvas to explore • Tap points directly",
                        fontSize = 8.sp,
                        color = LightSlate,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // HORIZONTAL CAROUSEL OF FILTERED PLACES FOR EASY TAP SELECTING
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_locations_carousel"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { loc ->
                    val isSelected = selectedLocation?.id == loc.id
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable {
                                selectedLocation = loc
                                // Auto pull viewport close to the selected spot
                                val targetLatOffset = loc.lat - 11.3789
                                val targetLngOffset = loc.lng - 78.1512
                                val scale = 12f * zoomLevel
                                val dragX = -(targetLngOffset * 109.0 * scale).toFloat()
                                val dragY = (targetLatOffset * 111.0 * scale).toFloat()
                                panOffset = Offset(dragX, dragY)
                            }
                            .testTag("location_card_${loc.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) WarmSlate.copy(alpha = 1.0f) else DarkCharcoal.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) BloodCrimson else CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (loc.type == "Hospital") "🏥 Hosp" else "🩸 Bank",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (loc.type == "Hospital") BloodCrimson else PaavaiGold
                                )
                                Text(
                                    text = loc.dist,
                                    fontSize = 8.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = loc.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                modifier = Modifier.testTag("loc_title_${loc.id}")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${loc.lat}, ${loc.lng}",
                                fontSize = 8.sp,
                                color = LightSlate,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SHOW DETAILED SELECTED PLACE DOSSIER & REAL INTEGRATION ACTION BAR
            selectedLocation?.let { loc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_facility_dossier"),
                    colors = CardDefaults.cardColors(containerColor = WarmSlate),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = loc.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.testTag("dossier_name")
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = BloodCrimson,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${loc.lat}° N, ${loc.lng}° E (${loc.dist} away)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightGold
                                    )
                                }
                            }
                            
                            Surface(
                                color = (if (loc.type == "Hospital") BloodCrimson else PaavaiGold).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = loc.type.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (loc.type == "Hospital") BloodCrimson else PaavaiGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = loc.address,
                            fontSize = 11.sp,
                            color = LightSlate,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCharcoal.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalActivity,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Status: ${loc.availabilityStatus}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // REAL FUNCTIONAL INTEGRATION ACTION BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dial hotlines button
                            FilledTonalButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loc.phone}"))
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Problem triggering dialer application.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dossier_call_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DarkCharcoal,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call Facility", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Open coordinates directly on maps application
                            Button(
                                onClick = {
                                    val geoUri = "geo:${loc.lat},${loc.lng}?q=${loc.lat},${loc.lng}(${Uri.encode(loc.name)})"
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        // Fallback web redirect inside Google Maps coordinate search
                                        val webMapUri = "https://www.google.com/maps/search/?api=1&query=${loc.lat},${loc.lng}"
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webMapUri))
                                        context.startActivity(webIntent)
                                    }
                                    Toast.makeText(context, "Launching Navigator for ${loc.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("dossier_route_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson)
                            ) {
                                Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Route GPS (Coordinates)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Share Coordinates / Copy coordinates action row
                        OutlinedButton(
                            onClick = {
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Hospital Coordinates", "${loc.lat}, ${loc.lng}")
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Coordinates of ${loc.name} copied to Clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("dossier_copy_coords_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LightGold),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Geolocation Coordinates (${loc.lat}, ${loc.lng})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Coordinate mapping helper calculation for plotting on simulated Canvas viewport
private fun getOffsetForCoordinates(
    lat: Double,
    lng: Double,
    width: Float,
    height: Float,
    zoom: Float,
    pan: Offset
): Offset {
    val deltaLat = lat - 11.3789
    val deltaLng = lng - 78.1512
    val scale = 12f * zoom // Pixels per km
    
    // x coordinate maps horizontally based on longitude scale (East-West)
    val screenX = (width / 2) + (deltaLng * 109.0 * scale).toFloat() + pan.x
    // y coordinate maps vertically based on latitude scale (North-South, invert coordinate space)
    val screenY = (height / 2) - (deltaLat * 111.0 * scale).toFloat() + pan.y
    
    return Offset(screenX, screenY)
}

