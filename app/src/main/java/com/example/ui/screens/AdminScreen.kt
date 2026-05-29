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

    var activePredictionTab by remember { mutableStateOf(false) }

    // Mapped analytics metrics
    val totalCount = donors.size
    val activeCount = requests.count { !it.isFulfilled }
    val campsCount = camps.size

    val bloodDistribution = donors.groupBy { it.bloodGroup }
        .mapValues { it.value.size }

    val bloodGroupsMax = bloodDistribution.values.maxOrNull() ?: 1

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Heading
        item {
            Column {
                Text(
                    text = "Paavai College Admin Panel",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
                Text(
                    text = "Verify donor registries, monitor stock distribution trends, and consult forecast audits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate
                )
            }
        }

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
                        // Horizontal bar chart representation
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
                                // Label
                                Text(
                                    text = bg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.width(36.dp)
                                )

                                // Custom solid block bar
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
                                            .fillMaxWidth(barPercentage.coerceAtLeast(0.04f)) // Minimum width so single elements show on chart
                                            .background(
                                                if (bg == "O-") BloodCrimson else DeepMaroon
                                            )
                                    )
                                }

                                // Count
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
                                    .background(Color(0xFFE9D5FF), CircleShape),
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
                                    color = Color(0xFF581C87),
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
                        color = Color(0xFF6B21A8),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forecast compile button
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
                        Text("Simulate AI 30-Day Scarcity Forecast", fontWeight = FontWeight.Bold)
                    }

                    // Report outputs
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

        // Verified list of overall registry students
        item {
            Text(
                text = "Manage Verified Student Registry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = donor.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text(text = "${donor.department} • Reg: ${donor.registerNumber}", fontSize = 11.sp, color = LightSlate)
                        }

                        // Actions: Delete or Edit indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(DeepMaroon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = donor.bloodGroup,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
