package com.fincalc.app.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 键定义：label 主功能；shiftLabel 第二功能（SHIFT 态显示并触发）。 */
data class Key(
    val label: String,
    val shiftLabel: String? = null,
    val onPress: () -> Unit,
    val onShiftPress: (() -> Unit)? = null
)

/** 卡西欧风格键面网格（6 列）。按键高亮反馈由 Button 自带；振动由调用层统一处理。 */
@Composable
fun Keypad(
    rows: List<List<Key>>,
    shift: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in row) {
                    val active = shift && key.shiftLabel != null
                    Button(
                        onClick = { if (active) (key.onShiftPress ?: key.onPress)() else key.onPress() },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) Color(0xFF39493B) else Color(0xFF232B25)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
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
