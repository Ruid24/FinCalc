package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenizerTest {
    @Test
    fun `number with exponent`() {
        val t = Tokenizer.tokenize("1.2E3")
        assertEquals(listOf(Token.Num("1.2E3", 1200.0)), t)
    }

    @Test
    fun `number with negative exponent`() {
        val t = Tokenizer.tokenize("1.2E-3")
        assertEquals(listOf(Token.Num("1.2E-3", 0.0012)), t)
    }

    @Test
    fun `leading dot number`() {
        val t = Tokenizer.tokenize(".5")
        assertEquals(listOf(Token.Num(".5", 0.5)), t)
    }

    @Test
    fun `operators and parens`() {
        val t = Tokenizer.tokenize("2+3×(4-1)÷2")
        assertEquals(
            listOf(
                Token.Num("2", 2.0), Token.Plus,
                Token.Num("3", 3.0), Token.Times,
                Token.LParen, Token.Num("4", 4.0), Token.Minus, Token.Num("1", 1.0), Token.RParen,
                Token.Div, Token.Num("2", 2.0)
            ),
            t
        )
    }

    @Test
    fun `constants variables ans`() {
        val t = Tokenizer.tokenize("2πA+Ans-e")
        assertEquals(
            listOf(
                Token.Num("2", 2.0), Token.PiTok, Token.VarTok("A"),
                Token.Plus, Token.VarTok("Ans"), Token.Minus, Token.EConstTok
            ),
            t
        )
    }

    @Test
    fun `function longest match sinh not sin`() {
        val t = Tokenizer.tokenize("sinh(1)")
        assertEquals(listOf(Token.FuncTok(FuncName.SINH), Token.LParen, Token.Num("1", 1.0), Token.RParen), t)
    }

    @Test
    fun `postfix and special tokens`() {
        val t = Tokenizer.tokenize("5!+2²+3³+4⁻¹+15%")
        assertEquals(
            listOf(
                Token.Num("5", 5.0), Token.Bang, Token.Plus,
                Token.Num("2", 2.0), Token.Square, Token.Plus,
                Token.Num("3", 3.0), Token.Cube, Token.Plus,
                Token.Num("4", 4.0), Token.Recip, Token.Plus,
                Token.Num("15", 15.0), Token.Percent
            ),
            t
        )
    }

    @Test
    fun `roots perm comb ran colon comma`() {
        val t = Tokenizer.tokenize("√(2):∛(5):5ˣ√(32):10 nPr 4:10 nCr 4:Ran#:Pol(1,2)")
        val expect = listOf(
            Token.SqrtTok, Token.LParen, Token.Num("2", 2.0), Token.RParen, Token.Colon,
            Token.CbrtTok, Token.LParen, Token.Num("5", 5.0), Token.RParen, Token.Colon,
            Token.Num("5", 5.0), Token.XRootTok, Token.LParen, Token.Num("32", 32.0), Token.RParen, Token.Colon,
            Token.Num("10", 10.0), Token.PermTok, Token.Num("4", 4.0), Token.Colon,
            Token.Num("10", 10.0), Token.CombTok, Token.Num("4", 4.0), Token.Colon,
            Token.RanTok, Token.Colon,
            Token.FuncTok(FuncName.POL), Token.LParen, Token.Num("1", 1.0), Token.Comma, Token.Num("2", 2.0), Token.RParen
        )
        assertEquals(expect, t)
    }

    @Test
    fun `whitespace is skipped`() {
        val t = Tokenizer.tokenize("  1 + 2 ")
        assertEquals(listOf(Token.Num("1", 1.0), Token.Plus, Token.Num("2", 2.0)), t)
    }

    @Test
    fun `unknown char throws syntax error`() {
        val e = assertThrows(CalcException::class.java) { Tokenizer.tokenize("1\$2") }
        assertEquals(CalcException.Kind.SYNTAX, e.kind)
    }

    @Test
    fun `bad number throws syntax error`() {
        assertThrows(CalcException::class.java) { Tokenizer.tokenize("1.2.3") }
    }

    @Test
    fun `lowercase variable name not accepted`() {
        assertThrows(CalcException::class.java) { Tokenizer.tokenize("a+1") }
    }

    @Test
    fun `num token equality includes raw`() {
        val a = Tokenizer.tokenize("1.20")
        assertTrue(a[0] is Token.Num && (a[0] as Token.Num).raw == "1.20")
    }
}
