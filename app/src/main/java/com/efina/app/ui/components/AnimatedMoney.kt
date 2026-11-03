package com.efina.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.efina.app.ui.theme.EfinaMotion

/**
 * Money text that counts up to its value instead of appearing in place.
 *
 * In a finance app the headline figure is the product, and a number that snaps reads as
 * a static label. Counting gives it weight, and more practically it makes a change
 * visible: a refresh repainting 4.250.000 as 4.253.500 is otherwise indistinguishable
 * from no refresh at all.
 *
 * [Animatable] starts at zero, so the first composition counts up, and each later
 * update animates from wherever the value currently sits rather than snapping back to
 * zero. Reduce-motion draws the target directly.
 */
@Composable
fun AnimatedMoney(
    amount: Double,
    currency: String = "IDR",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    durationMillis: Int = 1100
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(amount) {
        if (!EfinaMotion.enabled) {
            animatable.snapTo(amount.toFloat())
        } else {
            animatable.animateTo(
                targetValue = amount.toFloat(),
                animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
            )
        }
    }

    Text(
        text = formatMoney(animatable.value.toDouble(), currency),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1
    )
}