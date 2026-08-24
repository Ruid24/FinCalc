package com.fincalc.app.core.render

/** 文本测量接口（UI 层用 Compose TextMeasurer 实现；测试用假测量器）。返回 (宽, 高)。 */
fun interface TextMeasure {
    fun measure(text: String, sizeScale: Float): Pair<Float, Float>
}

/** 排版盒子。所有尺寸单位 px；[baseline] 为基线距盒子顶部的距离。 */
sealed class MathBox {
    abstract val width: Float
    abstract val height: Float
    abstract val baseline: Float
}

/** 文本叶子。[scale] 为相对基准字号的缩放（上标/下标 SCRIPT 缩小）。 */
class TextBox(
    val text: String,
    val scale: Float,
    override val width: Float,
    override val height: Float,
    override val baseline: Float
) : MathBox()

/** 水平排列（基线对齐）。 */
class RowBox(val children: List<MathBox>) : MathBox() {
    override val width: Float = children.fold(0f) { acc, b -> acc + b.width }
    override val baseline: Float = children.maxOfOrNull { it.baseline } ?: 0f
    override val height: Float = baseline + (children.maxOfOrNull { it.height - it.baseline } ?: 0f)
}

/** 分数线：分子在上、分母在下、中间横线；基线取横线中心。 */
class FracBox(val num: MathBox, val den: MathBox, em: Float) : MathBox() {
    val pad = 0.2f * em
    val gap = 0.15f * em
    val lineThickness = 0.06f * em
    override val width = maxOf(num.width, den.width) + 2 * pad
    val lineY = num.height + gap + lineThickness / 2
    override val baseline = lineY
    override val height = num.height + 2 * gap + lineThickness + den.height
    val denTop = num.height + 2 * gap + lineThickness
}

/** 上标（sup 缩小抬升）。sup 超出 base 顶部时整体上移 lift，保证墨迹落在盒内。 */
class SupBox(val base: MathBox, val sup: MathBox, em: Float) : MathBox() {
    val shiftUp = 0.45f * em
    /** 内容下移量：sup 的顶部不低于盒顶（审查修复：原契约 baseline/height/supTop 自相矛盾）。 */
    val lift = maxOf(0f, sup.baseline + shiftUp - base.baseline)
    override val width = base.width + sup.width
    override val baseline = base.baseline + lift
    override val height = base.height + lift
    /** base 距盒顶的偏移（= lift）。 */
    val baseTop = lift
    /** sup 距盒顶的偏移（非负）。 */
    val supTop get() = baseline - shiftUp - sup.baseline
}

/** 下标（sub 缩小下移；仅 log 底数用；基线同底）。 */
class SubBox(val base: MathBox, val sub: MathBox, em: Float) : MathBox() {
    val shiftDown = 0.25f * em
    override val width = base.width + sub.width
    override val baseline = base.baseline
    override val height = maxOf(base.height, base.baseline + shiftDown + (sub.height - sub.baseline))
}

/** 根号：左侧根号符号位 + 顶部横线覆盖内容；index 为左上次数（∛/ˣ√）。 */
class SqrtBox(val content: MathBox, val index: MathBox?, em: Float) : MathBox() {
    val indexWidth = index?.width ?: 0f
    val radicalWidth = 0.55f * em
    val padTop = 0.1f * em
    val padRight = 0.1f * em
    override val width = indexWidth + radicalWidth + content.width + padRight
    override val baseline = content.baseline + padTop
    override val height = content.height + padTop
}
