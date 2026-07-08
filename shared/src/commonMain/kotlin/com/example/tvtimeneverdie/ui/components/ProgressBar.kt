package com.example.tvtimeneverdie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val ProgressTrackColor = Color(0xFF1A1A2E)
private val ProgressGradientStart = Color(0xFF00E5FF)
private val ProgressGradientMid = Color(0xFF3B82F6)
private val ProgressGradientEnd = Color(0xFF8B5CF6)
private val ProgressStripeColor = Color.White.copy(alpha = 0.18f)
private val ProgressBubbleColor = Color(0xFFE8E6F7)
private val ProgressBubbleTextColor = Color(0xFF2D1B69)

private val TrackHeight = 20.dp
private val BubbleDiameter = 18.dp

@Composable
fun EpisodeProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val percent = (clampedFraction * 100f).roundToInt().coerceIn(0, 100)

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(TrackHeight)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(ProgressTrackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedFraction)
                    .drawWithCache {
                        val gradientBrush = Brush.linearGradient(
                            colors = listOf(ProgressGradientStart, ProgressGradientMid, ProgressGradientEnd),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                        )
                        val stripeWidthPx = 3.dp.toPx()
                        val stripeSpacingPx = 9.dp.toPx()
                        onDrawBehind {
                            drawRect(brush = gradientBrush, size = size)
                            rotate(degrees = 20f) {
                                var x = -size.height
                                while (x < size.width + size.height) {
                                    drawLine(
                                        color = ProgressStripeColor,
                                        start = Offset(x, -size.height),
                                        end = Offset(x, size.height * 2),
                                        strokeWidth = stripeWidthPx,
                                    )
                                    x += stripeSpacingPx
                                }
                            }
                        }
                    },
            )
        }

        val bubbleRadius = BubbleDiameter / 2
        val rawCenterX = maxWidth * clampedFraction
        val bubbleCenterX = if (maxWidth <= BubbleDiameter) {
            maxWidth / 2
        } else {
            rawCenterX.coerceIn(bubbleRadius, maxWidth - bubbleRadius)
        }
        val bubbleOffsetX = bubbleCenterX - bubbleRadius

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = bubbleOffsetX)
                .size(BubbleDiameter)
                .clip(CircleShape)
                .background(ProgressBubbleColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$percent%",
                color = ProgressBubbleTextColor,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 7.sp,
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                    ),
                ),
            )
        }
    }
}
