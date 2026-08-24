package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.finance.Cnvr
import com.fincalc.app.core.finance.Cost
import com.fincalc.app.core.finance.Days
import com.fincalc.app.core.finance.Smpl
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/** SMPL（CN-50~52）：Set/Dys/I%/PV → SI、SFV。 */
fun smplSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "SMPL",
        vars = listOf(
            FinanceVar("Dys", "Dys", solvable = false, formula = "SI' = Dys÷(365 or 360) × PV × i", integer = true),
            FinanceVar("I%", "I%", solvable = false, formula = "i = I%÷100"),
            FinanceVar("PV", "PV", solvable = false, formula = "本金（现值）"),
            FinanceVar("SI", "SI", solvable = true, formula = "SI = −SI′"),
            FinanceVar("SFV", "SFV", solvable = true, formula = "SFV = −(PV+SI′)")
        )
    ),
    { target: FinanceVar ->
        val days = if (state.settings.days360) 360 else 365
        when (target.key) {
            "SI" -> Smpl.si(state.getVar("Dys"), state.getVar("I%"), state.getVar("PV"), days)
            "SFV" -> Smpl.sfv(state.getVar("Dys"), state.getVar("I%"), state.getVar("PV"), days)
            else -> error("不可求解")
        }
    }
)

/** CNVR（CN-71~73）：n、I% → EFF/APR。 */
fun cnvrSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "CNVR",
        vars = listOf(
            FinanceVar("n", "n", solvable = false, formula = "年复利计算数", integer = true),
            FinanceVar("I%", "I%", solvable = false, formula = "利率（每年）"),
            FinanceVar("EFF", "EFF", solvable = true, formula = "EFF = ((1+APR/100/n)^n − 1)×100"),
            FinanceVar("APR", "APR", solvable = true, formula = "APR = ((1+EFF/100)^(1/n) − 1)×n×100")
        )
    ),
    { target: FinanceVar ->
        when (target.key) {
            "EFF" -> Cnvr.eff(state.getVar("I%"), state.getVar("n"))
            "APR" -> Cnvr.apr(state.getVar("I%"), state.getVar("n"))
            else -> error("不可求解")
        }
    }
)

/** COST（CN-74/75）：CST/SEL/MRG 互求。 */
fun costSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "COST",
        vars = listOf(
            FinanceVar("CST", "CST", solvable = true, formula = "CST = SEL × (1 − MRG/100)"),
            FinanceVar("SEL", "SEL", solvable = true, formula = "SEL = CST ÷ (1 − MRG/100)"),
            FinanceVar("MRG", "MRG", solvable = true, formula = "MRG = (1 − CST/SEL) × 100")
        )
    ),
    { target: FinanceVar ->
        when (target.key) {
            "CST" -> Cost.cst(state.getVar("SEL"), state.getVar("MRG"))
            "SEL" -> Cost.sel(state.getVar("CST"), state.getVar("MRG"))
            "MRG" -> Cost.mrg(state.getVar("CST"), state.getVar("SEL"))
            else -> error("不可求解")
        }
    }
)

/** DAYS（CN-76~79）：d1/d2/Dys 知二求一。 */
fun daysSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DAYS",
        vars = listOf(
            FinanceVar("d1", "d1", solvable = true, formula = "起始日期", integer = true),
            FinanceVar("d2", "d2", solvable = true, formula = "结束日期", integer = true),
            FinanceVar("Dys", "Dys", solvable = true, formula = "天数（时期）", integer = true)
        )
    ),
    { target: FinanceVar ->
        val fmt = state.settings.dateFormat
        val d360 = state.settings.days360
        when (target.key) {
            "Dys" -> Days.daysBetween(
                Days.parse(state.getVar("d1"), fmt),
                Days.parse(state.getVar("d2"), fmt),
                d360
            ).toDouble()
            "d1" -> Days.format(
                Days.minusDays(Days.parse(state.getVar("d2"), fmt), state.getVar("Dys").toInt()),
                fmt
            )
            "d2" -> Days.format(
                Days.plusDays(Days.parse(state.getVar("d1"), fmt), state.getVar("Dys").toInt()),
                fmt
            )
            else -> error("不可求解")
        }
    }
)
