package com.fincalc.app.ui.comp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.state.CalcState
import kotlin.jvm.JvmName

/** COMP 模式控制器：输入行（String + 光标）、求值、错误、历史回溯、Ans 续算。 */
class CompController(val state: CalcState) {

    var input by mutableStateOf("")
        private set
    var cursor by mutableStateOf(0)
        private set
    /** 最近一次求值结果（原始值；显示时按当前 Fix/Sci/Norm 格式化——设置变更即时重显，真机行为）。 */
    var result by mutableStateOf<Double?>(null)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    private var justEvaluated = false

    /** 插入文本（函数名等调用方自带括号）。EXE 后按运算符自动续 Ans（说明书 CN-42）。 */
    fun insert(text: String) {
        if (justEvaluated) {
            if (text.isOperatorStart()) {
                input = "Ans"
                cursor = 3
            } else {
                input = ""
                cursor = 0
            }
            justEvaluated = false
            result = null
        }
        errorText = null
        input = input.substring(0, cursor) + text + input.substring(cursor)
        cursor += text.length
        state.clearShift()   // SHIFT 只作用于下一次按键（真机行为）
    }

    fun delete() {
        if (cursor > 0) {
            input = input.substring(0, cursor - 1) + input.substring(cursor)
            cursor--
        }
    }

    fun clear() {
        input = ""
        cursor = 0
        result = null
        errorText = null
        justEvaluated = false
    }

    fun moveLeft() {
        if (cursor > 0) cursor--
    }

    fun moveRight() {
        if (cursor < input.length) cursor++
    }

    /** 触控定位光标（用户反馈 2026-08-24）。 */
    @JvmName("setCursorPosition")
    fun setCursor(pos: Int) {
        cursor = pos.coerceIn(0, input.length)
        errorText = null
    }

    fun execute() {
        if (input.isBlank()) return
        try {
            val r = ExprEngine.eval(input, state.exprContext())
            state.onEvaluated(input, r)
            result = r
            errorText = null
            justEvaluated = true
        } catch (e: CalcException) {
            errorText = e.kind.display
            result = null
            justEvaluated = false
        }
    }

    /** 历史回溯：把条目载入输入行。 */
    fun historyBack() {
        state.historyBack()?.let {
            input = it.input
            cursor = it.input.length
            result = it.result
            errorText = null
            justEvaluated = false
        }
    }

    fun historyForward() {
        val entry = state.historyForward()
        if (entry != null) {
            input = entry.input
            cursor = entry.input.length
            result = entry.result
        } else {
            clear()
        }
        errorText = null
        justEvaluated = false
    }

    private fun String.isOperatorStart(): Boolean =
        first() in "+-×÷^!%²³" || this == "ˣ√(" || this == " nPr " || this == " nCr "
}
