package com.fincalc.app.ui.comp

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.ui.math.MathView
import com.fincalc.app.ui.math.measurePrefixWidth
import kotlin.math.roundToInt

/** 排版输入行 + 闪烁光标 + 触控定位（用户反馈 2026-08-24）。 */
@Composable
fun InputLine(
    input: String,
    cursor: Int,
    onCursorTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    baseTextSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = Color(0xFFE8F5E9)
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val em = with(density) { baseTextSize.toPx() }
    // 审查修复：测量结果缓存——闪烁动画每帧重组，避免每帧重解析+重测量
    val cursorX = remember(input, cursor, baseTextSize) {
        measurePrefixWidth(input.take(cursor.coerceIn(0, input.length)), textMeasurer, baseTextSize, em)
    }
    val totalW = remember(input, baseTextSize) {
        measurePrefixWidth(input, textMeasurer, baseTextSize, em)
    }

    // 闪烁（500ms 往复）
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    Box(
        modifier = modifier.pointerInput(input, totalW) {
            detectTapGestures { offset ->
                val ratio = if (totalW > 0) (offset.x / totalW).coerceIn(0f, 1f) else 0f
                onCursorTap((ratio * input.length).roundToInt())
            }
        }
    ) {
        MathView(input, baseTextSize = baseTextSize, color = color)
        // 光标条（常驻 + alpha 控制显隐）
        Box(
            modifier = Modifier
                .offset(x = with(density) { cursorX.toDp() }, y = 0.dp)
                .width(2.dp)
                .height(with(density) { (em * 1.1f).toDp() })
                .alpha(if (alpha > 0.5f) 1f else 0f)
                .background(color)
        )
    }
}
