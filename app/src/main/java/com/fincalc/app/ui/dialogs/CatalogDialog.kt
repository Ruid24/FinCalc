package com.fincalc.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.fincalc.app.R
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.comp.Memory

/**
 * CTLG 目录弹窗：上栏变量（A~D、X、Y、M、Ans，显示现值，点击插入变量名）；
 * 下栏函数目录（新键面放不下的函数，从改造前 compKeys（已删除）收纳，点击插入文本）。
 */
@Composable
fun CatalogDialog(state: CalcState, onInsert: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.catalog)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.catalog_vars), fontSize = 14.sp)
                for (row in (Memory.VAR_NAMES + "Ans").chunked(4)) {
                    Row {
                        for (name in row) {
                            TextButton(
                                onClick = { onInsert(name); onDismiss() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "$name = ${NumberFormatter.format(state.getVar(name), state.settings.display)}",
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Text(stringResource(R.string.catalog_funcs), fontSize = 14.sp)
                for (row in CATALOG_FUNCS.chunked(5)) {
                    Row {
                        for ((label, text) in row) {
                            TextButton(
                                onClick = { onInsert(text); onDismiss() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

/** 函数目录（label 显示 → 插入文本）：与改造前 compKeys（已删除）的插入文本一致。 */
private val CATALOG_FUNCS = listOf(
    "x³" to "³",
    "x⁻¹" to "⁻¹",
    "∛(" to "∛(",
    "ˣ√(" to "ˣ√(",
    "log(" to "log(",
    "10^(" to "10^(",
    "Abs(" to "Abs(",
    "sin⁻¹(" to "asin(",
    "cos⁻¹(" to "acos(",
    "tan⁻¹(" to "atan(",
    "nPr" to " nPr ",
    "nCr" to " nCr ",
    "!" to "!",
    ":" to ":",
    "Ran#" to "Ran#",
    "Pol(" to "Pol("
)
