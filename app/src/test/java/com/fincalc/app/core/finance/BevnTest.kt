package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BevnTest {

    // ---- BEV（说明书例1-6，L2794-2874）：PRC=100、VCU=50、FC=100000 ----

    @Test
    fun `manual bev profit mode`() {
        // 例1/例2：PRF=0 → 损益平衡点
        assertEquals(2000.0, Bevn.qbePrf(100.0, 50.0, 100000.0, 0.0), 1e-9)
        assertEquals(200000.0, Bevn.sbePrf(100.0, 50.0, 100000.0, 0.0), 1e-6)
        // 例3/例4：PRF=400000
        assertEquals(10000.0, Bevn.qbePrf(100.0, 50.0, 100000.0, 400000.0), 1e-9)
        assertEquals(1000000.0, Bevn.sbePrf(100.0, 50.0, 100000.0, 400000.0), 1e-6)
    }

    @Test
    fun `manual bev ratio mode`() {
        // 例5/例6：r%=40
        assertEquals(10000.0, Bevn.qbeRatio(100.0, 50.0, 100000.0, 40.0), 1e-9)
        assertEquals(1000000.0, Bevn.sbeRatio(100.0, 50.0, 100000.0, 40.0), 1e-6)
    }

    @Test
    fun `bev inverse solutions round trip`() {
        // 五变量任解（L2872-2874）：由 QBE=10000 反解其余四变量
        assertEquals(100.0, Bevn.prcFromQbePrf(10000.0, 50.0, 100000.0, 400000.0), 1e-9)
        assertEquals(50.0, Bevn.vcuFromQbePrf(10000.0, 100.0, 100000.0, 400000.0), 1e-9)
        assertEquals(100000.0, Bevn.fcFromQbePrf(10000.0, 100.0, 50.0, 400000.0), 1e-6)
        assertEquals(400000.0, Bevn.prfFromQbe(10000.0, 100.0, 50.0, 100000.0), 1e-6)
        assertEquals(40.0, Bevn.rPercentFromQbe(10000.0, 100.0, 50.0, 100000.0), 1e-9)
    }

    // ---- MOS（说明书例，L2940）：SAL=1200000、SBE=1000000 ----

    @Test
    fun `manual mos`() {
        assertEquals(1.0 / 6, Bevn.mos(1200000.0, 1000000.0), 1e-12)
        assertEquals(1200000.0, Bevn.salFromMos(1.0 / 6, 1000000.0), 1e-6)
        assertEquals(1000000.0, Bevn.sbeFromMos(1.0 / 6, 1200000.0), 1e-6)
    }

    // ---- DOL（L3015）：SAL=1200000、VC=600000、FC=200000 → 1.5 ----

    @Test
    fun `manual dol`() {
        assertEquals(1.5, Bevn.dol(1200000.0, 600000.0, 200000.0), 1e-12)
        assertEquals(1200000.0, Bevn.salFromDol(1.5, 600000.0, 200000.0), 1e-6)
        assertEquals(600000.0, Bevn.vcFromDol(1.5, 1200000.0, 200000.0), 1e-6)
        assertEquals(200000.0, Bevn.fcFromDol(1.5, 1200000.0, 600000.0), 1e-6)
    }

    // ---- DFL（L3092）：EIT=400000、ITR=80000 → 1.25 ----

    @Test
    fun `manual dfl`() {
        assertEquals(1.25, Bevn.dfl(400000.0, 80000.0), 1e-12)
        assertEquals(400000.0, Bevn.eitFromDfl(1.25, 80000.0), 1e-6)
        assertEquals(80000.0, Bevn.itrFromDfl(1.25, 400000.0), 1e-6)
    }

    // ---- DCL（L3171）：SAL=1200000、VC=600000、FC=200000、ITR=100000 → 2 ----

    @Test
    fun `manual dcl`() {
        assertEquals(2.0, Bevn.dcl(1200000.0, 600000.0, 200000.0, 100000.0), 1e-12)
        assertEquals(1200000.0, Bevn.salFromDcl(2.0, 600000.0, 200000.0, 100000.0), 1e-6)
        assertEquals(600000.0, Bevn.vcFromDcl(2.0, 1200000.0, 200000.0, 100000.0), 1e-6)
        assertEquals(200000.0, Bevn.fcFromDcl(2.0, 1200000.0, 600000.0, 100000.0), 1e-6)
        assertEquals(100000.0, Bevn.itrFromDcl(2.0, 1200000.0, 600000.0, 200000.0), 1e-6)
    }

    // ---- QTY CONV（L3260）：SAL=100000、PRC=200、QTY=500；VC=15000、VCU=30、QTY=500 ----

    @Test
    fun `manual qty conv`() {
        assertEquals(500.0, Bevn.qtyFromSal(100000.0, 200.0), 1e-12)
        assertEquals(500.0, Bevn.qtyFromVc(15000.0, 30.0), 1e-12)
        assertEquals(100000.0, Bevn.sal(200.0, 500.0), 1e-9)
        assertEquals(200.0, Bevn.prcFromSal(100000.0, 500.0), 1e-12)
        assertEquals(15000.0, Bevn.vc(30.0, 500.0), 1e-9)
        assertEquals(30.0, Bevn.vcuFromVc(15000.0, 500.0), 1e-12)
    }

    @Test
    fun `division by zero throws math error`() {
        assertThrows(CalcException::class.java) { Bevn.qbePrf(50.0, 50.0, 100000.0, 0.0) }
        assertThrows(CalcException::class.java) { Bevn.qbeRatio(50.0, 50.0, 100000.0, 0.0) }
        assertThrows(CalcException::class.java) { Bevn.mos(0.0, 100.0) }
        assertThrows(CalcException::class.java) { Bevn.dol(100.0, 50.0, 50.0) }
        assertThrows(CalcException::class.java) { Bevn.dfl(100.0, 100.0) }
        assertThrows(CalcException::class.java) { Bevn.dcl(100.0, 50.0, 30.0, 20.0) }
        assertThrows(CalcException::class.java) { Bevn.qtyFromSal(100.0, 0.0) }
    }
}
