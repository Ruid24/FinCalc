package com.fincalc.app.ui.keyboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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

/** 键定义：label 主功能；shiftLabel 第二功能（SHIFT 态显示并触发）；onLongPress 长按钩子（可选，null 则无长按行为）。 */
data class Key(
    val label: String,
    val shiftLabel: String? = null,
    val onPress: () -> Unit,
    val onShiftPress: (() -> Unit)? = null,
    val onLongPress: (() -> Unit)? = null
)

/** 卡西欧风格键面网格（6 列）。短按触发主/第二功能；长按触发 onLongPress（null 则无长按行为）；点击带振动反馈。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Keypad(
    rows: List<List<Key>>,
    shift: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in row) {
                    val active = shift && key.shiftLabel != null
                    // Material3 Button 不支持长按，改用 Surface + combinedClickable（深色键面不变）
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (active) (key.onShiftPress ?: key.onPress)() else key.onPress()
                                },
                                onLongClick = key.onLongPress
                            ),
                        color = if (active) Color(0xFF39493B) else Color(0xFF232B25),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (active) key.shiftLabel!! else key.label,
                                fontSize = 13.sp,
                                color = Color(0xFFE8F5E9),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
