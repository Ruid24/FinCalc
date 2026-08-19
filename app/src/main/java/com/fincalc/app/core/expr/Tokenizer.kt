package com.fincalc.app.core.expr

/** 分词器：把键盘产生的线性输入串（语法规范见计划 2 头部）切成 Token 序列。 */
object Tokenizer {

    // 函数名最长匹配优先（sinh 先于 sin），避免前缀吞并
    private val FUNC_NAMES: List<Pair<String, FuncName>> = mapOf(
        "sin" to FuncName.SIN, "cos" to FuncName.COS, "tan" to FuncName.TAN,
        "asin" to FuncName.ASIN, "acos" to FuncName.ACOS, "atan" to FuncName.ATAN,
        "sinh" to FuncName.SINH, "cosh" to FuncName.COSH, "tanh" to FuncName.TANH,
        "asinh" to FuncName.ASINH, "acosh" to FuncName.ACOSH, "atanh" to FuncName.ATANH,
        "log" to FuncName.LOG, "ln" to FuncName.LN,
        "Abs" to FuncName.ABS, "Rnd" to FuncName.RND,
        "Pol" to FuncName.POL, "Rec" to FuncName.REC
    ).entries.sortedByDescending { it.key.length }.map { it.toPair() }

    fun tokenize(input: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        fun err(msg: String): Nothing = throw CalcException(CalcException.Kind.SYNTAX, "$msg（位置 $i）")
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || (c == '.' && i + 1 < input.length && input[i + 1].isDigit()) -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    if (i < input.length && input[i] == 'E') {
                        var j = i + 1
                        if (j < input.length && (input[j] == '+' || input[j] == '-')) j++
                        if (j < input.length && input[j].isDigit()) {
                            i = j
                            while (i < input.length && input[i].isDigit()) i++
                        }
                    }
                    val raw = input.substring(start, i)
                    val value = try {
                        raw.toDouble()
                    } catch (e: NumberFormatException) {
                        throw CalcException(CalcException.Kind.SYNTAX, "非法数字 '$raw'")
                    }
                    out += Token.Num(raw, value)
                }

                c == '+' -> { out += Token.Plus; i++ }
                c == '-' -> { out += Token.Minus; i++ }
                c == '×' -> { out += Token.Times; i++ }
                c == '÷' -> { out += Token.Div; i++ }
                c == '(' -> { out += Token.LParen; i++ }
                c == ')' -> { out += Token.RParen; i++ }
                c == ',' -> { out += Token.Comma; i++ }
                c == ':' -> { out += Token.Colon; i++ }
                c == '^' -> { out += Token.Caret; i++ }
                c == '!' -> { out += Token.Bang; i++ }
                c == '%' -> { out += Token.Percent; i++ }
                c == 'π' -> { out += Token.PiTok; i++ }
                c == '√' -> { out += Token.SqrtTok; i++ }
                c == '∛' -> { out += Token.CbrtTok; i++ }
                c == '²' -> { out += Token.Square; i++ }
                c == '³' -> { out += Token.Cube; i++ }
                input.startsWith("⁻¹", i) -> { out += Token.Recip; i += 2 }
                input.startsWith("ˣ√", i) -> { out += Token.XRootTok; i += 2 }
                input.startsWith("Ran#", i) -> { out += Token.RanTok; i += 4 }
                input.startsWith("nPr", i) -> { out += Token.PermTok; i += 3 }
                input.startsWith("nCr", i) -> { out += Token.CombTok; i += 3 }

                c.isLetter() -> {
                    val fn = FUNC_NAMES.firstOrNull { input.startsWith(it.first, i) }
                    when {
                        fn != null -> { out += Token.FuncTok(fn.second); i += fn.first.length }
                        input.startsWith("Ans", i) -> { out += Token.VarTok("Ans"); i += 3 }
                        c == 'e' -> { out += Token.EConstTok; i++ }
                        c in "ABCDXYM" -> { out += Token.VarTok(c.toString()); i++ }
                        else -> err("无法识别的名称")
                    }
                }

                else -> err("无法识别的字符 '$c'")
            }
        }
        return out
    }
}
