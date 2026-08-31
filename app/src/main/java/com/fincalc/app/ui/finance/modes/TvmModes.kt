package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.finance.Amrt
import com.fincalc.app.core.finance.Bond
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/** CMPD（CN-53~58）：n、I%、PV、PMT、FV 任求其一。 */
fun cmpdSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "CMPD",
        vars = listOf(
            FinanceVar("n", "n", solvable = true, formula = "付款次数"),
            FinanceVar("I%", "I%", solvable = true, formula = "利率（每年，牛顿法近似）"),
            FinanceVar("PV", "PV", solvable = true, formula = "PV = (−α·PMT − β·FV)/γ"),
            FinanceVar("PMT", "PMT", solvable = true, formula = "PMT = (−γ·PV − β·FV)/α"),
            FinanceVar("FV", "FV", solvable = true, formula = "FV = (−γ·PV − α·PMT)/β")
        )
    ),
    { target: FinanceVar ->
        val s = state.settings
        val py = s.periodsPerYear
        val cy = s.periodsPerYear
        val pay = s.payment
        val dn = s.dn
        when (target.key) {
            "n" -> Cmpd.solveN(state.getVar("I%"), state.getVar("PV"), state.getVar("PMT"), state.getVar("FV"), py, cy, pay, dn)
            "I%" -> Cmpd.solveI(state.getVar("n"), state.getVar("PV"), state.getVar("PMT"), state.getVar("FV"), py, cy, pay, dn)
            "PV" -> Cmpd.solvePV(state.getVar("n"), state.getVar("I%"), state.getVar("PMT"), state.getVar("FV"), py, cy, pay, dn)
            "PMT" -> Cmpd.solvePMT(state.getVar("n"), state.getVar("I%"), state.getVar("PV"), state.getVar("FV"), py, cy, pay, dn)
            "FV" -> Cmpd.solveFV(state.getVar("n"), state.getVar("I%"), state.getVar("PV"), state.getVar("PMT"), py, cy, pay, dn)
            else -> error("不可求解")
        }
    }
)

/** AMRT（CN-65~70）：与 CMPD 共享 n/I%/PV/PMT/FV；PM1/PM2 + 五量。 */
fun amrtSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "AMRT",
        vars = listOf(
            FinanceVar("PM1", "PM1", solvable = false, formula = "范围首笔付款", integer = true),
            FinanceVar("PM2", "PM2", solvable = false, formula = "范围末笔付款", integer = true),
            FinanceVar("n", "n", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("I%", "I%", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("PV", "PV", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("PMT", "PMT", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("FV", "FV", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("BAL", "BAL", solvable = true, formula = "PM2 付款完毕时的本金余额"),
            FinanceVar("INT", "INT", solvable = true, formula = "PM1 的利息部分"),
            FinanceVar("PRN", "PRN", solvable = true, formula = "PM1 的本金部分"),
            FinanceVar("ΣINT", "ΣINT", solvable = true, formula = "PM1 至 PM2 的总利息"),
            FinanceVar("ΣPRN", "ΣPRN", solvable = true, formula = "PM1 至 PM2 的总本金")
        )
    ),
    { target: FinanceVar ->
        val s = state.settings
        val r = Amrt.amortize(
            state.getVar("PM1").toInt(), state.getVar("PM2").toInt(),
            state.getVar("I%"), state.getVar("PV"), state.getVar("PMT"),
            s.periodsPerYear, s.periodsPerYear, s.payment
        )
        when (target.key) {
            "BAL" -> r.bal
            "INT" -> r.int
            "PRN" -> r.prn
            "ΣINT" -> r.sumInt
            "ΣPRN" -> r.sumPrn
            else -> error("不可求解")
        }
    }
)

/** BOND（CN-85~92）：Date 形态（d1/d2）与 Term 形态（n）随设置切换。 */
fun bondSpec(state: CalcState): Pair<ModeScreenSpec, (FinanceVar) -> Double> {
    val term = state.settings.bondTerm
    val vars = mutableListOf<FinanceVar>()
    if (term) {
        vars += FinanceVar("n", "n", solvable = false, formula = "到期前票息支付次数", integer = true)
    } else {
        vars += FinanceVar("d1", "d1", solvable = false, formula = "购买日期", integer = true)
        vars += FinanceVar("d2", "d2", solvable = false, formula = "偿还日期", integer = true)
    }
    vars += listOf(
        FinanceVar("RDV", "RDV", solvable = false, formula = "每 \$100 票面价值的赎回价格"),
        FinanceVar("CPN", "CPN", solvable = false, formula = "息票利率"),
        FinanceVar("PRC", "PRC", solvable = true, formula = "每 \$100 票面价值的价格"),
        FinanceVar("YLD", "YLD", solvable = true, formula = "年收益（牛顿法近似）")
    )
    if (!term) {
        vars += FinanceVar("INT", "INT", solvable = false, formula = "应计利息（随 PRC 同算）")
        vars += FinanceVar("CST", "CST", solvable = false, formula = "含息价格（PRC+INT）")
    }
    val solver = { target: FinanceVar ->
        val s = state.settings
        val m = s.periodsPerYear
        val d360 = s.days360
        when (target.key) {
            "PRC" -> if (term) {
                Bond.prcTerm(state.getVar("n").toInt(), state.getVar("RDV"), state.getVar("CPN"), state.getVar("YLD"), m)
            } else {
                // BOND 专属日期范围（CN-87 L2570-2572）：d1∈1902~2097
                val d1 = Days.parse(state.getVar("d1"), s.dateFormat)
                val d2 = Days.parse(state.getVar("d2"), s.dateFormat)
                if (d1.year !in 1902..2097 || d2.year !in 1902..2097) {
                    throw CalcException(CalcException.Kind.ARGUMENT, "BOND 日期范围 1902~2097")
                }
                val r = Bond.prcDate(d1, d2, state.getVar("RDV"), state.getVar("CPN"), state.getVar("YLD"), m, d360)
                state.setVar("INT", r.int)
                state.setVar("CST", r.cst)
                r.prc
            }
            "YLD" -> if (term) {
                Bond.yldTerm(state.getVar("n").toInt(), state.getVar("RDV"), state.getVar("CPN"), state.getVar("PRC"), m)
            } else {
                val d1 = Days.parse(state.getVar("d1"), s.dateFormat)
                val d2 = Days.parse(state.getVar("d2"), s.dateFormat)
                Bond.yldDate(d1, d2, state.getVar("RDV"), state.getVar("CPN"), state.getVar("PRC"), m, d360)
            }
            else -> error("不可求解")
        }
    }
    return Pair(ModeScreenSpec(title = "BOND", vars = vars), solver)
}
