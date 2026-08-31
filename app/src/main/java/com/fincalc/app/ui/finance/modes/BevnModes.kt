package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.finance.Bevn
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/** BEVN 子模式清单（设计文档 §5）。 */
enum class BevnSub { BEV, MOS, DOL, DFL, DCL, QTY }

/** BEVN 子模式 spec（BEV 随 PRF/Ratio 与 B-Even 设置切换第 5/6 变量含义）。 */
fun bevnSpec(state: CalcState, sub: BevnSub): Pair<ModeScreenSpec, (FinanceVar) -> Double> =
    when (sub) {
        BevnSub.BEV -> bevSpec(state)
        BevnSub.MOS -> mosSpec(state)
        BevnSub.DOL -> dolSpec(state)
        BevnSub.DFL -> dflSpec(state)
        BevnSub.DCL -> dclSpec(state)
        BevnSub.QTY -> qtySpec(state)
    }

private fun bevSpec(state: CalcState): Pair<ModeScreenSpec, (FinanceVar) -> Double> {
    val ratio = state.settings.prfRatio
    val sales = state.settings.bevenSales
    val vars = mutableListOf(
        FinanceVar("PRC", "PRC", solvable = true, formula = "销售价格"),
        FinanceVar("VCU", "VCU", solvable = true, formula = "单位可变成本"),
        FinanceVar("FC", "FC", solvable = true, formula = "固定成本")
    )
    vars += if (ratio) {
        FinanceVar("r%", "r%", solvable = true, formula = "利润率")
    } else {
        FinanceVar("PRF", "PRF", solvable = true, formula = "利润")
    }
    vars += if (sales) {
        FinanceVar("SBE", "SBE", solvable = true, formula = "损益平衡销售额")
    } else {
        FinanceVar("QBE", "QBE", solvable = true, formula = "损益平衡销售量")
    }
    val solver = { target: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (target.key) {
            "PRC" -> if (ratio) Bevn.prcFromQbeRatio(g("QBE"), g("VCU"), g("FC"), g("r%")) else Bevn.prcFromQbePrf(g("QBE"), g("VCU"), g("FC"), g("PRF"))
            "VCU" -> if (ratio) Bevn.vcuFromQbeRatio(g("QBE"), g("PRC"), g("FC"), g("r%")) else Bevn.vcuFromQbePrf(g("QBE"), g("PRC"), g("FC"), g("PRF"))
            "FC" -> if (ratio) Bevn.fcFromQbeRatio(g("QBE"), g("PRC"), g("VCU"), g("r%")) else Bevn.fcFromQbePrf(g("QBE"), g("PRC"), g("VCU"), g("PRF"))
            "PRF" -> Bevn.prfFromQbe(g("QBE"), g("PRC"), g("VCU"), g("FC"))
            "r%" -> Bevn.rPercentFromQbe(g("QBE"), g("PRC"), g("VCU"), g("FC"))
            "QBE" -> if (ratio) Bevn.qbeRatio(g("PRC"), g("VCU"), g("FC"), g("r%")) else Bevn.qbePrf(g("PRC"), g("VCU"), g("FC"), g("PRF"))
            "SBE" -> if (ratio) Bevn.sbeRatio(g("PRC"), g("VCU"), g("FC"), g("r%")) else Bevn.sbePrf(g("PRC"), g("VCU"), g("FC"), g("PRF"))
            else -> error("不可求解")
        }
    }
    return Pair(ModeScreenSpec(title = "BEV", vars = vars), solver)
}

private fun mosSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "MOS",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额"),
            FinanceVar("SBE", "SBE", solvable = true, formula = "损益平衡销售额"),
            FinanceVar("MOS", "MOS", solvable = true, formula = "MOS = (SAL−SBE)/SAL")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.salFromMos(g("MOS"), g("SBE"))
            "SBE" -> Bevn.sbeFromMos(g("MOS"), g("SAL"))
            "MOS" -> Bevn.mos(g("SAL"), g("SBE"))
            else -> error("不可求解")
        }
    }
)

private fun dolSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DOL",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额"),
            FinanceVar("VC", "VC", solvable = true, formula = "可变成本"),
            FinanceVar("FC", "FC", solvable = true, formula = "固定成本"),
            FinanceVar("DOL", "DOL", solvable = true, formula = "DOL = (SAL−VC)/(SAL−VC−FC)")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.salFromDol(g("DOL"), g("VC"), g("FC"))
            "VC" -> Bevn.vcFromDol(g("DOL"), g("SAL"), g("FC"))
            "FC" -> Bevn.fcFromDol(g("DOL"), g("SAL"), g("VC"))
            "DOL" -> Bevn.dol(g("SAL"), g("VC"), g("FC"))
            else -> error("不可求解")
        }
    }
)

private fun dflSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DFL",
        vars = listOf(
            FinanceVar("EIT", "EIT", solvable = true, formula = "利税前收入（EBIT）"),
            FinanceVar("ITR", "ITR", solvable = true, formula = "利息"),
            FinanceVar("DFL", "DFL", solvable = true, formula = "DFL = EIT/(EIT−ITR)")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "EIT" -> Bevn.eitFromDfl(g("DFL"), g("ITR"))
            "ITR" -> Bevn.itrFromDfl(g("DFL"), g("EIT"))
            "DFL" -> Bevn.dfl(g("EIT"), g("ITR"))
            else -> error("不可求解")
        }
    }
)

private fun dclSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DCL",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额"),
            FinanceVar("VC", "VC", solvable = true, formula = "可变成本"),
            FinanceVar("FC", "FC", solvable = true, formula = "固定成本"),
            FinanceVar("ITR", "ITR", solvable = true, formula = "利息"),
            FinanceVar("DCL", "DCL", solvable = true, formula = "DCL = (SAL−VC)/(SAL−VC−FC−ITR)")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.salFromDcl(g("DCL"), g("VC"), g("FC"), g("ITR"))
            "VC" -> Bevn.vcFromDcl(g("DCL"), g("SAL"), g("FC"), g("ITR"))
            "FC" -> Bevn.fcFromDcl(g("DCL"), g("SAL"), g("VC"), g("FC"))
            "ITR" -> Bevn.itrFromDcl(g("DCL"), g("SAL"), g("VC"), g("FC"))
            "DCL" -> Bevn.dcl(g("SAL"), g("VC"), g("FC"), g("ITR"))
            else -> error("不可求解")
        }
    }
)

private fun qtySpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "QTY CONV",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额 = PRC×QTY"),
            FinanceVar("PRC", "PRC", solvable = true, formula = "销售价格"),
            FinanceVar("QTY", "QTY", solvable = true, formula = "销售数量（两组联动）"),
            FinanceVar("VC", "VC", solvable = true, formula = "可变成本 = VCU×QTY"),
            FinanceVar("VCU", "VCU", solvable = true, formula = "单位可变成本")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.sal(g("PRC"), g("QTY"))
            "PRC" -> Bevn.prcFromSal(g("SAL"), g("QTY"))
            "QTY" -> if (g("PRC") != 0.0) Bevn.qtyFromSal(g("SAL"), g("PRC")) else Bevn.qtyFromVc(g("VC"), g("VCU"))
            "VC" -> Bevn.vc(g("VCU"), g("QTY"))
            "VCU" -> Bevn.vcuFromVc(g("VC"), g("QTY"))
            else -> error("不可求解")
        }
    }
)
