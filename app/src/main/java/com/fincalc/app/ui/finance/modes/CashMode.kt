package com.fincalc.app.ui.finance.modes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.finance.Cash
import com.fincalc.app.state.CalcState

/** CASH 模式控制器：I% + 现金流列表（≤80 项，CN-63）→ NPV/IRR/NFV/PBP。 */
class CashController(val state: CalcState) {

    /** 现金流编辑行（文本态，求解时解析；mutableStateListOf 驱动重组）。 */
    val rows = mutableStateListOf<String>()
    var resultText by mutableStateOf<String?>(null)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun addRow() { if (rows.size < Cash.MAX_ITEMS) rows += "" }   // ≤80 项（CN-63）
    fun deleteRow(i: Int) { if (i in rows.indices) rows.removeAt(i) }
    fun editRow(i: Int, text: String) { if (i in rows.indices) rows[i] = text }

    /** 解析现金流列表（空行跳过；非法数值 → Syntax ERROR）。 */
    private fun parseFlows(): List<Double> {
        val flows = rows.filter { it.isNotBlank() }.map {
            it.trim().toDoubleOrNull() ?: throw com.fincalc.app.core.expr.CalcException(
                com.fincalc.app.core.expr.CalcException.Kind.SYNTAX, "非法数值: $it"
            )
        }
        return flows
    }

    fun solve(target: String) {
        try {
            val flows = parseFlows()
            val i = state.getVar("I%")
            val r = when (target) {
                "NPV" -> Cash.npv(i, flows)
                "IRR" -> Cash.irr(flows)
                "NFV" -> Cash.nfv(i, flows)
                "PBP" -> Cash.pbp(i, flows)
                else -> error("不可求解")
            }
            resultText = "$target = ${com.fincalc.app.core.format.NumberFormatter.format(r, state.settings.display)}"
            errorText = null
        } catch (e: com.fincalc.app.core.expr.CalcException) {
            errorText = e.kind.display
            resultText = null
        }
    }
}
