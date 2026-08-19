package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CmpdTest {
    private val end = Cmpd.Payment.END
    private val bgn = Cmpd.Payment.BEGIN
    private val ci = Cmpd.OddPeriod.CI
    private val si = Cmpd.OddPeriod.SI

    // ---- 说明书例1（L1545-1583）：End，n=48，I%=4，PV=−1000，PMT=−300，P/Y=C/Y=12 ----

    @Test
    fun `manual example solve fv`() {
        // 期望值按说明书公式体系高精度计算（答案在说明书为截图；表格近似值 $16,760）
        assertEquals(
            16761.07896780279,
            Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci),
            1e-4
        )
    }

    @Test
    fun `manual example begin variant`() {
        // 同例改为期初付款（说明书无 Begin 例题，按公式体系参考值）
        assertEquals(
            16813.03856879555,
            Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, bgn, ci),
            1e-4
        )
    }

    @Test
    fun `odd period dn ci vs si`() {
        // n=48.5（16 个月 20 天式输入，CN-56）：dn=CI 与 dn=SI 结果不同
        assertEquals(
            16763.032672186913,
            Cmpd.solveFV(48.5, 4.0, -1000.0, -300.0, 12, 12, end, ci),
            1e-4
        )
        assertEquals(
            16763.034298919418,
            Cmpd.solveFV(48.5, 4.0, -1000.0, -300.0, 12, 12, end, si),
            1e-4
        )
    }

    @Test
    fun `zero interest special case`() {
        // CN-58 特例：PV=−(PMT×n+FV)；PMT=−(PV+FV)/n；FV=−(PMT×n+PV)；n=−(PV+FV)/PMT
        assertEquals(2000.0, Cmpd.solveFV(10.0, 0.0, -1000.0, -100.0, 1, 1, end, ci), 1e-12)
        assertEquals(-100.0, Cmpd.solvePMT(10.0, 0.0, -1000.0, 2000.0, 1, 1, end, ci), 1e-12)
        assertEquals(-1000.0, Cmpd.solvePV(10.0, 0.0, -100.0, 2000.0, 1, 1, end, ci), 1e-12)
        assertEquals(10.0, Cmpd.solveN(0.0, -1000.0, -100.0, 2000.0, 1, 1, end, ci), 1e-12)
    }

    @Test
    fun `py differs from cy`() {
        // P/Y=1、C/Y=2（年付、半年复利）：i = (1+0.06/2)^2 − 1 = 0.0609
        assertEquals(0.0609, Cmpd.periodRate(6.0, 1, 2), 1e-15)
        assertEquals(
            1806.1112346694129,
            Cmpd.solveFV(10.0, 6.0, -1000.0, 0.0, 1, 2, end, ci),
            1e-6
        )
    }

    // ---- 往返测试（说明书无此类例题：先顺求 FV，再以其反解其余四变量） ----

    @Test
    fun `solve i round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        assertEquals(4.0, Cmpd.solveI(48.0, -1000.0, -300.0, fv, 12, 12, end, ci), 1e-6)
    }

    @Test
    fun `solve n round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        assertEquals(48.0, Cmpd.solveN(4.0, -1000.0, -300.0, fv, 12, 12, end, ci), 1e-9)
    }

    @Test
    fun `solve pv pmt round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        assertEquals(-1000.0, Cmpd.solvePV(48.0, 4.0, -300.0, fv, 12, 12, end, ci), 1e-6)
        assertEquals(-300.0, Cmpd.solvePMT(48.0, 4.0, -1000.0, fv, 12, 12, end, ci), 1e-6)
    }

    @Test
    fun `solve i begin round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, bgn, ci)
        assertEquals(4.0, Cmpd.solveI(48.0, -1000.0, -300.0, fv, 12, 12, bgn, ci), 1e-6)
    }

    @Test
    fun `solve i negative rate`() {
        // 亏损场景：PV=−1000，PMT=0，I%=−5，n=10 → FV = 1000×0.95^10，再反解利率
        val fv = Cmpd.solveFV(10.0, -5.0, -1000.0, 0.0, 1, 1, end, ci)
        assertEquals(598.7369392383787, fv, 1e-9)
        assertEquals(-5.0, Cmpd.solveI(10.0, -1000.0, 0.0, fv, 1, 1, end, ci), 1e-9)
    }

    // ---- 错误条件（CN-168 与范围表） ----

    @Test
    fun `i percent at or below minus 100 throws math error`() {
        assertThrows(CalcException::class.java) {
            Cmpd.solveFV(48.0, -100.0, -1000.0, -300.0, 12, 12, end, ci)
        }
        assertThrows(CalcException::class.java) {
            Cmpd.periodRate(-101.0, 12, 12)
        }
    }

    @Test
    fun `non positive n throws math error`() {
        assertThrows(CalcException::class.java) {
            Cmpd.solveFV(0.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        }
    }

    @Test
    fun `solve i with all same sign throws math error`() {
        // 残差 γ·PV+α·PMT+β·FV 恒为负（系数恒正），无解 → Math ERROR
        assertThrows(CalcException::class.java) {
            Cmpd.solveI(48.0, -1000.0, -300.0, -100.0, 12, 12, end, ci)
        }
    }

    @Test
    fun `invalid py cy throws argument error`() {
        val e = assertThrows(CalcException::class.java) {
            Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 0, 12, end, ci)
        }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
        assertThrows(CalcException::class.java) {
            Cmpd.periodRate(4.0, 12, 10000)
        }
    }

    @Test
    fun `zero interest with non positive n throws math error`() {
        // 审查发现：I%=0 捷径曾绕过 n≤0 校验，静默返回 −Infinity
        assertThrows(CalcException::class.java) {
            Cmpd.solvePMT(0.0, 0.0, -1000.0, 2000.0, 1, 1, end, ci)
        }
        assertThrows(CalcException::class.java) {
            Cmpd.solvePV(-5.0, 0.0, -100.0, 2000.0, 1, 1, end, ci)
        }
    }

    @Test
    fun `solve n non positive result throws math error`() {
        // 审查发现：PV=−1000、PMT=−100、FV=+500、I%=4 场景公式直译得负 n（约 −4.89），
        // 正利率下投入多拿回少属无解除 → Math ERROR（CN-168 n≤0 精神的延伸）
        assertThrows(CalcException::class.java) {
            Cmpd.solveN(4.0, -1000.0, -100.0, 500.0, 12, 12, end, ci)
        }
    }

    @Test
    fun `zero interest still validates py cy`() {
        // 终审发现：I%=0 捷径曾跳过 checkPyCy，py/cy 越界静默通过
        assertThrows(CalcException::class.java) {
            Cmpd.solveFV(10.0, 0.0, -1000.0, -100.0, 0, 12, end, ci)
        }
    }
}
