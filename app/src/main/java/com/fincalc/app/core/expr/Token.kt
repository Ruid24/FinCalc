package com.fincalc.app.core.expr

sealed class Token {
    data class Num(val raw: String, val value: Double) : Token()
    object Plus : Token()
    object Minus : Token()
    object Times : Token()
    object Div : Token()
    object LParen : Token()
    object RParen : Token()
    object Comma : Token()
    object Colon : Token()
    object Caret : Token()      // ^
    object Bang : Token()       // !
    object Percent : Token()    // %
    object Square : Token()     // ²（解析期归一为 ^2）
    object Cube : Token()       // ³（解析期归一为 ^3）
    object Recip : Token()      // ⁻¹（解析期归一为 ^-1）
    object SqrtTok : Token()    // √（函数式，后随 ( ）
    object CbrtTok : Token()    // ∛（函数式，后随 ( ）
    object XRootTok : Token()   // ˣ√（中缀，右操作数带括号）
    object PiTok : Token()
    object EConstTok : Token()
    data class VarTok(val name: String) : Token()   // A B C D X Y M Ans
    data class FuncTok(val fn: FuncName) : Token()
    object RanTok : Token()     // Ran#
    object PermTok : Token()    // nPr
    object CombTok : Token()    // nCr
}

enum class FuncName {
    SIN, COS, TAN, ASIN, ACOS, ATAN,
    SINH, COSH, TANH, ASINH, ACOSH, ATANH,
    LOG, LN, ABS, RND, POL, REC
}
