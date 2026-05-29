package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Donor
import com.example.ui.theme.*
import com.example.viewmodel.BloodConnectViewModel

// Department Stats helper class
data class DepartmentStats(
    val name: String,
    val totalDonations: Int,
    val totalPoints: Int,
    val memberCount: Int
)

// Badge Milestone helper class
data class BadgeMilestone(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isEarned: Boolean,
    val progressMessage: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    viewModel: BloodConnectViewModel,
    onNavigateToRegister: () -> Unit
) {
    val registeredProfile by viewModel.registeredProfile.collectAsState()
    val donors by viewModel.allDonors.collectAsState()
    val history by viewModel.allHistory.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Badges & My Status, 1: Overall Leaderboard, 2: Department Leaderboard

    // Calculate donor points and badges dynamically to ensure sync
    val currentDonor = registeredProfile

    // Points calculation logic:
    // 100 points per successful physical donation
    // +50 points bonus if donor blood group belongs to a rare type (O-, A-, B-, AB-)
    fun calculatePoints(donor: Donor): Int {
        val basePoints = donor.totalDonations * 100
        val isRareType = donor.bloodGroup == "O-" || donor.bloodGroup == "A-" || 
                         donor.bloodGroup == "B-" || donor.bloodGroup == "AB-"
        val rareBonus = if (isRareType && donor.totalDonations > 0) 50 else 0
        // Additional milestone bonus points
        val milestoneBonus = when {
            donor.totalDonations >= 5 -> 200
            donor.totalDonations >= 3 -> 100
            else -> 0
        }
        return basePoints + rareBonus + milestoneBonus
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate),
        topBar = {
            Column(
                modifier = Modifier
                    .background(WarmSlate)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Gamified Rewards Cell",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
                Text(
                    text = "Track points, earn exclusive badges, and help your department rise on the leaderboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Visual Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCharcoal)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("My Achievements", "Top Donors", "Departments")
                    tabs.forEachIndexed { index, label ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DeepMaroon else Color.Transparent)
                                .clickable { activeTab = index }
                                .padding(vertical = 10.dp)
                                .testTag("rewards_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else LightSlate,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = WarmSlate
    ) { innerPadding ->
        
        if (currentDonor == null) {
            // Unregistered state banner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(WarmSlate),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(BloodCrimson.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Locked rewards",
                                tint = BloodCrimson,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Text(
                            text = "Awaiting Onboarding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Register as a prospective donor in our digital registry to unlock point system achievements, start collecting unique service badges, and gain points for Paavai Institutions department rankings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { onNavigateToRegister() },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rewards_register_now_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Go to card register",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Go to Digital Register", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Live Rewards Content
            val myPoints = calculatePoints(currentDonor)
            val myTotalDonations = currentDonor.totalDonations

            // Calculate level progress
            val levelTier = when {
                myPoints >= 700 -> "Giga Lifesaver (Platinum)"
                myPoints >= 400 -> "Golden Vanguard (Gold)"
                myPoints >= 200 -> "Crimson Guardian (Silver)"
                myPoints >= 100 -> "Active Rescuer (Bronze)"
                else -> "Novice Cadet"
            }

            val nextTierTarget = when {
                myPoints < 100 -> 100
                myPoints < 200 -> 200
                myPoints < 400 -> 400
                myPoints < 700 -> 700
                else -> 0
            }

            val progressRatio = if (nextTierTarget > 0) {
                myPoints.toFloat() / nextTierTarget.toFloat()
            } else {
                1.0f
            }

            // Define badges dynamically based on user stats
            val hasVolunteerHist = history.any { 
                it.donorRegisterNumber == currentDonor.registerNumber && 
                it.hospitalName.contains("Volunteer") 
            }
            
            val isRareGroup = currentDonor.bloodGroup == "O-" || currentDonor.bloodGroup == "A-" || 
                              currentDonor.bloodGroup == "B-" || currentDonor.bloodGroup == "AB-"

            val badgesList = listOf(
                BadgeMilestone(
                    title = "First Time Donor",
                    description = "Awarded upon completing your initial verified blood donation.",
                    icon = Icons.Default.Verified,
                    accentColor = SuccessGreen,
                    isEarned = myTotalDonations >= 1,
                    progressMessage = if (myTotalDonations >= 1) "Completed!" else "0 / 1 Donation"
                ),
                BadgeMilestone(
                    title = "5-Time Donor",
                    description = "Awarded for exceptional dedication across five life-saving procedures.",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = PaavaiGold,
                    isEarned = myTotalDonations >= 5,
                    progressMessage = if (myTotalDonations >= 5) "Completed!" else "$myTotalDonations / 5 Donations"
                ),
                BadgeMilestone(
                    title = "Rare Blood Hero",
                    description = "Rare donor type (Negative RH Factor) logged on our rescue network.",
                    icon = Icons.Default.Shield,
                    accentColor = BloodCrimson,
                    isEarned = isRareGroup && myTotalDonations >= 1,
                    progressMessage = if (isRareGroup && myTotalDonations >= 1) "Earned!" else if (!isRareGroup) "Requires rare RH-" else "Required: 1 donation"
                ),
                BadgeMilestone(
                    title = "Camp Crusader",
                    description = "Register for upcoming Paavai blood camps or donor drives on campus.",
                    icon = Icons.Default.Campaign,
                    accentColor = InfoBlue,
                    isEarned = hasVolunteerHist,
                    progressMessage = if (hasVolunteerHist) "Completed!" else "Register for 1 Camp"
                ),
                BadgeMilestone(
                    title = "Universal Lifeline",
                    description = "Exclusive badge awarded only to O Negative (Universal Donor) volunteers.",
                    icon = Icons.Default.FlashOn,
                    accentColor = WarmCoral,
                    isEarned = currentDonor.bloodGroup == "O-" && myTotalDonations >= 1,
                    progressMessage = if (currentDonor.bloodGroup == "O-" && myTotalDonations >= 1) "Claimed!" else if (currentDonor.bloodGroup != "O-") "Requires O- blood" else "Required: 1 donation"
                ),
                BadgeMilestone(
                    title = "Active Sentinel",
                    description = "Current active status with fully updated credentials and weight metrics verified.",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF10B981),
                    isEarned = currentDonor.availability && currentDonor.weight >= 45.0,
                    progressMessage = if (currentDonor.availability && currentDonor.weight >= 45.0) "Active" else "Ineligible/Locked"
                )
            )

            // Dynamic ranking calculated across all registered database donors
            val rankedDonors = donors.map { donor ->
                Pair(donor, calculatePoints(donor))
            }.sortedByDescending { it.second }

            val myRankIndex = rankedDonors.indexOfFirst { it.first.registerNumber == currentDonor.registerNumber }
            val myRankDisplay = if (myRankIndex >= 0) "#${myRankIndex + 1}" else "Unranked"

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                
                // Show dynamic tab panels
                when (activeTab) {
                    0 -> {
                        // TAB 0: Points overview panel + Badges list
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.2.dp, CardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(DeepMaroon.copy(alpha = 0.15f), Transparent)
                                            )
                                        )
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "CURRENT BALANCE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LightSlate,
                                                letterSpacing = 1.2.sp
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "$myPoints",
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = PaavaiGold
                                                )
                                                Text(
                                                    text = "PTS",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LightSlate,
                                                    modifier = Modifier.padding(top = 16.dp)
                                                )
                                            }
                                        }

                                        // Badge / Tier Emblem
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(PaavaiGold.copy(alpha = 0.25f), Color.Transparent)
                                                    ),
                                                    shape = CircleShape
                                                )
                                                .border(1.5.dp, PaavaiGold, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = "Tier Badge",
                                                tint = PaavaiGold,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }

                                    // Dynamic Level Rank
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tier: $levelTier",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                            Text(
                                                text = "$myRankDisplay Overall",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LightSlate
                                            )
                                        }

                                        // Material Progress Bar
                                        LinearProgressIndicator(
                                            progress = progressRatio,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = BloodCrimson,
                                            trackColor = CardBorder
                                        )

                                        if (nextTierTarget > 0) {
                                            Text(
                                                text = "Earn ${nextTierTarget - myPoints} more points to reach the next major military-grade donor ranking.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LightSlate,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            Text(
                                                text = "🎉 You have reached the ultimate Giga Lifesaver tier! Fantastic job!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Grid-like achievements layout header
                        item {
                            Text(
                                text = "Milestone Badges",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Badges custom render items
                        itemsIndexed(badgesList) { _, badge ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("badge_card_${badge.title.replace(" ", "_")}"),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (badge.isEarned) badge.accentColor.copy(alpha = 0.5f) else CardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Badge Icon sphere
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (badge.isEarned) badge.accentColor.copy(alpha = 0.12f)
                                                else Color.White.copy(alpha = 0.03f)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (badge.isEarned) badge.accentColor else CardBorder.copy(alpha = 0.5f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = badge.icon,
                                            contentDescription = badge.title,
                                            tint = if (badge.isEarned) badge.accentColor else LightSlate.copy(alpha = 0.4f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    // Badge Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = badge.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (badge.isEarned) TextDark else LightSlate
                                            )
                                            if (badge.isEarned) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badge.accentColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "UNLOCKED",
                                                        fontSize = 8.sp,
                                                        color = badge.accentColor,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = badge.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (badge.isEarned) LightSlate else LightSlate.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = badge.progressMessage,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (badge.isEarned) badge.accentColor else LightSlate.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Individual Leaderboard
                        item {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "Overall Donor Leaderboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Top performing students and faculties contributing to Namakkal medical centers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightSlate
                                )
                            }
                        }

                        itemsIndexed(rankedDonors) { index, pair ->
                            val donor = pair.first
                            val pointsValue = pair.second
                            val isMe = donor.registerNumber == currentDonor.registerNumber

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("leaderboard_card_$index")
                                    .border(
                                        width = if (isMe) 1.5.dp else 0.dp,
                                        color = if (isMe) PaavaiGold else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) DeepMaroon.copy(alpha = 0.12f) else DarkCharcoal
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Rank counter Sphere with award theme representation
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (index) {
                                                    0 -> PaavaiGold.copy(alpha = 0.15f)
                                                    1 -> Color(0xFFE2E8F0).copy(alpha = 0.15f)
                                                    2 -> WarmCoral.copy(alpha = 0.15f)
                                                    else -> CardBorder
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (index < 3) {
                                            Icon(
                                                imageVector = Icons.Default.MilitaryTech,
                                                contentDescription = "Rank Icon",
                                                tint = when (index) {
                                                    0 -> PaavaiGold
                                                    1 -> Color(0xFFE2E8F0)
                                                    else -> WarmCoral
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LightSlate
                                            )
                                        }
                                    }

                                    // Donor info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isMe) "${donor.name} (You)" else donor.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) PaavaiGold else TextDark
                                        )
                                        Text(
                                            text = "${donor.department} • ${donor.bloodGroup}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LightSlate,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Score / Stats summary
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$pointsValue PTS",
                                            fontWeight = FontWeight.Black,
                                            color = if (index == 0) PaavaiGold else BloodCrimson,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${donor.totalDonations} donations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LightSlate.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Department Rankings
                        item {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "Department Tournament",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Sum of all active logs from department members combined.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightSlate
                                )
                            }
                        }

                        // Calculate Department Stats
                        val deptStatsList = donors.groupBy { it.department }
                            .map { (deptName, members) ->
                                val donationsCount = members.sumOf { it.totalDonations }
                                val deptPoints = members.sumOf { calculatePoints(it) }
                                DepartmentStats(
                                    name = deptName,
                                    totalDonations = donationsCount,
                                    totalPoints = deptPoints,
                                    memberCount = members.size
                                )
                            }.sortedByDescending { it.totalPoints }

                        itemsIndexed(deptStatsList) { index, dept ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dept_card_${dept.name.replace(" ", "_")}"),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Rank Badge
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (index) {
                                                    0 -> SuccessGreen.copy(alpha = 0.15f)
                                                    else -> CardBorder
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) SuccessGreen else TextDark
                                        )
                                    }

                                    // Department statistics
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dept.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "${dept.memberCount} active registrars • ${dept.totalDonations} donations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LightSlate,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Cumulative score
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${dept.totalPoints} PTS",
                                            fontWeight = FontWeight.Black,
                                            color = if (index == 0) SuccessGreen else PaavaiGold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Tournament Scale",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LightSlate.copy(alpha = 0.5f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val Transparent = Color(0x00000000)
