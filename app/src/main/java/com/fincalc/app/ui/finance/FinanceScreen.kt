package com.fincalc.app.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 金融模式通用屏：多行可滚动变量列表，当前行高亮（设计文档 §6）。 */
@Composable
fun FinanceScreen(
    controller: FinanceController,
    onLongPressVar: (FinanceVar) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A1E))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        controller.spec.vars.forEachIndexed { index, v ->
            val isCurrent = index == controller.selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCurrent) Color(0xFF39493B) else Color.Transparent)
                    .clickable { controller.select(index) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${v.label} = ${controller.displayValue(index, v)}",
                    color = Color(0xFFE8F5E9),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
        // 错误/结果行
        controller.errorText?.let {
            Text(it, color = Color(0xFFFFB4A2), fontSize = 18.sp)
        }
        controller.resultText?.let {
            Text(it, color = Color(0xFFE8F5E9), fontSize = 20.sp)
        }
    }
}
