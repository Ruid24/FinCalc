package com.fincalc.app.core.expr

/** 角度单位（说明书 CN-24：90° = π/2 弧度 = 100 百分度）。 */
enum class AngleUnit(val toRadians: Double) {
    DEG(Math.PI / 180),
    RAD(1.0),
    GRA(Math.PI / 200)
}

/** 显示数字设定（说明书 CN-25/26）。Rnd 函数按其规则真实舍入。 */
sealed class DisplayMode {
    object Norm1 : DisplayMode()
    object Norm2 : DisplayMode()
    data class Fix(val digits: Int) : DisplayMode()
    data class Sci(val digits: Int) : DisplayMode()
}

/**
 * 求值上下文：变量/Ans 存取、角度、显示模式、随机源。
 * 完整状态机（M+/M-、STO/RCL 按键流、持久化）属后续计划，引擎只依赖本接口。
 */
interface EvalContext {
    val angle: AngleUnit
    val display: DisplayMode
    fun getVar(name: String): Double
    fun setVar(name: String, value: Double)
    fun nextRandom(): Double
}

/** 默认实现：未赋值变量为 0（与真机一致）；随机源可注入以便测试。 */
class DefaultContext(
    override var angle: AngleUnit = AngleUnit.DEG,
    override var display: DisplayMode = DisplayMode.Norm1,
    private val random: () -> Double = { kotlin.random.Random.nextDouble() }
) : EvalContext {
    private val vars = mutableMapOf<String, Double>()

    override fun getVar(name: String): Double = vars[name] ?: 0.0

    override fun setVar(name: String, value: Double) {
        vars[name] = value
    }

    override fun nextRandom(): Double = random()
}
