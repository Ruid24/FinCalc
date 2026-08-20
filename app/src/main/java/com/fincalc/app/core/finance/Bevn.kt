package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/**
 * BEVN 损益分析（说明书 CN-93~110）：BEV、MOS、DOL、DFL、DCL、QTY CONV 六子模式。
 * 全部变量正解/反解（BEV 五变量任解，CN-95 L2872-2874）；分母为 0 → Math ERROR（CN-169 通用规则）。
 * BEV 受两个设置影响（CN-23）：PRF/Ratio（目标利润额/利润率）、B-Even（销售量/销售额）——
 * 引擎按函数区分，设置切换属 state 层。
 */
object Bevn {

    // ---------- BEV（CN-94~99） ----------

    /** 利润模式（PRF/Ratio=PRF）：QBE = (FC+PRF)/(PRC−VCU)。 */
    fun qbePrf(prc: Double, vcu: Double, fc: Double, prf: Double): Double {
        val d = prc - vcu
        if (d == 0.0) mathErr("除以 0")
        return (fc + prf) / d
    }

    /** SBE = QBE × PRC。 */
    fun sbePrf(prc: Double, vcu: Double, fc: Double, prf: Double): Double =
        qbePrf(prc, vcu, fc, prf) * prc

    /** 利润率模式（PRF/Ratio=r%）：QBE = FC/(PRC×(1−r%/100)−VCU)。 */
    fun qbeRatio(prc: Double, vcu: Double, fc: Double, rPercent: Double): Double {
        val d = prc * (1 - rPercent / 100) - vcu
        if (d == 0.0) mathErr("除以 0")
        return fc / d
    }

    /** SBE = QBE × PRC。 */
    fun sbeRatio(prc: Double, vcu: Double, fc: Double, rPercent: Double): Double =
        qbeRatio(prc, vcu, fc, rPercent) * prc

    // BEV 反解（PRF 模式）
    /** PRC = (FC+PRF)/QBE + VCU。 */
    fun prcFromQbePrf(qbe: Double, vcu: Double, fc: Double, prf: Double): Double {
        if (qbe == 0.0) mathErr("除以 0")
        return (fc + prf) / qbe + vcu
    }

    /** VCU = PRC − (FC+PRF)/QBE。 */
    fun vcuFromQbePrf(qbe: Double, prc: Double, fc: Double, prf: Double): Double {
        if (qbe == 0.0) mathErr("除以 0")
        return prc - (fc + prf) / qbe
    }

    /** FC = QBE×(PRC−VCU) − PRF。 */
    fun fcFromQbePrf(qbe: Double, prc: Double, vcu: Double, prf: Double): Double =
        qbe * (prc - vcu) - prf

    /** PRF = QBE×(PRC−VCU) − FC。 */
    fun prfFromQbe(qbe: Double, prc: Double, vcu: Double, fc: Double): Double =
        qbe * (prc - vcu) - fc

    // BEV 反解（r% 模式）
    /** PRC = (FC/QBE + VCU)/(1 − r%/100)。 */
    fun prcFromQbeRatio(qbe: Double, vcu: Double, fc: Double, rPercent: Double): Double {
        val d = 1 - rPercent / 100
        if (qbe == 0.0 || d == 0.0) mathErr("除以 0")
        return (fc / qbe + vcu) / d
    }

    /** VCU = PRC×(1−r%/100) − FC/QBE。 */
    fun vcuFromQbeRatio(qbe: Double, prc: Double, fc: Double, rPercent: Double): Double {
        if (qbe == 0.0) mathErr("除以 0")
        return prc * (1 - rPercent / 100) - fc / qbe
    }

    /** FC = QBE×(PRC×(1−r%/100) − VCU)。 */
    fun fcFromQbeRatio(qbe: Double, prc: Double, vcu: Double, rPercent: Double): Double =
        qbe * (prc * (1 - rPercent / 100) - vcu)

    /** r% = (1 − (FC/QBE + VCU)/PRC) × 100。 */
    fun rPercentFromQbe(qbe: Double, prc: Double, vcu: Double, fc: Double): Double {
        if (qbe == 0.0 || prc == 0.0) mathErr("除以 0")
        return (1 - (fc / qbe + vcu) / prc) * 100
    }

    // ---------- MOS 安全边际（CN-100 前后）：MOS = (SAL−SBE)/SAL ----------

    fun mos(sal: Double, sbe: Double): Double {
        if (sal == 0.0) mathErr("除以 0")
        return (sal - sbe) / sal
    }

    /** SAL = SBE/(1−MOS)。 */
    fun salFromMos(mos: Double, sbe: Double): Double {
        if (1 - mos == 0.0) mathErr("除以 0")
        return sbe / (1 - mos)
    }

