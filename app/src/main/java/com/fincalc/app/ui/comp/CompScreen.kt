package com.fincalc.app.ui.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.ui.keyboard.Key
import com.fincalc.app.ui.keyboard.Keypad
import com.fincalc.app.ui.keyboard.modeKeyRows
import com.fincalc.app.ui.math.MathView

/** COMP 模式界面。上屏（输入实时排版 + 结果）下键（仿真键盘）。 */
@Composable
fun CompScreen(controller: CompController, onOpenModes: () -> Unit, onOpenSettings: () -> Unit) {
    val state = controller.state
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        // 显示屏（深色液晶屏底色）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1B2A1E))
                .padding(12.dp)
        ) {
            // 状态行（模式/角度/SHIFT 指示符）
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildString {
                        append(state.mode.name)
                        append("  ")
                        append(state.settings.angle.name)
                        if (state.shift) append("  SHIFT")
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
                    MathView(controller.input, baseTextSize = 22.sp)
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
        // 键盘区
        Keypad(rows = compKeys(controller, onOpenModes, onOpenSettings), shift = state.shift, modifier = Modifier.weight(3f))
    }
}

/** COMP 键面（SHIFT 层为第二功能）。 */
private fun compKeys(c: CompController, onOpenModes: () -> Unit, onOpenSettings: () -> Unit): List<List<Key>> {
    val s = c.state
    fun ins(text: String): Key = Key(text, onPress = { c.insert(text) })
    fun insShift(label: String, shiftLabel: String, text: String, shiftText: String): Key =
        Key(label, shiftLabel, onPress = { c.insert(text) }, onShiftPress = { c.insert(shiftText) })

    return modeKeyRows(s) + listOf(
        listOf(
            Key("SHIFT", onPress = { s.toggleShift() }),
            Key("MODE", "SET", onPress = onOpenModes, onShiftPress = onOpenSettings),
            Key("◀", onPress = { c.moveLeft() }),
            Key("▶", onPress = { c.moveRight() }),
            Key("DEL", onPress = { c.delete() }),
            Key("AC", onPress = { c.clear() })
        ),
        listOf(
            insShift("x²", "x³", "²", "³"),
            insShift("√(", "∛(", "√(", "∛("),
            insShift("^", "ˣ√(", "^(", "ˣ√("),
            insShift("ln(", "e^(", "ln(", "e^("),
            insShift("log(", "10^(", "log(", "10^("),
            insShift("(-)", "Abs(", "-", "Abs(")
        ),
        listOf(
            insShift("sin(", "sin⁻¹(", "sin(", "asin("),
            insShift("cos(", "cos⁻¹(", "cos(", "acos("),
            insShift("tan(", "tan⁻¹(", "tan(", "atan("),
            insShift("π", "e", "π", "e"),
            insShift("nPr", "nCr", " nPr ", " nCr "),
            insShift("%", "!", "%", "!")
        ),
        listOf(
            ins("7"), ins("8"), ins("9"), ins("("), ins(")"), insShift(":", "Ran#", ":", "Ran#")
        ),
        listOf(
            ins("4"), ins("5"), ins("6"), ins("×"), ins("÷"), insShift(",", "Pol(", ",", "Pol(")
        ),
        listOf(
            ins("1"), ins("2"), ins("3"), ins("+"), ins("-"), insShift("Ans", "Rnd(", "Ans", "Rnd(")
        ),
        listOf(
            ins("0"), ins("."), ins("E"),
            Key("=", onPress = { c.execute() }),
            Key("▲", onPress = { c.historyBack() }),
            Key("▼", onPress = { c.historyForward() })
        ),
        listOf(
            ins("A"), ins("B"), ins("C"), ins("D"), ins("X"), ins("Y")
        )
    )
}
