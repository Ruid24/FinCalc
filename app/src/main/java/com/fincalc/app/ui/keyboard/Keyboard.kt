package com.fincalc.app.ui.keyboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 键帽配色（FC-200V 真机色板）：数字稍亮、模式/功能稍暗、当前模式高亮、DEL/AC/SOLVE 蓝键。 */
enum class KeyColor { NORMAL, NUM, OP, MODE, MODE_ACTIVE, FUNC, BLUE }

/**
 * 键定义：label 主功能；shiftLabel/onShiftPress 第二功能（SHIFT 态）；alphaLabel/onAlphaPress 红字层（ALPHA 态）；
 * onLongPress 长按钩子（可选，null 则无长按行为）；color 键帽配色（见 KeyColor）。
 */
data class Key(
    val label: String,
    val shiftLabel: String? = null,
    val onPress: () -> Unit,
    val onShiftPress: (() -> Unit)? = null,
    val onLongPress: (() -> Unit)? = null,
    val alphaLabel: String? = null,
    val onAlphaPress: (() -> Unit)? = null,
    val color: KeyColor = KeyColor.NORMAL
)

private val KEY_SHIFT_ACTIVE = Color(0xFF39493B)
private val KEY_ALPHA_ACTIVE = Color(0xFF4B3230)
private val KEY_TEXT = Color(0xFFE8F5E9)
private val KEY_SHIFT_MARK = Color(0xFFF0C040)   // shift 标注黄（真机印刷色）
private val KEY_ALPHA_MARK = Color(0xFFE05544)   // alpha 标注红（真机印刷色）

private fun baseColor(color: KeyColor): Color = when (color) {
    KeyColor.NORMAL -> Color(0xFF232B25)
    KeyColor.NUM -> Color(0xFF34403A)
    KeyColor.OP -> Color(0xFF3A483E)
    KeyColor.MODE -> Color(0xFF1E261F)
    KeyColor.MODE_ACTIVE -> Color(0xFF4E6B52)
    KeyColor.FUNC -> Color(0xFF1E261F)
    KeyColor.BLUE -> Color(0xFF2B5EA7)
}

private fun mainFontSize(color: KeyColor) = when (color) {
    KeyColor.NUM -> 20.sp
    KeyColor.MODE, KeyColor.MODE_ACTIVE -> 14.sp
    else -> 16.sp
}

/**
 * 单个键帽（Keypad 与 TopFunctionRows 共用，保证视觉一致）。
 * 键帽顶部印刷 shift 标注（黄）、底部印刷 alpha 标注（红）——非激活态也印刷（真机风格）；
 * 激活时对应标注层升格为主标签显示（原位置不再重复小字）。
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
        shape = RoundedCornerShape(percent = 50)
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
                    color = KEY_TEXT,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else {
                // label 含 "\n" 时拆两行绘制（SHORT CUT1/2）
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (line in lines) {
                        Text(
                            text = line,
                            fontSize = mainFontSize(key.color),
                            color = KEY_TEXT,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** 卡西欧风格键面网格（行内键等宽）。短按触发主/第二/红字功能；长按触发 onLongPress；点击带振动反馈。 */
@Composable
fun Keypad(
    rows: List<List<Key>>,
    shift: Boolean,
    alpha: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in row) {
                    KeyCap(key, shift, alpha, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}
