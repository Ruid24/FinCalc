package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BondTest {
    // 说明书例（CN-86~88）：Date、365、Annual，d1=2022-06-01，d2=2024-12-15，RDV=100，CPN=3，YLD=4
    private val d1 = Days.Date(2022, 6, 1)
    private val d2 = Days.Date(2024, 12, 15)

    @Test
    fun `manual example 1 prc date mode`() {
        // A=168、D=365、B=197、N=3（日程推算已验证）
        // 说明书设定值表 10 位显示 PRC=−97.6151555；此处按全精度参考值断言
        val r = Bond.prcDate(d1, d2, 100.0, 3.0, 4.0, 1, days360 = false)
        assertEquals(-97.61515550118818, r.prc, 1e-9)
        assertEquals(-1.3808219178082193, r.int, 1e-12)   // 参考值（截图缺失）
        assertEquals(-98.9959774189964, r.cst, 1e-9)      // 参考值
    }

    @Test
    fun `manual example 2 yld date round trip`() {
        // 例2：以 PRC=−97.6151555 反解 YLD → 4
        val y = Bond.yldDate(d1, d2, 100.0, 3.0, -97.6151555, 1, false)
        assertEquals(4.0, y, 1e-6)
    }

    @Test
    fun `manual example 3 prc term mode`() {
        // 例3：Term、n=3（前置 Date Mode=360、Annual 属设置层）
        assertEquals(-97.22490896677286, Bond.prcTerm(3, 100.0, 3.0, 4.0, 1), 1e-9)
    }

    @Test
    fun `manual example 4 yld term`() {
        // 例4：Term、n=3、PRC=−97.6151555 → YLD（Term 口径，参考值）
        assertEquals(3.857044586, Bond.yldTerm(3, 100.0, 3.0, -97.6151555, 1), 1e-6)
    }

    @Test
    fun `zero coupon bond`() {
        // 零息票（CN-86 L2562）：CPN=0 → 纯贴现
        val p = Bond.prcTerm(3, 100.0, 0.0, 4.0, 1)
        assertEquals(-100.0 / 1.04 / 1.04 / 1.04, p, 1e-9)
    }

    @Test
    fun `semi annual schedule`() {
        // Semi（Periods/Y=2）：票息日为每年 6/15 与 12/15；2022-06-01 在 2021-12-15 与 2022-06-15 之间
        // A=168（2021-12-15→2022-06-01），D=182（2021-12-15→2022-06-15），B=14，N=6
        val r = Bond.prcDate(d1, d2, 100.0, 3.0, 4.0, 2, days360 = false)
        // 参考值（按 CN-88/89 公式高精度计算）
        assertEquals(-97.60774696391445, r.prc, 1e-9)
        assertEquals(-1.3846153846153846, r.int, 1e-12)
        assertEquals(-98.99236234852984, r.cst, 1e-9)
    }

    @Test
    fun `par bond prices at minus 100`() {
        // 票面利率 = 收益率时平价：PRC = −100（Term、Annual、n=5）
        assertEquals(-100.0, Bond.prcTerm(5, 100.0, 5.0, 5.0, 1), 1e-9)
    }

    @Test
    fun `error conditions`() {
        // CN-168：PRC 计算要求 RDV>0、CPN≥0；YLD 要求 RDV>0、PRC<0
        assertThrows(CalcException::class.java) { Bond.prcTerm(3, 0.0, 3.0, 4.0, 1) }
        assertThrows(CalcException::class.java) { Bond.prcTerm(3, 100.0, -1.0, 4.0, 1) }
        assertThrows(CalcException::class.java) { Bond.yldTerm(3, 100.0, 3.0, 97.0, 1) }   // PRC 非负
        assertThrows(CalcException::class.java) { Bond.prcTerm(0, 100.0, 3.0, 4.0, 1) }    // n<1
        val e = assertThrows(CalcException::class.java) { Bond.prcTerm(3, 100.0, 3.0, 4.0, 3) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
        assertThrows(CalcException::class.java) {
            Bond.prcDate(d2, d1, 100.0, 3.0, 4.0, 1, false)   // d1 ≥ d2
        }
    }
}
