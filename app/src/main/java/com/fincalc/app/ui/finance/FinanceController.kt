package com.fincalc.app.ui.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/**
 * 金融模式控制器（FC-200V 操作逻辑，设计文档 §6）：
 * ▲▼/触控选变量（当前行高亮）→ 键盘输入（允许表达式）→ EXE 存入 → 选目标 → SOLVE 求解写回。
 */
class FinanceController(
    val state: CalcState,
    val spec: ModeScreenSpec,
    /** 求解派发：目标变量 → 结果值（引擎调用由模式接线处给出）。 */
    private val solver: (target: FinanceVar) -> Double
) {
    /** 当前选中行下标。 */
    var selected by mutableStateOf(0)
        private set

    /** 当前编辑串（为空 = 未在编辑，显示选中行现值）。 */
    var editText by mutableStateOf<String?>(null)
        private set

    var errorText by mutableStateOf<String?>(null)
        private set

    /** 结果消息（求解成功后显示：target=result）。 */
    var resultText by mutableStateOf<String?>(null)
        private set

    fun select(index: Int) {
        state.clearShift()
        selected = index.coerceIn(0, spec.vars.size - 1)
        editText = null
        errorText = null
        resultText = null
    }

    fun moveUp() = select(selected - 1)
    fun moveDown() = select(selected + 1)

    /** 输入字符（开始/继续编辑当前行）。 */
    fun insert(text: String) {
        state.clearShift()
        errorText = null
        resultText = null
        editText = (editText ?: "") + text
    }

    fun delete() {
        state.clearShift()
        editText = editText?.let { if (it.isNotEmpty()) it.dropLast(1) else null }
    }

    fun clear() {
        state.clearShift()
        editText = null
        errorText = null
        resultText = null
    }

    /** 当前行显示值：仅选中行在编辑中显示编辑串，其余行显示 VARS 现值（格式化）。 */
    fun displayValue(index: Int, v: FinanceVar): String =
        if (index == selected && editText != null) editText!!
        else NumberFormatter.format(state.getVar(v.key), state.settings.display)

    /** EXE：求值当前编辑串并存入选中变量（允许表达式输入，CN-56）。 */
    fun exe() {
        state.clearShift()
        val text = editText ?: return
        try {
            val value = ExprEngine.eval(text, state.exprContext())
            val v = spec.vars[selected]
            state.setVar(v.key, if (v.integer) kotlin.math.round(value) else value)
            editText = null
            errorText = null
        } catch (e: CalcException) {
            errorText = e.kind.display
        }
    }

    /** SOLVE：求解选中变量并写回 VARS。 */
    fun solve() {
        state.clearShift()
        val target = spec.vars[selected]
        if (!target.solvable) {
            errorText = "Math ERROR"
            return
        }
        try {
            val result = solver(target)
            state.setVar(target.key, result)
            resultText = "${target.label} = ${NumberFormatter.format(result, state.settings.display)}"
            errorText = null
            editText = null
        } catch (e: CalcException) {
            errorText = e.kind.display
            resultText = null
        }
    }
}
