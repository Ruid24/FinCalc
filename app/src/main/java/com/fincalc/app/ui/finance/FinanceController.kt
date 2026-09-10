package com.fincalc.app.ui.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.expr.EvalContext
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/**
 * 金融模式控制器（FC-200V 操作逻辑，设计文档 §6）：
 * ▲▼/触控选变量（当前行高亮）→ 键盘输入（允许表达式）→ EXE 存入 → 选目标 → SOLVE 求解写回。
 * 即输即改（计划 7 Task 4）：insert/delete 后若编辑串可求值立即写回当前行变量（失败静默保持旧值）；
 * 移动选中行（select/moveUp/moveDown）时未确认的编辑先静默提交再移动。
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

    /** 编辑起始时本行变量的快照（即输即改的求值基准）：防止自引用表达式（如 n+1）被中间提交重复求值。 */
    private var editBase: Double? = null

    fun select(index: Int) {
        state.clearModifiers()
        commitEdit(reportError = false)   // 移动前静默提交未确认编辑（插在清 editText 之前）
        selected = index.coerceIn(0, spec.vars.size - 1)
        editText = null
        editBase = null
        errorText = null
        resultText = null
    }

    fun moveUp() = select(selected - 1)
    fun moveDown() = select(selected + 1)

    /** 输入字符（开始/继续编辑当前行）；输入后即输即改：可求值则立即写回当前行变量。 */
    fun insert(text: String) {
        state.clearModifiers()
        errorText = null
        resultText = null
        if (editText == null) editBase = state.getVar(spec.vars[selected].key)   // 编辑开始快照基准值
        editText = (editText ?: "") + text
        commitEdit(reportError = false)
    }

    fun delete() {
        state.clearModifiers()
        errorText = null
        resultText = null
        editText = editText?.let { if (it.isNotEmpty()) it.dropLast(1) else null }
        commitEdit(reportError = false)
    }

    fun clear() {
        state.clearModifiers()
        editText = null
        editBase = null
        errorText = null
        resultText = null
    }

    /** 当前行显示值：仅选中行在编辑中显示编辑串，其余行显示 VARS 现值（格式化）。 */
    fun displayValue(index: Int, v: FinanceVar): String =
        if (index == selected && editText != null) editText!!
        else NumberFormatter.format(state.getVar(v.key), state.settings.display)

    /**
     * 提交当前编辑（复用点）：editText 非空且求值成功则写回当前行变量（integer 行取整）并返回 true；
     * 失败保持旧值与输入，reportError=true 时给出错误提示（EXE 用），false 时静默（insert/delete/select 用）。
     * 注意：本函数不清 editText——即输即改要求输入中行保持编辑态，清理由 exe/select 自行处理。
     * 求值上下文以 editBase（编辑起始快照）为本行变量值：自引用表达式（如 n+1）多次提交幂等。
     */
    private fun commitEdit(reportError: Boolean): Boolean {
        val text = editText?.takeIf { it.isNotEmpty() } ?: return false
        return try {
            val v = spec.vars[selected]
            val base = editBase
            val context = if (base == null) state.exprContext() else object : EvalContext by state.exprContext() {
                override fun getVar(name: String): Double = if (name == v.key) base else state.getVar(name)
            }
            val value = ExprEngine.eval(text, context)
            state.setVar(v.key, if (v.integer) kotlin.math.round(value) else value)
            errorText = null
            true
        } catch (e: CalcException) {
            if (reportError) errorText = e.kind.display
            false
        }
    }

    /** EXE：求值当前编辑串并存入选中变量（允许表达式输入，CN-56）。 */
    fun exe() {
        state.clearModifiers()
        if (commitEdit(reportError = true)) {
            editText = null
            editBase = null
        }
    }

    /** SOLVE：求解选中变量并写回 VARS。 */
    fun solve() {
        state.clearModifiers()
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
            editBase = null
        } catch (e: CalcException) {
            errorText = e.kind.display
            resultText = null
        }
    }
}
