package com.fincalc.app.core.expr

/**
 * 递归下降解析器。优先级（说明书 CN-162，高→低）：
 * 1 函数/括号；2 后缀(² ³ ⁻¹ ! %)与 ^ ˣ√；3 一元负号；5 nPr nCr；6 × ÷ 与隐式乘（同级左结合）；7 + -。
 * 权威语义：`1÷2π = (1÷2)×π`；`-2² = -(2²)`。
 */
object Parser {

    /** 嵌套深度上限，模拟真机指令堆栈（说明书：24 级），超限报 Stack ERROR。 */
    private const val MAX_DEPTH = 24

    fun parse(input: String): Program {
        val tokens = Tokenizer.tokenize(input)
        if (tokens.isEmpty()) throw CalcException(CalcException.Kind.SYNTAX, "空输入")
        val program = State(tokens).parseProgram()
        return program
    }

    private class State(val tokens: List<Token>) {
        private var pos = 0

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun next(): Token =
            tokens.getOrNull(pos)?.also { pos++ }
                ?: throw CalcException(CalcException.Kind.SYNTAX, "表达式不完整")

        private fun syntax(msg: String): Nothing = throw CalcException(CalcException.Kind.SYNTAX, msg)

        private fun stackErr(): Nothing =
            throw CalcException(CalcException.Kind.STACK, "嵌套深度超过 $MAX_DEPTH 级")

        fun parseProgram(): Program {
            val stmts = mutableListOf(parseExpr(0))
            while (peek() == Token.Colon) {
                next()
                stmts += parseExpr(0)
            }
            if (pos != tokens.size) syntax("多余的内容")
            return Program(stmts)
        }

        /** expr := muldiv (('+'|'-') muldiv)* —— 优先级 7 */
        private fun parseExpr(depth: Int): Node {
            if (depth > MAX_DEPTH) stackErr()
            var left = parseMulDiv(depth)
            while (true) {
                left = when (peek()) {
                    Token.Plus -> { next(); Node.Add(left, parseMulDiv(depth)) }
                    Token.Minus -> { next(); Node.Sub(left, parseMulDiv(depth)) }
                    else -> return left
                }
            }
        }

        /** muldiv := permcomb (('×'|'÷'|隐式) permcomb)* —— 优先级 6，同级左结合 */
        private fun parseMulDiv(depth: Int): Node {
            var left = parsePermComb(depth)
            while (true) {
                left = when {
                    peek() == Token.Times -> { next(); Node.Mul(left, parsePermComb(depth)) }
                    peek() == Token.Div -> { next(); Node.Div(left, parsePermComb(depth)) }
                    startsUnary(peek()) -> Node.ImplicitMul(left, parsePermComb(depth))
                    else -> return left
                }
            }
        }

        /** permcomb := unary (('nPr'|'nCr') unary)* —— 优先级 5 */
        private fun parsePermComb(depth: Int): Node {
            var left = parseUnary(depth)
            while (true) {
                left = when (peek()) {
                    Token.PermTok -> { next(); Node.Perm(left, parseUnary(depth)) }
                    Token.CombTok -> { next(); Node.Comb(left, parseUnary(depth)) }
                    else -> return left
                }
            }
        }

        /** unary := '-' unary | postfix —— 优先级 3（低于后缀与幂） */
        private fun parseUnary(depth: Int): Node =
            if (peek() == Token.Minus) {
                next()
                Node.Neg(parseUnary(depth))
            } else {
                parsePostfix(depth)
            }

        /** postfix := power (²|³|⁻¹|!|%)* —— 优先级 2 */
        private fun parsePostfix(depth: Int): Node {
            var e = parsePower(depth)
            while (true) {
                e = when (peek()) {
                    Token.Square -> { next(); Node.Pow(e, Node.Num("2", 2.0)) }
                    Token.Cube -> { next(); Node.Pow(e, Node.Num("3", 3.0)) }
                    Token.Recip -> { next(); Node.Pow(e, Node.Num("-1", -1.0)) }
                    Token.Bang -> { next(); Node.Fact(e) }
                    Token.Percent -> { next(); Node.Percent(e) }
                    else -> return e
                }
            }
        }

        /** power := primary (('^' powerOperand) | ('ˣ√' '(' expr ')'))? —— 优先级 2，^ 右结合 */
        private fun parsePower(depth: Int): Node {
            val base = parsePrimary(depth)
            return when (peek()) {
                Token.Caret -> { next(); Node.Pow(base, parsePowerOperand(depth)) }
                Token.XRootTok -> {
                    next()
                    expectLParen()
                    val rad = parseExpr(depth + 1)
                    expectRParen()
                    Node.XRoot(base, rad)
                }
                else -> base
            }
        }

        /** ^ 右操作数：允许前导负号（2^-3），右结合（2^3^2 = 2^(3^2)） */
        private fun parsePowerOperand(depth: Int): Node =
            if (peek() == Token.Minus) {
                next()
                Node.Neg(parsePowerOperand(depth))
            } else {
                parsePostfix(depth)
            }

        private fun parsePrimary(depth: Int): Node = when (val t = next()) {
            is Token.Num -> Node.Num(t.raw, t.value)
            Token.PiTok -> Node.Pi
            Token.EConstTok -> Node.EConst
            is Token.VarTok -> Node.Var(t.name)
            Token.RanTok -> Node.Ran
            Token.LParen -> {
                val e = parseExpr(depth + 1)
                expectRParen()
                e
            }
            Token.SqrtTok -> {
                expectLParen()
                val e = parseExpr(depth + 1)
                expectRParen()
                Node.Sqrt(e)
            }
            Token.CbrtTok -> {
                expectLParen()
                val e = parseExpr(depth + 1)
                expectRParen()
                Node.Cbrt(e)
            }
            is Token.FuncTok -> {
                expectLParen()
                val args = parseArgs(depth)
                Node.Func(t.fn, args)
            }
            else -> syntax("此处不应出现 $t")
        }

        private fun parseArgs(depth: Int): List<Node> {
            val args = mutableListOf(parseExpr(depth + 1))
            while (peek() == Token.Comma) {
                next()
                args += parseExpr(depth + 1)
            }
            expectRParen()
            return args
        }

        private fun expectLParen() {
            if (peek() == Token.LParen) next() else syntax("缺少左括号")
        }

        /** 说明书 CN-36：计算结尾处允许省略右括号。 */
        private fun expectRParen() {
            if (peek() == Token.RParen) {
                next()
            } else if (pos < tokens.size) {
                syntax("缺少右括号")
            }
        }

        /** 隐式乘法：下一个 token 能作为一元表达式的起始（负号除外——'×'后由显式规则处理） */
        private fun startsUnary(t: Token?): Boolean = when (t) {
            is Token.Num, Token.PiTok, Token.EConstTok, is Token.VarTok, Token.RanTok,
            Token.LParen, Token.SqrtTok, Token.CbrtTok, is Token.FuncTok -> true
            else -> false
        }
    }
}
