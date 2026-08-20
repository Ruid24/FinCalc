package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class StatTest {

    // ---- 例4-6（L4231-4298）：1-VAR + FREQ ----
    private val oneVarData = listOf(
        Stat.Entry(0.0, freq = 1.0), Stat.Entry(1.0, freq = 2.0), Stat.Entry(2.0, freq = 1.0),
        Stat.Entry(3.0, freq = 2.0), Stat.Entry(4.0, freq = 2.0), Stat.Entry(5.0, freq = 2.0),
        Stat.Entry(6.0, freq = 3.0), Stat.Entry(7.0, freq = 4.0), Stat.Entry(9.0, freq = 2.0),
        Stat.Entry(10.0, freq = 1.0)
    )

    @Test
    fun `manual one var with freq`() {
        assertEquals(20.0, Stat.count(oneVarData), 0.0)          // 原文 L4276
        assertEquals(102.0, Stat.sumX(oneVarData), 1e-12)        // 原文 L4254
        assertEquals(672.0, Stat.sumX2(oneVarData), 1e-12)       // 原文 L4246
        assertEquals(5.1, Stat.meanX(oneVarData), 1e-12)         // 原文 L4284
        assertEquals(2.754995463, Stat.stdXn(oneVarData), 1e-9)  // 原文 L4292
        assertEquals(2.8265657049165736, Stat.stdXn1(oneVarData), 1e-12)
        assertEquals(0.0, Stat.minX(oneVarData), 0.0)
        assertEquals(10.0, Stat.maxX(oneVarData), 0.0)
    }

    // ---- 例7-10（L4347-4446）：线性回归数据 ----
    private val linearData = listOf(
        Stat.Entry(1.0, 1.0), Stat.Entry(1.2, 1.1), Stat.Entry(1.5, 1.2),
        Stat.Entry(1.6, 1.3), Stat.Entry(1.9, 1.4), Stat.Entry(2.1, 1.5),
        Stat.Entry(2.4, 1.6), Stat.Entry(2.5, 1.7), Stat.Entry(2.7, 1.8),
        Stat.Entry(3.0, 2.0)
    )

    @Test
    fun `two var sums and stats`() {
        assertEquals(10.0, Stat.count(linearData), 0.0)
        assertEquals(19.9, Stat.sumX(linearData), 1e-12)
        assertEquals(14.6, Stat.sumY(linearData), 1e-12)
        assertEquals(0.63, Stat.stdXn(linearData), 1e-12)        // 例8 原文 ≈0.63
        assertEquals(0.6640783086353597, Stat.stdXn1(linearData), 1e-12)
        assertEquals(1.99, Stat.meanX(linearData), 1e-12)
        assertEquals(1.46, Stat.meanY(linearData), 1e-12)
        assertEquals(102.45100000000001, Stat.sumX3(linearData), 1e-9)
        assertEquals(71.244, Stat.sumX2Y(linearData), 1e-9)
        assertEquals(253.5541, Stat.sumX4(linearData), 1e-9)
        assertEquals(1.0, Stat.minX(linearData), 0.0)
        assertEquals(3.0, Stat.maxX(linearData), 0.0)
        assertEquals(2.0, Stat.maxY(linearData), 0.0)            // 原文 L4408
    }

    @Test
    fun `manual linear regression`() {
        val r = Stat.regress(Stat.RegType.LINEAR, linearData)
        assertEquals(0.5043587805492551, r.a, 1e-12)
        assertEquals(0.48022171831695787, r.b, 1e-12)            // 说明书 10 位显示 0.4802217183（L4428）
        assertEquals(0.9952824845978723, r.r!!, 1e-12)           // 说明书 10 位显示 0.9952824846（L4432）
        assertNull(r.c)
        assertEquals(-7.297376705, Stat.estimateX(Stat.RegType.LINEAR, r, -3.0), 1e-9)  // 原文 L4440
        assertEquals(1.464802217, Stat.estimateY(Stat.RegType.LINEAR, r, 2.0), 1e-9)    // 原文 L4446
    }

    // ---- 例11-13（L4529-4580）：二次回归 ----

    @Test
    fun `manual quadratic regression`() {
        val r = Stat.regress(Stat.RegType.QUADRATIC, linearData)
        assertEquals(0.7028598638, r.a, 1e-9)                    // 原文 L4529
        assertEquals(0.25763843788924545, r.b, 1e-12)
        assertEquals(0.05610274152791289, r.c!!, 1e-12)
        assertNull(r.r)                                          // 二次回归无 r（CN-149）
        val (x1, x2) = Stat.estimateXQuadratic(r, 3.0)
        assertEquals(4.502211457, x1, 1e-9)                      // 原文 L4552
        assertEquals(-9.094472563, x2, 1e-9)                     // 原文 L4580
        assertEquals(1.442547706, Stat.estimateY(Stat.RegType.QUADRATIC, r, 2.0), 1e-9)
    }

    // ---- 例15（L4741-4839）：对数回归 y = A + B·ln x ----
    private val logData = listOf(
        Stat.Entry(29.0, 1.6), Stat.Entry(50.0, 23.5), Stat.Entry(74.0, 38.0),
        Stat.Entry(103.0, 46.4), Stat.Entry(118.0, 48.9)
    )

    @Test
    fun `manual log regression`() {
        // 答案为截图；期望值为公式体系参考值
        val r = Stat.regress(Stat.RegType.LOG, logData)
        assertEquals(-111.1283976473655, r.a, 1e-9)
        assertEquals(34.02014750160489, r.b, 1e-9)
        assertEquals(0.994013946616563, r.r!!, 1e-12)
        assertEquals(37.94879482020123, Stat.estimateY(Stat.RegType.LOG, r, 80.0), 1e-9)
        assertEquals(224.15413126072139, Stat.estimateX(Stat.RegType.LOG, r, 73.0), 1e-6)
    }

    // ---- 例16（L4842-4926）：e 指数回归 y = A·e^(Bx) ----
    private val expData = listOf(
        Stat.Entry(6.9, 21.4), Stat.Entry(12.9, 15.7), Stat.Entry(19.8, 12.1),
        Stat.Entry(26.7, 8.5), Stat.Entry(35.1, 5.2)
    )

    @Test
    fun `manual exp regression`() {
        val r = Stat.regress(Stat.RegType.EXP, expData)
        assertEquals(30.49758742585542, r.a, 1e-9)               // 说明书 10 位显示 30.49758743（L4888）
        assertEquals(-0.04920370830766393, r.b, 1e-12)
        assertEquals(-0.997247352, r.r!!, 1e-9)                  // 原文 L4903
        assertEquals(13.879157394259396, Stat.estimateY(Stat.RegType.EXP, r, 16.0), 1e-9)   // 说明书显示 13.87915739（L4915）
        assertEquals(8.574868047, Stat.estimateX(Stat.RegType.EXP, r, 20.0), 1e-9)   // 原文 L4926
    }

    // ---- 例17（L4928-5013）：ab 指数回归 y = A·B^x ----
    private val abData = listOf(
        Stat.Entry(-1.0, 0.24), Stat.Entry(3.0, 4.0),
        Stat.Entry(5.0, 16.2), Stat.Entry(10.0, 513.0)
    )

    @Test
    fun `manual ab exp regression`() {
        val r = Stat.regress(Stat.RegType.AB_EXP, abData)
        assertEquals(0.48886664, r.a, 1e-9)                      // 原文 L4976
        assertEquals(2.0074993437791706, r.b, 1e-12)
        assertEquals(0.9999873551795408, r.r!!, 1e-12)
        assertEquals(1.055357865, Stat.estimateX(Stat.RegType.AB_EXP, r, 1.02), 1e-9)  // 原文 L5013
        assertEquals(16944.2200173692, Stat.estimateY(Stat.RegType.AB_EXP, r, 15.0), 1e-6)
    }

    // ---- 例18（L5016-5068）：幂回归 y = A·x^B ----
    private val powerData = listOf(
        Stat.Entry(28.0, 2410.0), Stat.Entry(30.0, 3033.0), Stat.Entry(33.0, 3895.0),
        Stat.Entry(35.0, 4491.0), Stat.Entry(38.0, 5717.0)
    )

    @Test
    fun `manual power regression`() {
        val r = Stat.regress(Stat.RegType.POWER, powerData)
        assertEquals(0.23880106853373598, r.a, 1e-12)
        assertEquals(2.771866158, r.b, 1e-9)                     // 原文 L5057
        assertEquals(0.9989062551, r.r!!, 1e-9)                  // 原文 L5061
        assertEquals(6587.674589, Stat.estimateY(Stat.RegType.POWER, r, 40.0), 1e-6)   // 原文 L5065
        assertEquals(20.262256810920633, Stat.estimateX(Stat.RegType.POWER, r, 1000.0), 1e-9)
    }

    // ---- 例19（L5071-5162）：倒数回归 y = A + B/x ----
    private val recipData = listOf(
        Stat.Entry(1.1, 18.3), Stat.Entry(2.1, 9.7), Stat.Entry(2.9, 6.8),
        Stat.Entry(4.0, 4.9), Stat.Entry(4.9, 4.1)
    )

    @Test
    fun `manual reciprocal regression`() {
        val r = Stat.regress(Stat.RegType.RECIPROCAL, recipData)
        assertEquals(-0.09344061817312763, r.a, 1e-12)
        assertEquals(20.267097114570788, r.b, 1e-12)
        assertEquals(0.9998526952656159, r.r!!, 1e-12)
        assertEquals(5.697158557, Stat.estimateY(Stat.RegType.RECIPROCAL, r, 3.5), 1e-9)  // 原文 L5152
        assertEquals(1.342775158, Stat.estimateX(Stat.RegType.RECIPROCAL, r, 15.0), 1e-9)  // 原文 L5154
    }

    // ---- 错误条件 ----

    @Test
    fun `error conditions`() {
        // 空数据
        assertThrows(CalcException::class.java) { Stat.count(emptyList()) }
        assertThrows(CalcException::class.java) { Stat.regress(Stat.RegType.LINEAR, emptyList()) }
        // 样本数不足（xσn−1 需 n>1）
        assertThrows(CalcException::class.java) { Stat.stdXn1(listOf(Stat.Entry(5.0))) }
        // 负频率
        assertThrows(CalcException::class.java) { Stat.count(listOf(Stat.Entry(5.0, freq = -1.0))) }
        // 1-VAR 数据用于回归（y 缺失）
        assertThrows(CalcException::class.java) { Stat.sumY(oneVarData) }
        // 对数/幂回归定义域：x ≤ 0 或 y ≤ 0
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.LOG, listOf(Stat.Entry(-1.0, 1.0), Stat.Entry(2.0, 2.0)))
        }
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.EXP, listOf(Stat.Entry(1.0, -1.0), Stat.Entry(2.0, 2.0)))
        }
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.POWER, listOf(Stat.Entry(1.0, 1.0), Stat.Entry(0.0, 2.0)))
        }
        // 倒数回归 x = 0
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.RECIPROCAL, listOf(Stat.Entry(0.0, 1.0), Stat.Entry(2.0, 2.0)))
        }
        // 线性回归 x 全部相同 → 分母为 0
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.LINEAR, listOf(Stat.Entry(1.0, 1.0), Stat.Entry(1.0, 2.0)))
        }
        // 二次回归判别式为负
        val qr = Stat.regress(Stat.RegType.QUADRATIC, linearData)
        assertThrows(CalcException::class.java) { Stat.estimateXQuadratic(qr, -100.0) }
        // estimateX 对二次回归报错
        assertThrows(CalcException::class.java) { Stat.estimateX(Stat.RegType.QUADRATIC, qr, 3.0) }
    }
}
