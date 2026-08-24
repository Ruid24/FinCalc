package com.fincalc.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fincalc.app.R
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode

/** 模式选择对话框：12 模式网格。非 COMP 模式选中后切换（界面为占位，计划 6 实现）。 */
@Composable
fun ModeDialog(state: CalcState, onDismiss: () -> Unit) {
    val modes = Mode.entries.toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mode_select)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in modes.chunked(3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (m in row) {
                            Button(
                                onClick = {
                                    if (m == Mode.COMP) {
                                        state.switchMode(m)
                                    } else {
                                        // 计划 6 实现：先切模式显示占位界面
                                        state.switchMode(m)
                                    }
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(m.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
