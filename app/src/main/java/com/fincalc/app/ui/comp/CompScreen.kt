package com.fincalc.app.ui.comp

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.ui.dialogs.CatalogDialog
import com.fincalc.app.ui.keyboard.Keypad
import com.fincalc.app.ui.keyboard.TopFunctionRows
import com.fincalc.app.ui.keyboard.fc200vKeys
import com.fincalc.app.ui.keyboard.fc200vTopRows

/** COMP 模式界面。上屏（输入实时排版 + 结果）下键（FC-200V 仿真键盘）。 */
@Composable
fun CompScreen(controller: CompController, onOpenSettings: () -> Unit) {
    val state = controller.state
    val activity = LocalContext.current as? Activity
    var stoPicker by remember { mutableStateOf(false) }
    var rclPicker by remember { mutableStateOf(false) }
    var varsPicker by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        // 显示屏（深色液晶屏底色）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1B2A1E))
                .padding(12.dp)
        ) {
            // 状态行（模式/角度/SHIFT/ALPHA 指示符）
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildString {
                        append(state.mode.name)
                        append("  ")
                        append(state.settings.angle.name)
                        if (state.shift) append("  SHIFT")
                        if (state.alpha) append("  ALPHA")
                    },
                    color = Color(0xFF9DBA9F),
                    fontSize = 12.sp
                )
            }
            // 输入行（实时 LaTeX 排版，横向可滚动）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (controller.input.isEmpty()) {
                    Text("0", color = Color(0xFFE8F5E9), fontSize = 22.sp, fontFamily = FontFamily.Serif)
                } else {
                    InputLine(input = controller.input, cursor = controller.cursor, onCursorTap = { controller.setCursor(it) }, baseTextSize = 22.sp)
                }
            }
            // 结果/错误行（右对齐；显示时按当前 Fix/Sci/Norm 格式化——设置变更即时重显，真机行为）
            Row(modifier = Modifier.fillMaxWidth()) {
                val error = controller.errorText
                val result = controller.result
                when {
                    error != null -> Text(error, color = Color(0xFFFFB4A2), fontSize = 20.sp)
                    result != null -> Text(
                        NumberFormatter.format(result, state.settings.display),
                        color = Color(0xFFE8F5E9),
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    else -> Text("", fontSize = 26.sp)
                }
            }
        }
        // 键盘区（行0/1 功能行 + 行2-8 键面；屏:键 = 1:4，功能行:键面 = 2:7）
        Column(
            modifier = Modifier.fillMaxWidth().weight(4f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val top = fc200vTopRows(state, comp = controller, fin = null, onOpenSettings = onOpenSettings)
            TopFunctionRows(
                leftTop = top.leftTop,
                leftBottom = top.leftBottom,
                rightTop = top.rightTop,
                rightBottom = top.rightBottom,
                onUp = top.onUp,
                onDown = top.onDown,
                onLeft = top.onLeft,
                onRight = top.onRight,
                shift = state.shift,
                alpha = state.alpha,
                modifier = Modifier.weight(2f)
            )
            Keypad(
                rows = fc200vKeys(
                    state, comp = controller, fin = null,
                    onFinish = { activity?.finish() },
                    onCatalog = { catalog = true },
                    onVars = { varsPicker = true },
                    onSto = { stoPicker = true },
                    onRcl = { rclPicker = true }
                ),
                shift = state.shift,
                alpha = state.alpha,
                modifier = Modifier.weight(7f)
            )
        }
    }
    if (stoPicker) {
        VarPickerDialog("STO", state, onPick = { Memory.store(state, it) }, onDismiss = { stoPicker = false })
    }
    if (rclPicker) {
        VarPickerDialog("RCL", state, onPick = { controller.insert(it) }, onDismiss = { rclPicker = false })
    }
    if (varsPicker) {
        VarPickerDialog("VARS", state, onPick = { controller.insert(it) }, onDismiss = { varsPicker = false })
    }
    if (catalog) {
        CatalogDialog(state, onInsert = { controller.insert(it) }, onDismiss = { catalog = false })
    }
}
