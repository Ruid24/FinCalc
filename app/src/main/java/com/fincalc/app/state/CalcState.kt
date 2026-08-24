package com.fincalc.app.state

import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.core.expr.EvalContext
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 12 个计算模式（设计文档 §5）。 */
enum class Mode { COMP, SMPL, CMPD, CASH, AMRT, CNVR, COST, DAYS, DEPR, BOND, BEVN, STAT }

/** 全部设置项（说明书 CN-19~26 配置设定）。 */
data class Settings(
    val angle: AngleUnit = AngleUnit.DEG,
    val display: DisplayMode = DisplayMode.Norm1,
    val payment: Cmpd.Payment = Cmpd.Payment.END,      // Payment：期初 Begin/期末 End（CMPD/AMRT）
    val dn: Cmpd.OddPeriod = Cmpd.OddPeriod.CI,        // dn：奇数期利息 CI 复利/SI 单利（CMPD）
    val days360: Boolean = false,                    // Date Mode：false=365（默认）/true=360
    val dateFormat: Days.DateFormat = Days.DateFormat.MDY,
    val bondTerm: Boolean = false,                   // Bond Date：false=Date/true=Term
    val periodsPerYear: Int = 1,                     // Periods/Y：1=Annual/2=Semi
    val prfRatio: Boolean = false,                   // PRF/Ratio：false=PRF/true=r%
    val bevenSales: Boolean = false,                 // B-Even：false=Quantity/true=Sales
    val statFreq: Boolean = false,                   // STAT：FREQ 栏显示
    val chinese: Boolean = true                      // 界面语言：默认中文
)

/** COMP 历史条目（表达式 + 结果）。 */
data class HistoryEntry(val input: String, val result: Double)

/**
 * 计算器状态机（纯 Kotlin）。持有模式、变量、设置、历史。
 * 变量：A~D、X、Y、M、Ans + 金融 VARS（n、I%、PV、PMT、FV、P/Y、C/Y、PM1、PM2、Dys……计划 6 接线）。
 */
class CalcState(
    settings: Settings = Settings()
) {
    /** Compose 可观察（mutableStateOf）：UI 直接订阅，改动即触发重组（Task 5 审查修复）。 */
    var settings by mutableStateOf(settings)

    var mode: Mode by mutableStateOf(Mode.COMP)
        private set

    var shift: Boolean by mutableStateOf(false)
        private set

    private val vars = mutableMapOf<String, Double>()
    val history = mutableListOf<HistoryEntry>()
    var historyCursor = -1
        private set

    fun switchMode(m: Mode) {
        mode = m
        shift = false
    }

    fun toggleShift() {
        shift = !shift
    }

    /** 真机行为：SHIFT 只作用于下一次按键，插入后自动解除。 */
    fun clearShift() {
        shift = false
    }

    fun getVar(name: String): Double = vars[name] ?: 0.0

    fun setVar(name: String, value: Double) {
        vars[name] = value
    }

    /** 执行表达式求值成功后的状态更新：Ans、历史、光标（说明书 CN-42）。 */
    fun onEvaluated(input: String, result: Double) {
        setVar("Ans", result)
        history += HistoryEntry(input, result)
        if (history.size > HISTORY_CAP) history.removeAt(0)
        historyCursor = -1
    }

    /** 历史回溯：先向旧（▲）再向新（▼）。返回条目或 null（无更多）。 */
    fun historyBack(): HistoryEntry? {
        if (history.isEmpty()) return null
        if (historyCursor == -1) historyCursor = history.size - 1
        else if (historyCursor > 0) historyCursor--
        return history[historyCursor]
    }

    fun historyForward(): HistoryEntry? {
        if (historyCursor == -1) return null
        if (historyCursor < history.size - 1) {
            historyCursor++
            return history[historyCursor]
        }
        historyCursor = -1
        return null
    }

    /** 表达式求值上下文（core/expr 的 EvalContext 适配：变量直通本状态机）。 */
    fun exprContext(): EvalContext = object : EvalContext {
        override val angle: AngleUnit get() = settings.angle
        override val display: DisplayMode get() = settings.display
        override fun getVar(name: String): Double = this@CalcState.getVar(name)
        override fun setVar(name: String, value: Double) = this@CalcState.setVar(name, value)
        override fun nextRandom(): Double = kotlin.random.Random.nextDouble()
    }

    companion object {
        const val HISTORY_CAP = 50
    }
}
