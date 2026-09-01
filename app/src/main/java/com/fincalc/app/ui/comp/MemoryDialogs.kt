package com.fincalc.app.ui.comp

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/** COMP 存储器操作：STO（存当前结果到变量）/RCL（插入变量名到输入行）/M+（M+=Ans）/M-（M-=Ans）。 */
object Memory {

    val VAR_NAMES = listOf("A", "B", "C", "D", "X", "Y", "M")

    fun store(state: CalcState, name: String) {
        state.setVar(name, state.getVar("Ans"))
    }

    fun memPlus(state: CalcState) {
        state.setVar("M", state.getVar("M") + state.getVar("Ans"))
    }

    fun memMinus(state: CalcState) {
        state.setVar("M", state.getVar("M") - state.getVar("Ans"))
    }
}

/** 变量选择弹窗（STO/RCL 共用）：列出 A B C D X Y M Ans 及当前值。 */
@Composable
fun VarPickerDialog(
    title: String,
    state: CalcState,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                (Memory.VAR_NAMES + "Ans").forEach { name ->
                    TextButton(onClick = { onPick(name); onDismiss() }) {
                        Text("$name = ${NumberFormatter.format(state.getVar(name), state.settings.display)}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
