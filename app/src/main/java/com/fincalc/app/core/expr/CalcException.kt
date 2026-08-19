package com.fincalc.app.core.expr

/** 计算器错误。[kind] 决定屏幕显示的错误名（与说明书 CN-169 错误信息表一致）。 */
class CalcException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind(val display: String) {
        MATH("Math ERROR"),
        SYNTAX("Syntax ERROR"),
        STACK("Stack ERROR"),
        INSUFFICIENT_MEM("Insufficient MEM"),
        ARGUMENT("Argument ERROR")
    }
}
