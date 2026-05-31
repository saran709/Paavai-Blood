package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToNext: () -> Unit
) {
    // Navigate after 2.5 seconds delay
    LaunchedEffect(key1 = true) {
        delay(2500)
        onNavigateToNext()
    }

    // Ripple and Pulse Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSlate),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background gradient rays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BloodCrimson.copy(alpha = 0.15f), Color.Transparent),
                    center = centerOffset,
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = centerOffset
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer pulsing wave
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .background(BloodCrimson.copy(alpha = 0.2f), CircleShape)
                )

                // Middle structural ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Transparent, CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(BloodCrimson, DeepMaroon)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bloodtype,
                        contentDescription = "Paavai Blooddrop Network",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                // Small decorative orbit
                Canvas(modifier = Modifier.size(140.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = PaavaiGold.copy(alpha = 0.4f),
                        radius = size.width / 2.3f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "PAAVAI",
                fontSize = 13.sp,
                color = PaavaiGold,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 5.sp
            )

            Text(
                text = "BLOODCONNECT",
                fontSize = 24.sp,
                color = DeepMaroon,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "Selfless Mobilization Network",
                fontSize = 11.sp,
                color = LightSlate,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Running indicator at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SAVING LIVES INSTANTLY",
                fontSize = 9.sp,
                color = BloodCrimson,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
