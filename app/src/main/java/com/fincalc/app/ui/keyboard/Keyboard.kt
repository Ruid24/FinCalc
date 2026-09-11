package com.fincalc.app.ui.keyboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.ui.KEY_ALPHA_ACTIVE
import com.fincalc.app.ui.KEY_ALPHA_MARK
import com.fincalc.app.ui.KEY_BLUE
import com.fincalc.app.ui.KEY_FUNC
import com.fincalc.app.ui.KEY_MODE
import com.fincalc.app.ui.KEY_MODE_ACTIVE
import com.fincalc.app.ui.KEY_NORMAL
import com.fincalc.app.ui.KEY_NUM
import com.fincalc.app.ui.KEY_OP
import com.fincalc.app.ui.KEY_SHIFT_ACTIVE
import com.fincalc.app.ui.KEY_SHIFT_MARK
import com.fincalc.app.ui.KEY_TEXT

/** 键帽配色（FC-200V 真机色板，常量见 ui/Theme.kt）：数字稍亮、模式/功能稍暗、当前模式高亮、DEL/AC/SOLVE 蓝键。 */
enum class KeyColor { NORMAL, NUM, OP, MODE, MODE_ACTIVE, FUNC, BLUE }

/**
 * 键定义：label 主功能；shiftLabel/onShiftPress 第二功能（SHIFT 态）；alphaLabel/onAlphaPress 红字层（ALPHA 态）；
 * onLongPress 长按钩子（可选，null 则无长按行为）；color 键帽配色（见 KeyColor）；
 * labelColor 主标签文字色（null=默认 KEY_TEXT；SHIFT/ALPHA 键用对应标注色）。
 */
data class Key(
    val label: String,
    val shiftLabel: String? = null,
    val onPress: () -> Unit,
    val onShiftPress: (() -> Unit)? = null,
    val onLongPress: (() -> Unit)? = null,
    val alphaLabel: String? = null,
    val onAlphaPress: (() -> Unit)? = null,
    val color: KeyColor = KeyColor.NORMAL,
    val labelColor: Color? = null
)

private fun baseColor(color: KeyColor): Color = when (color) {
    KeyColor.NORMAL -> KEY_NORMAL
    KeyColor.NUM -> KEY_NUM
    KeyColor.OP -> KEY_OP
    KeyColor.MODE -> KEY_MODE
    KeyColor.MODE_ACTIVE -> KEY_MODE_ACTIVE
    KeyColor.FUNC -> KEY_FUNC
    KeyColor.BLUE -> KEY_BLUE
}

private fun mainFontSize(color: KeyColor) = when (color) {
    KeyColor.NUM -> 20.sp
    KeyColor.MODE, KeyColor.MODE_ACTIVE -> 14.sp
    else -> 16.sp
}

/**
 * 单个键帽（Keypad 与 TopFunctionRows 共用，保证视觉一致）。
 * 键帽顶部印刷 shift 标注（黄）、底部印刷 alpha 标注（红）——非激活态也印刷（真机风格）；
 * 激活时对应标注层升格为主标签显示（原位置不再重复小字，文字用对应标注色）。
 * 键帽为小曲率圆角矩形（12.dp，渲染图风格）；label 含 "\n" 的两行键（SHORT CUT1/2）
 * 用 11sp 小字、行中心对齐键高 45%/75%，与顶部 shift 标注（~14% 高）互不重叠。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyCap(key: Key, shift: Boolean, alpha: Boolean, modifier: Modifier = Modifier) {
    val view = LocalView.current
    // 激活优先级：alpha > shift > 主功能
    val alphaActive = alpha && key.alphaLabel != null
    val shiftActive = !alphaActive && shift && key.shiftLabel != null
    val displayLabel = when {
        alphaActive -> key.alphaLabel!!
        shiftActive -> key.shiftLabel!!
        else -> key.label
    }
    // 主标签文字色：激活升格显示用对应标注色（本就黄/红），否则用 labelColor（null=默认）
    val labelColor = when {
        alphaActive -> KEY_ALPHA_MARK
        shiftActive -> KEY_SHIFT_MARK
        else -> key.labelColor ?: KEY_TEXT
    }
    // Material3 Button 不支持长按，改用 Surface + combinedClickable
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    when {
                        alphaActive -> (key.onAlphaPress ?: key.onPress)()
                        shiftActive -> (key.onShiftPress ?: key.onPress)()
                        else -> key.onPress()
                    }
                },
                onLongClick = key.onLongPress?.let { lp ->
                    {
                        // 审查发现：foundation 1.7.0 长按框架不自动振动，补 LongPress 触觉反馈
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        lp()
                    }
                }
            ),
        color = when {
            alphaActive -> KEY_ALPHA_ACTIVE
            shiftActive -> KEY_SHIFT_ACTIVE
            else -> baseColor(key.color)
        },
        shape = RoundedCornerShape(12.dp)   // 渲染图小曲率圆角矩形（DPad 仍保持圆形）
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (key.shiftLabel != null && !shiftActive) {
                Text(
                    text = key.shiftLabel,
                    fontSize = 9.sp,
                    color = KEY_SHIFT_MARK,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )
            }
            if (key.alphaLabel != null && !alphaActive) {
                Text(
                    text = key.alphaLabel,
                    fontSize = 9.sp,
                    color = KEY_ALPHA_MARK,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
            }
            val lines = displayLabel.split("\n")
            if (lines.size == 1) {
                Text(
                    text = lines[0],
                    fontSize = mainFontSize(key.color),
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else {
                // label 含 "\n" 拆两行绘制（SHORT CUT1/2）：11sp 小字，
                // 行中心对齐键高 45%/75%，与顶部 shift 标注（~14% 高）、底部留白互不重叠
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.weight(30f))
                    Box(Modifier.weight(30f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(lines[0], fontSize = 11.sp, color = labelColor,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                    Box(Modifier.weight(30f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(lines[1], fontSize = 11.sp, color = labelColor,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                    Spacer(Modifier.weight(10f))
                }
            }
        }
    }
}

/**
 * 卡西欧风格键面网格（行内键等宽）。短按触发主/第二/红字功能；长按触发 onLongPress；点击带振动反馈。
 * fixedRowHeight = null（默认）：各行 weight(1f) 均分 modifier 给定的高度（COMP/金融模式全键盘）；
 * 非 null：各行固定为该高度（CASH/STAT 两行模式键，计划 7 Task 5.2——否则两行被 weight 拉成半屏巨型键），
 * 键盘整体随内容收缩，调用方勿再传 weight/fillMaxHeight 类 modifier。
 */
@Composable
fun Keypad(
    rows: List<List<Key>>,
    shift: Boolean,
    alpha: Boolean,
    modifier: Modifier = Modifier,
    fixedRowHeight: Dp? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = if (fixedRowHeight != null) Modifier.height(fixedRowHeight) else Modifier.weight(1f)
            ) {
                for (key in row) {
                    KeyCap(key, shift, alpha, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}
