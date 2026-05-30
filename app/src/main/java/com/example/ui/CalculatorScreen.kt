package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val displayText by viewModel.displayText.collectAsState()
    val expressionPreview by viewModel.expressionPreview.collectAsState()

    // Precise color palette satisfying the disguised standard calculator look
    val darkBgColor = Color(0xFF0F0F0F) // Solid matte charcoal/black base
    val deepBlueCircle = Color(0xFF1B3B6F) // Deep blue/slate-blue circle for Row 1 and right operators
    val lightBlueGreyText = Color(0xFFADD8E6) // Light blue/grey text for AC / Row 1
    val softDarkGreyBg = Color(0xFF2C2C2E) // Soft dark-grey circle background for numbers
    val softMintGreenBg = Color(0xFFB1F2D1) // Solid soft mint-green circle
    val darkGreenOperatorText = Color(0xFF064E3B) // Dark operator text/icon on '='

    // Row 1: AC, (), %, ÷
    // Rows 2-4: Numbers 7, 8, 9, × | 4, 5, 6, - | 1, 2, 3, +
    // Row 5: 0, ., ⌫, =
    val buttons = listOf(
        listOf("AC", "()", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkBgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Top AppBar area: Translucent/dark background containing the calculator action button and Settings gear action button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x990F0F0F))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calculator Icon Button (representing mathematical operators)
            IconButton(
                onClick = { },
                modifier = Modifier.testTag("btn_calculator_icon_nav")
            ) {
                // Developer Note: To use the custom asset file 'image_3f4dff.png':
                // 1. Copy the PNG/SVG file to your 'app/src/main/res/drawable/image_3f4dff.png' directory.
                // 2. Import 'androidx.compose.ui.res.painterResource' and use:
                //    Icon(
                //        painter = painterResource(id = R.drawable.image_3f4dff),
                //        contentDescription = "Hidden Vault Entry",
                //        tint = Color.White.copy(alpha = 0.8f),
                //        modifier = Modifier.size(24.dp)
                //    )
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Hidden Vault Entry",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Settings/Gear Icon Button next to it
            IconButton(
                onClick = { },
                modifier = Modifier.testTag("btn_settings_icon_nav")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Vault Settings Link",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Display Panel: A large, minimal black display area for calculations
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                if (expressionPreview.isNotBlank()) {
                    Text(
                        text = expressionPreview,
                        color = Color.Gray,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = if (displayText.length > 8) 42.sp else 64.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("calculator_display")
                )
            }
        }

        // Collapse/expand arrow icon right above the button pad on the far left side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkBgColor)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = "Collapse/Expand keypad",
                tint = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Calculator Buttons Grid Pad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkBgColor)
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { char ->
                        // Determine background and text colors according to exact specifications
                        val backColor = when (char) {
                            "AC", "()", "%", "÷" -> deepBlueCircle
                            "×", "-", "+" -> deepBlueCircle
                            "=" -> softMintGreenBg
                            else -> softDarkGreyBg
                        }
                        
                        val textColor = when (char) {
                            "AC" -> lightBlueGreyText
                            "()" -> lightBlueGreyText
                            "%" -> lightBlueGreyText
                            "=" -> darkGreenOperatorText
                            else -> Color.White
                        }

                        // Map keyboard actions back to standard math symbols as supported by the parser
                        val actionKey = when (char) {
                            "÷" -> "/"
                            "×" -> "*"
                            else -> char
                        }

                        CalculatorButton(
                            text = char,
                            backgroundColor = backColor,
                            textColor = textColor,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { viewModel.onCalculatorKeyPress(actionKey) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag("btn_$text")
    ) {
        if (text == "⌫") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                color = textColor,
                fontSize = if (text.length > 2) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
