package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.cos
import kotlin.math.sin

private val IntroInk = Color(0xFF142C3B)
private val IntroTeal = Color(0xFF16776D)

/** A session welcome screen; navigation state is saved by the activity across recreation. */
@Composable
fun IntroScreen(onExplore: () -> Unit, modifier: Modifier = Modifier) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(750)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FAF6), Color(0xFFEAF3EF))))
            .safeDrawingPadding()
            .testTag("intro_screen")
    ) {
        val availableHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = availableHeight)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.intro_brand),
                color = IntroInk, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(vertical = 24.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * 18.dp.toPx()
                    }
            ) {
                KestrelEmblem(Modifier.size(210.dp))
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.intro_title),
                    color = IntroInk, fontSize = 38.sp, lineHeight = 42.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.intro_caption),
                    color = IntroTeal, fontSize = 18.sp, lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.intro_description),
                    modifier = Modifier.widthIn(max = 300.dp),
                    color = Color(0xFF526772), fontSize = 15.sp,
                    lineHeight = 23.sp, textAlign = TextAlign.Center
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Button(
                    onClick = onExplore,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("intro_explore"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IntroInk, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(stringResource(R.string.intro_explore), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.intro_footer), color = Color(0xFF526772),
                    fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun KestrelEmblem(modifier: Modifier = Modifier) {
    val ambient = rememberInfiniteTransition(label = "kestrelAmbient")
    val angle by ambient.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "orbit"
    )
    val pulse by ambient.animateFloat(
        initialValue = 0.94f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "halo"
    )
    Canvas(modifier) {
        val unit = size.minDimension
        val radius = unit * 0.44f
        drawCircle(
            Brush.radialGradient(listOf(IntroTeal.copy(alpha = 0.14f), Color.Transparent), center, unit * 0.5f),
            radius = unit * 0.5f * pulse
        )
        drawCircle(IntroTeal.copy(alpha = 0.18f), radius, style = Stroke(1.dp.toPx()))
        drawCircle(IntroTeal.copy(alpha = 0.10f), radius * 0.76f, style = Stroke(1.dp.toPx()))
        val radians = Math.toRadians(angle.toDouble())
        drawCircle(
            IntroTeal, 4.dp.toPx(),
            center + Offset(cos(radians).toFloat() * radius, sin(radians).toFloat() * radius)
        )
        // An abstract kestrel in flight, drawn natively for crisp scaling.
        val wing = Path().apply {
            moveTo(unit * 0.19f, unit * 0.34f)
            lineTo(unit * 0.48f, unit * 0.43f)
            lineTo(unit * 0.67f, unit * 0.29f)
            lineTo(unit * 0.61f, unit * 0.45f)
            lineTo(unit * 0.81f, unit * 0.47f)
            lineTo(unit * 0.59f, unit * 0.54f)
            lineTo(unit * 0.43f, unit * 0.75f)
            lineTo(unit * 0.45f, unit * 0.53f)
            close()
        }
        drawPath(wing, Brush.linearGradient(listOf(IntroTeal, IntroInk)))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IntroScreenPreview() {
    MyApplicationTheme { IntroScreen(onExplore = {}) }
}
