package com.personal.mymusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.mymusic.ui.components.*

@Composable
fun OnboardingScreen(
    onContinue: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    FaintCirclesBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Orange,
                showPin = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "What's your name?",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = SketchColors.Ink,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Personalize your music stream and home experience.",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = SketchColors.MutedText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SketchSearchField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "Enter your name",
                        leadingIcon = null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val isEnabled = nameInput.trim().isNotEmpty()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 3.dp, end = 3.dp)
                    ) {
                        // Drop shadow
                        if (isEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(SketchColors.Ink, RoundedCornerShape(14.dp))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isEnabled) Modifier.bounceClick { onContinue(nameInput.trim()) } else Modifier)
                                .background(if (isEnabled) SketchColors.Orange else Color(0xFFE2E4EB), RoundedCornerShape(14.dp))
                                .border(2.dp, if (isEnabled) SketchColors.Ink else Color(0xFFB0B4C4), RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue",
                                color = if (isEnabled) SketchColors.Ink else SketchColors.MutedText,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
