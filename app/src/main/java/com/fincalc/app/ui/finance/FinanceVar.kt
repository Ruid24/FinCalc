package com.fincalc.app.ui.finance

/** 金融模式变量描述（FC-200V 变量列表的一行）。 */
data class FinanceVar(
    val key: String,             // VARS 键名（共享变量容器）
    val label: String,           // 显示名（英文缩写，真机一致）
    val solvable: Boolean,       // 可否作为 SOLVE 目标
    val formula: String,         // 长按显示的公式（学习辅助，线性文本）
    val integer: Boolean = false // 是否整数输入（如 PM1/PM2、n）
)

/** 模式界面声明：变量表 + 求解目标 + 设置联动说明。 */
data class ModeScreenSpec(
    val title: String,           // 界面标题（模式名）
    val vars: List<FinanceVar>
)
