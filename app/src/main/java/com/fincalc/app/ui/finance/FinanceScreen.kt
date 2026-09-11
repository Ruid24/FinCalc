package com.fincalc.app.ui.finance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.ui.SCREEN_BG
import com.fincalc.app.ui.SCREEN_ERROR
import com.fincalc.app.ui.SCREEN_SEL_BG
import com.fincalc.app.ui.SCREEN_SEL_TXT
import com.fincalc.app.ui.SCREEN_TXT

/**
 * 金融模式通用屏：多行可滚动变量列表（浅色液晶屏），当前行反色高亮（深底浅字）。长按变量行弹公式（学习辅助）。
 * 光标跟随滚动（计划 7 Task 5.3）：按键/圆盘改变 selected 时把当前行滚进视野；
 * 仅在 selected 变化时触发，用户手动滑走查看不被打断；触摸点击的行本就在屏上，bringIntoView 无副作用。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinanceScreen(
    controller: FinanceController,
    onLongPressVar: (FinanceVar) -> Unit = {}
) {
    // 单一 requester 随 recomposition 挂到当前选中行；LaunchedEffect 在重组生效后执行，定位的已是新行
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(controller, controller.selected) { requester.bringIntoView() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SCREEN_BG)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        controller.spec.vars.forEachIndexed { index, v ->
            val isCurrent = index == controller.selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isCurrent) Modifier.bringIntoViewRequester(requester) else Modifier)
                    .background(if (isCurrent) SCREEN_SEL_BG else Color.Transparent)
                    .combinedClickable(
                        onClick = { controller.select(index) },
                        onLongClick = { onLongPressVar(v) }
                    )
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${v.label} = ${controller.displayValue(index, v)}",
                    color = if (isCurrent) SCREEN_SEL_TXT else SCREEN_TXT,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
        // 错误/结果行
        controller.errorText?.let {
            Text(it, color = SCREEN_ERROR, fontSize = 18.sp)
        }
        controller.resultText?.let {
            Text(it, color = SCREEN_TXT, fontSize = 20.sp)
        }
    }
}
