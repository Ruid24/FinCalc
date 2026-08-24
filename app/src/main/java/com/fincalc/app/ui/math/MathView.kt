package com.fincalc.app.ui.math

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.expr.Program
import com.fincalc.app.core.render.FracBox
import com.fincalc.app.core.render.MathBox
import com.fincalc.app.core.render.MathBuilder
import com.fincalc.app.core.render.RowBox
import com.fincalc.app.core.render.SqrtBox
import com.fincalc.app.core.render.SubBox
import com.fincalc.app.core.render.SupBox
import com.fincalc.app.core.render.TextBox
import com.fincalc.app.core.render.TextMeasure

/** 数学公式视图：自研排版器矢量绘制（深色液晶屏上的亮色文字）。 */
@Composable
fun MathView(
    program: Program,
    modifier: Modifier = Modifier,
    baseTextSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = Color(0xFFE8F5E9)
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val em = with(density) { baseTextSize.toPx() }
    val box = remember(program, baseTextSize) {
        MathBuilder.build(program, TextMeasure { text, scale ->
            val layout = textMeasurer.measure(
                text, TextStyle(fontSize = baseTextSize * scale, fontFamily = FontFamily.Serif)
            )
            layout.size.width.toFloat() to layout.size.height.toFloat()
        }, em)
    }
    val w = with(density) { box.width.toDp() }
    val h = with(density) { box.height.toDp() }
    Canvas(modifier.width(w).height(h)) {
        drawBox(box, 0f, 0f, textMeasurer, baseTextSize, color)
    }
}

/** 便捷重载：直接给表达式字符串。解析失败时降级为线性文本（设计文档风险表）。 */
@Composable
fun MathView(
    input: String,
    modifier: Modifier = Modifier,
    baseTextSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = Color(0xFFE8F5E9)
) {
    val program = remember(input) {
        try {
            ExprEngine.parse(input)
        } catch (e: Exception) {
            null
        }
    }
    if (program != null) {
        MathView(program, modifier, baseTextSize, color)
    } else {
        androidx.compose.material3.Text(input, color = color, fontSize = baseTextSize, fontFamily = FontFamily.Serif, modifier = modifier)
    }
}

/** 递归绘制。[x]/[yTop] 为盒子左上角坐标。 */
private fun DrawScope.drawBox(box: MathBox, x: Float, yTop: Float, m: TextMeasurer, baseTextSize: androidx.compose.ui.unit.TextUnit, color: Color) {
    when (box) {
        is TextBox -> drawText(
            m, box.text, Offset(x, yTop),
            TextStyle(color = color, fontSize = baseTextSize * box.scale, fontFamily = FontFamily.Serif)
        )
        is RowBox -> {
            var cx = x
            for (child in box.children) {
                drawBox(child, cx, yTop + box.baseline - child.baseline, m, baseTextSize, color)
                cx += child.width
            }
        }
        is FracBox -> {
            drawBox(box.num, x + (box.width - box.num.width) / 2, yTop, m, baseTextSize, color)
            drawLine(
                color,
                Offset(x, yTop + box.lineY),
                Offset(x + box.width, yTop + box.lineY),
                strokeWidth = box.lineThickness
            )
            drawBox(box.den, x + (box.width - box.den.width) / 2, yTop + box.denTop, m, baseTextSize, color)
        }
        is SupBox -> {
            drawBox(box.base, x, yTop + box.baseTop, m, baseTextSize, color)
            drawBox(box.sup, x + box.base.width, yTop + box.supTop, m, baseTextSize, color)
        }
        is SubBox -> {
            drawBox(box.base, x, yTop, m, baseTextSize, color)
            drawBox(box.sub, x + box.base.width, yTop + box.baseline + box.shiftDown - box.sub.baseline, m, baseTextSize, color)
        }
        is SqrtBox -> {
            val rx = x + box.indexWidth
            val top = yTop
            val bottom = yTop + box.height
            val midX = rx + box.radicalWidth * 0.35f
            // 根号三段折线 + 顶横线
            drawLine(color, Offset(rx, bottom - box.height * 0.3f), Offset(midX, bottom), strokeWidth = 2f)
            drawLine(color, Offset(midX, bottom), Offset(rx + box.radicalWidth, top), strokeWidth = 2f)
            drawLine(
                color,
                Offset(rx + box.radicalWidth, top + 1f),
                Offset(rx + box.radicalWidth + box.content.width + box.padRight, top + 1f),
                strokeWidth = 2f
            )
            box.index?.let { drawBox(it, x, top, m, baseTextSize, color) }
            drawBox(box.content, rx + box.radicalWidth, yTop + box.padTop, m, baseTextSize, color)
        }
    }
}

/** 前缀排版宽度（光标定位用）：前缀可解析则精确，否则线性文本近似。 */
fun measurePrefixWidth(
    prefix: String,
    m: TextMeasurer,
    baseTextSize: androidx.compose.ui.unit.TextUnit,
    em: Float
): Float {
    if (prefix.isEmpty()) return 0f
    val measure = TextMeasure { text, scale ->
        val layout = m.measure(text, TextStyle(fontSize = baseTextSize * scale, fontFamily = FontFamily.Serif))
        layout.size.width.toFloat() to layout.size.height.toFloat()
    }
    val program = try {
        ExprEngine.parse(prefix)
    } catch (e: Exception) {
        null
    }
    return if (program != null) {
        MathBuilder.build(program, measure, em).width
    } else {
        m.measure(prefix, TextStyle(fontSize = baseTextSize, fontFamily = FontFamily.Serif)).size.width.toFloat()
    }
}
