package com.fincalc.app.core.solver

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.pow

class SolverTest {
    @Test
    fun `sqrt2 by newton`() {
        val r = Solver.solve({ x -> x * x - 2 }, x0 = 1.0, lower = 0.0, upper = 2.0)
        assertEquals(1.4142135623730951, r, 1e-12)
    }

    @Test
    fun `cos x minus x fixed point`() {
        val r = Solver.solve({ x -> cos(x) - x }, x0 = 0.5, lower = 0.0, upper = 1.0)
        assertEquals(0.7390851332151607, r, 1e-9)
    }

    @Test
    fun `irr style npv equation`() {
        val f = { i: Double -> -1000 + 300 / (1 + i) + 400 / (1 + i).pow(2) + 500 / (1 + i).pow(3) }
        val r = Solver.solve(f, x0 = 0.1, lower = 0.0, upper = 1.0)
        assertEquals(0.0, f(r), 1e-8)
        assertTrue(r in 0.08..0.10)
    }

    @Test
    fun `zero derivative falls back to bisection`() {
        val r = Solver.solve({ x -> x * x - 4 }, x0 = 0.0, lower = 0.0, upper = 5.0)
        assertEquals(2.0, r, 1e-9)
    }

    @Test
    fun `cycling newton falls back to bisection`() {
        val f = { x: Double -> x * x * x - 2 * x + 2 }
        val r = Solver.solve(f, x0 = 0.0, lower = -5.0, upper = -1.0)
        assertEquals(-1.7692923542386314, r, 1e-9)
    }

    @Test
    fun `no sign change throws math error`() {
        val e = assertThrows(CalcException::class.java) {
            Solver.solve({ x -> x * x + 1 }, x0 = 1.0, lower = 0.0, upper = 2.0)
        }
        assertEquals(CalcException.Kind.MATH, e.kind)
    }

    @Test
    fun `max iteration exceeded throws`() {
        assertThrows(CalcException::class.java) {
            Solver.solve({ x -> x * x - 2 }, x0 = 1.0, lower = 0.0, upper = 2.0, maxIterations = 1)
        }
    }

    @Test
    fun `root at bracket endpoint`() {
        val r = Solver.solve({ x -> x * x - 4 }, x0 = 0.5, lower = 2.0, upper = 10.0)
        assertEquals(2.0, r, 0.0)
    }
}
