package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.finance.Depr
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/**
 * DEPR 折旧（CN-80~84）：n、I%、PV、FV、j、YR1 → 第 j 年折旧费与剩余可折旧值。
 * 方法（SL/FP/SYD/DB）由子模式切换条选择（终审补入——计划 6 原任务划分漏掉了 DEPR 界面）。
 */
fun deprSpec(state: CalcState, method: Depr.Method): Pair<ModeScreenSpec, (FinanceVar) -> Double> =
    Pair(
        ModeScreenSpec(
            title = "DEPR-${method.name}",
            vars = listOf(
                FinanceVar("n", "n", solvable = false, formula = "使用年限", integer = true),
                FinanceVar(
                    "I%", "I%", solvable = false,
                    formula = if (method == Depr.Method.FP) "折旧比（FP 定率法）" else "折旧因子（DB，200=DDB）"
                ),
                FinanceVar("PV", "PV", solvable = false, formula = "原始成本"),
                FinanceVar("FV", "FV", solvable = false, formula = "剩余账面价值"),
                FinanceVar("j", "j", solvable = false, formula = "折旧成本计算年", integer = true),
                FinanceVar("YR1", "YR1", solvable = false, formula = "折旧第一年的月数", integer = true),
                FinanceVar("DEP", "DEP", solvable = true, formula = "第 j 年折旧费"),
                FinanceVar("RDV", "RDV", solvable = true, formula = "第 j 年末剩余可折旧值")
            )
        ),
        { target ->
            val r = Depr.depreciate(
                method,
                state.getVar("n").toInt(), state.getVar("I%"),
                state.getVar("PV"), state.getVar("FV"),
                state.getVar("j").toInt(), state.getVar("YR1").toInt()
            )
            // 折旧费与剩余可折旧值是联合输出，一次计算写回两者
            state.setVar("DEP", r.depreciation)
            state.setVar("RDV", r.rdv)
            when (target.key) {
                "DEP" -> r.depreciation
                "RDV" -> r.rdv
                else -> error("不可求解")
            }
        }
    )
