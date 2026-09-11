package com.fincalc.app.ui.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fincalc.app.ui.SCREEN_TXT
import com.fincalc.app.ui.screenTextFieldColors

/**
 * 行式数据编辑器（CASH 的 Csh 列 / STAT 的 X(,Y,FREQ) 列）。
 * 每行每列一个输入框（文本，求解时由调用层解析）；行尾 DEL 删除；末行追加。
 * 仅被 CASH/STAT 浅色屏使用（计划 7 Task 5.1），表头/单元格/DEL 文字直接用 Theme 屏区常量。
 */
@Composable
fun ListEditor(
    rows: List<List<String>>,
    columns: List<String>,
    onCellChange: (row: Int, col: Int, text: String) -> Unit,
    onDeleteRow: (row: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 浅底上的显式输入框配色（与 MainActivity 的 I% 框共用 screenTextFieldColors）
    val cellColors = screenTextFieldColors()
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // 表头
        Row(modifier = Modifier.fillMaxWidth()) {
            columns.forEach { c ->
                Text(c, color = SCREEN_TXT, modifier = Modifier.weight(1f).padding(4.dp))
            }
            // 末列与数据行 DEL 按钮对齐（TextButton 最小触控宽约 64dp）
            Text("", modifier = Modifier.width(64.dp).padding(4.dp))
        }
        rows.forEachIndexed { r, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { ci, cell ->
                    OutlinedTextField(
                        value = cell,
                        onValueChange = { onCellChange(r, ci, it) },
                        modifier = Modifier.weight(1f).padding(2.dp),
                        singleLine = true,
                        colors = cellColors
                    )
                }
                TextButton(onClick = { onDeleteRow(r) }) {
                    Text("DEL", color = SCREEN_TXT)
                }
            }
        }
    }
}