    /** SBE = SAL×(1−MOS)。 */
    fun sbeFromMos(mos: Double, sal: Double): Double = sal * (1 - mos)

    // ---------- DOL 经营杠杆（CN-102 前后）：DOL = (SAL−VC)/(SAL−VC−FC) ----------

    fun dol(sal: Double, vc: Double, fc: Double): Double {
        val d = sal - vc - fc
        if (d == 0.0) mathErr("除以 0")
        return (sal - vc) / d
    }

    /** SAL = DOL×FC/(DOL−1) + VC。 */
    fun salFromDol(dol: Double, vc: Double, fc: Double): Double {
        if (dol - 1 == 0.0) mathErr("除以 0")
        return dol * fc / (dol - 1) + vc
    }

    /** VC = SAL − DOL×FC/(DOL−1)。 */
    fun vcFromDol(dol: Double, sal: Double, fc: Double): Double {
        if (dol - 1 == 0.0) mathErr("除以 0")
        return sal - dol * fc / (dol - 1)
    }

    /** FC = (SAL−VC)×(DOL−1)/DOL。 */
    fun fcFromDol(dol: Double, sal: Double, vc: Double): Double {
        if (dol == 0.0) mathErr("除以 0")
        return (sal - vc) * (dol - 1) / dol
    }

    // ---------- DFL 财务杠杆（CN-104~106）：DFL = EIT/(EIT−ITR) ----------

    fun dfl(eit: Double, itr: Double): Double {
        val d = eit - itr
        if (d == 0.0) mathErr("除以 0")
        return eit / d
    }

    /** EIT = DFL×ITR/(DFL−1)。 */
    fun eitFromDfl(dfl: Double, itr: Double): Double {
        if (dfl - 1 == 0.0) mathErr("除以 0")
        return dfl * itr / (dfl - 1)
    }

    /** ITR = EIT×(DFL−1)/DFL。 */
    fun itrFromDfl(dfl: Double, eit: Double): Double {
        if (dfl == 0.0) mathErr("除以 0")
        return eit * (dfl - 1) / dfl
    }

    // ---------- DCL 复合杠杆（CN-106~108）：DCL = (SAL−VC)/(SAL−VC−FC−ITR) ----------

    fun dcl(sal: Double, vc: Double, fc: Double, itr: Double): Double {
        val d = sal - vc - fc - itr
        if (d == 0.0) mathErr("除以 0")
        return (sal - vc) / d
    }

    /** SAL = DCL×(FC+ITR)/(DCL−1) + VC。 */
    fun salFromDcl(dcl: Double, vc: Double, fc: Double, itr: Double): Double {
        if (dcl - 1 == 0.0) mathErr("除以 0")
        return dcl * (fc + itr) / (dcl - 1) + vc
    }

    /** VC = SAL − DCL×(FC+ITR)/(DCL−1)。 */
    fun vcFromDcl(dcl: Double, sal: Double, fc: Double, itr: Double): Double {
        if (dcl - 1 == 0.0) mathErr("除以 0")
        return sal - dcl * (fc + itr) / (dcl - 1)
    }

    /** FC = (SAL−VC)×(DCL−1)/DCL − ITR。 */
    fun fcFromDcl(dcl: Double, sal: Double, vc: Double, itr: Double): Double {
        if (dcl == 0.0) mathErr("除以 0")
        return (sal - vc) * (dcl - 1) / dcl - itr
    }

    /** ITR = (SAL−VC)×(DCL−1)/DCL − FC。 */
    fun itrFromDcl(dcl: Double, sal: Double, vc: Double, fc: Double): Double {
        if (dcl == 0.0) mathErr("除以 0")
        return (sal - vc) * (dcl - 1) / dcl - fc
    }

    // ---------- QTY CONV（CN-109/110）：SAL = PRC×QTY；VC = VCU×QTY（两组 QTY 联动） ----------

    fun sal(prc: Double, qty: Double): Double = prc * qty

    fun prcFromSal(sal: Double, qty: Double): Double {
        if (qty == 0.0) mathErr("除以 0")
        return sal / qty
    }

    fun qtyFromSal(sal: Double, prc: Double): Double {
        if (prc == 0.0) mathErr("除以 0")
        return sal / prc
    }

    fun vc(vcu: Double, qty: Double): Double = vcu * qty

    fun vcuFromVc(vc: Double, qty: Double): Double {
        if (qty == 0.0) mathErr("除以 0")
        return vc / qty
    }

    fun qtyFromVc(vc: Double, vcu: Double): Double {
        if (vcu == 0.0) mathErr("除以 0")
        return vc / vcu
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
