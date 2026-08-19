package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextTest {
    @Test
    fun `undefined variables default to zero`() {
        val ctx = DefaultContext()
        assertEquals(0.0, ctx.getVar("A"), 0.0)
        assertEquals(0.0, ctx.getVar("Ans"), 0.0)
    }

    @Test
    fun `set and get variable round trips`() {
        val ctx = DefaultContext()
        ctx.setVar("B", 57.0)
        assertEquals(57.0, ctx.getVar("B"), 0.0)
    }

    @Test
    fun `defaults are DEG and Norm1`() {
        val ctx = DefaultContext()
        assertEquals(AngleUnit.DEG, ctx.angle)
        assertEquals(DisplayMode.Norm1, ctx.display)
    }

    @Test
    fun `random source is injectable`() {
        val ctx = DefaultContext(random = { 0.583 })
        assertEquals(0.583, ctx.nextRandom(), 0.0)
    }

    @Test
    fun `angle unit conversion factors`() {
        assertEquals(Math.PI / 180, AngleUnit.DEG.toRadians, 1e-15)
        assertEquals(1.0, AngleUnit.RAD.toRadians, 0.0)
        assertEquals(Math.PI / 200, AngleUnit.GRA.toRadians, 1e-15)
    }
}
