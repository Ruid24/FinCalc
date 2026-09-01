package com.fincalc.app.ui.finance.modes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.finance.Stat
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/** STAT 模式控制器：类型（1-VAR/7 回归）+ 数据编辑器 + 统计量/回归结果。 */
class StatController(val state: CalcState) {

    var regType by mutableStateOf<Stat.RegType?>(null)   // null = 1-VAR
        private set
    val xs = mutableStateListOf<String>()
    val ys = mutableStateListOf<String>()
    val freqs = mutableStateListOf<String>()
    var resultLines by mutableStateOf<List<String>>(emptyList())
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun setType(type: Stat.RegType?) {
        regType = type
        xs.clear(); ys.clear(); freqs.clear()   // 切类型清数据（真机行为，CN-131）
        resultLines = emptyList()
        errorText = null
    }

    fun addRow() { xs += ""; if (regType != null) ys += ""; if (state.settings.statFreq) freqs += "" }
    fun deleteRow(i: Int) {
        if (i in xs.indices) xs.removeAt(i)
        if (i in ys.indices) ys.removeAt(i)
        if (i in freqs.indices) freqs.removeAt(i)
    }

    private fun entries(): List<Stat.Entry> {
        val n = xs.size
        return (0 until n).mapNotNull { i ->
            val x = xs[i].trim().toDoubleOrNull() ?: return@mapNotNull null
            val y = if (regType != null) ys.getOrNull(i)?.trim()?.toDoubleOrNull() else null
            if (regType != null && y == null) return@mapNotNull null
            val f = if (state.settings.statFreq) freqs.getOrNull(i)?.trim()?.toDoubleOrNull() ?: 1.0 else 1.0
            Stat.Entry(x, y, f)
        }
    }

    /** 计算当前类型的统计量/回归结果（文本行列表）。 */
    fun compute() {
        try {
            val data = entries()
            val lines = mutableListOf<String>()
            val fmt = { v: Double -> NumberFormatter.format(v, state.settings.display) }
            val type = regType
            if (type == null) {
                // 1-VAR
                lines += "n = ${fmt(Stat.count(data))}"
                lines += "Σx = ${fmt(Stat.sumX(data))}"
                lines += "Σx² = ${fmt(Stat.sumX2(data))}"
                lines += "x̄ = ${fmt(Stat.meanX(data))}"
                lines += "xσn = ${fmt(Stat.stdXn(data))}"
                lines += "xσn-1 = ${fmt(Stat.stdXn1(data))}"
                lines += "minX = ${fmt(Stat.minX(data))}"
                lines += "maxX = ${fmt(Stat.maxX(data))}"
            } else {
                val r = Stat.regress(type, data)
                lines += "A = ${fmt(r.a)}"
                lines += "B = ${fmt(r.b)}"
                r.c?.let { lines += "C = ${fmt(it)}" }
                r.r?.let { lines += "r = ${fmt(it)}" }
            }
            resultLines = lines
            errorText = null
        } catch (e: CalcException) {
            errorText = e.kind.display
            resultLines = emptyList()
        }
    }
}
