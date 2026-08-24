# 计划 5：state 状态机 + 显示层 + 自研排版器 + COMP 模式端到端界面

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把计算引擎变成可用 App：状态机（变量/Ans/设置/历史）+ 卡西欧显示规则格式化（Fix/Sci/Norm 10 位）+ 自研 LaTeX 排版器（Canvas 矢量绘制）+ 仿真键盘 + COMP 模式完整界面（实时自然显示输入、结果、历史回溯）+ 设置界面 + 双语 + DataStore 持久化。端到端打通一个模式；其余 11 模式界面在计划 6 实现。

**路线图调整（用户已知悉方向）**：设计文档原计划 5 为"UI 全量"，因 UI 工作量与自研排版器的加入，拆为两个计划：**计划 5（本文档）= UI 基础设施 + COMP 端到端**；计划 6 = 其余 11 模式界面（金融变量列表通用组件、数据编辑器、学习辅助长按公式）+ 收尾（README 截图、APK 签名）。

**Architecture:**
- `core/format`：显示格式化（纯 Kotlin，可测）
- `core/render`：自研排版器的布局模型（AST → MathBox 树，纯 Kotlin，注入文本测量接口，可测）
- `state`：计算器状态机（纯 Kotlin，可测）
- `data`：DataStore 持久化（Android 依赖，本计划唯一新依赖 androidx.datastore:datastore-preferences:1.1.7）
- `ui`：Compose 组件（MathView 绘制、键盘、显示屏、界面）——人工验证为主

**Tech Stack:** 既有（Kotlin 2.0.20 + AGP 8.5.2 + Compose BOM 2024.09.00）+ datastore-preferences 1.1.7（已验证 google() 仓库可用，直连无需代理）。语言切换用 `Locale.setDefault` + Activity 重建，**不引入 appcompat**。

**环境前提：** 每次新开 shell 先 `source .dev/env.sh`。**联网需求**：仅 Task 4（DataStore 依赖下载，约几百 KB；若 `./gradlew` 拉取失败，停下来通知用户挂代理节点）。

**权威依据：**
- 显示规则（说明书 CN-25/26，计划 2 提取 L568-610）：Fix 0~9 位小数、Sci 1~10 位有效数字、Norm1（10⁻²>|x| 或 |x|≥10¹⁰ 转指数）/Norm2（10⁻⁹>|x| 或 |x|≥10¹⁰）；四舍五入；默认 Norm1。10 位有效数字上限（CN-165）。
- Ans 更新时机（CN-42 L1186-1192）：按 EXE/=/M+/M-/STO 执行计算时更新；出错不改变。
- 操作符续算自动输入 Ans（CN-42 L1196-1208）：按 EXE 后直接按运算符，自动输入 Ans。
- 界面设计：设计文档 §6（竖屏上屏下键、深色液晶屏底色高分辨率渲染、SHIFT 第二功能、MODE 模式菜单、按键振动与高亮、方向键移动）。

**设计决策（说明书/设计文档未覆盖处）：**

1. **自研排版器直接消费 AST**（`Node` → MathBox 树 → Canvas），不经 LaTeX 字符串往返；`core/expr` 的 Latex.kt 保留（调试/日志/将来换库的备用通道）。排版规则与 Latex.kt 的约定对齐（分数、幂、根号、隐式乘并置、负式加括号等），并新增 `\log_{m}` 下标支持。
2. **排版器分层**：布局模型（MathBox 树 + 尺寸计算）在 `core/render`（纯 Kotlin，注入 `TextMeasure` 接口，单元测试用假测量器）；绘制在 `ui/math/MathView.kt`（Compose Canvas + TextMeasurer）。
3. **显示格式化**在 `core/format`（纯 Kotlin）。指数形态输出 `mantissaE指数`（如 `1.23456789E-12`，COMP 屏按卡西欧样式显示；自然排版美化留计划 6）。
4. **状态机**在 `state`（纯 Kotlin）：模式、变量（A~D/X/Y/M/Ans + 金融 VARS）、设置、历史。DataStore 只持久化设置与历史（变量不持久化——真机断电保留，但计划 5 从简，计划 6 再议）。
5. **COMP 编辑模型**：输入行为 String + 光标位置（插入/删除/左右移动）；EXE 求值；出错显示 CalcException.kind.display（Math ERROR 等）并允许 ◀▶ 返回编辑；历史记录（表达式+结果）双向回溯（▲▼）。
6. **键面缩写保持英文**（设计文档已定）；界面文字双语（默认中文，设置切换）。
7. **已知留白（计划 6）**：其余 11 模式界面、金融变量 VARS 与引擎的接线（state 已留 VARS 容器）、数据编辑器、学习辅助（长按公式）、M+/M-/STO/RCL 完整流程（计划 5 只提供 RCL 弹窗插入变量）、INS/DEL 数据编辑、定制快捷键、截图与 README 补全、APK 正式签名。
8. **Compose 代码的验收方式**：`core/*` 与 `state` 代码逐字照抄 + 单元测试全绿；`ui/*` 与 `data/*` 为参考实现——**若编译报错，实现子代理允许修正 Compose/AGP API 用法细节，但不许改变结构与行为**；验收以 `assembleDebug` 通过 + 行为走查为准。

**修订记录（执行期）：**

- 2026-08-24（Task 5 质量审查发现，FAIL 级已修复）：**CalcState 的 mode/shift/settings 是普通 Kotlin var，Compose 不观察**——SHIFT 指示符/键面不刷新、按键动作按旧组合态插入（错位一档）、SHIFT 粘滞不复位。已修复：①三者改 `mutableStateOf` 委托（compose runtime 为纯 JVM，不违反 state 的"无 android import"约束）；②`clearShift()` 加入 CalcState 并在 `CompController.insert` 末尾消费（真机：SHIFT 只作用于下一次按键）；③`isOperatorStart` 修正：去掉会产生非法 "Ans)" 的 `)`，补上 `² ³ ˣ√( nPr nCr`（EXE 后按 x² 应得 Ans²）。新增 CompControllerTest 锁定（6 测）。连带：可变属性委托生成 JVM setter 与原 `setMode` 方法签名冲突——`setMode` 改名 `switchMode`。非阻塞备注：SqrtBox 根号线宽为固定物理像素未随 em 缩放、底部顶点裁边约 1px（纯视觉）；输入中间态降级线性文本的闪烁（后续可"保留最后成功排版"优化）；横向滚动不自动跟光标（UX 注）。
- 2026-08-24（Task 4 双审查）：①质量审查发现 themes.xml 缺 `windowLightStatusBar=false`（Material.Light 默认深色状态栏图标，黑底下不可见）——已在计划与磁盘同步补一行。②其余备注（非阻塞未改码）：历史 load 未按 HISTORY_CAP 截断；历史序列化的 tab 注入理论边缘（当前键盘无法产生 \t，loader 有丢弃兜底）；periodsPerYear 无范围校验；Prefs 尚无调用点（计划 5 Task 5+ 接线时需注意 load/save 串行化）。规格审查 PASS（5 文件逐字一致）。
- 2026-08-24（Task 3 双审查发现，已修复）：①规格审查——实现把隐式乘的窄空格（U+2009）误写为普通空格（U+0020），已改回（单字符差异，逐字比对捕获）。②质量审查——`SupBox` 原契约 `baseline=base.baseline` 与 `height=base.height+max(0,−supTop)` 自相矛盾（sup 恒向上溢出盒界 0.21em，`2^3` 都触发；绘制者无法同时满足基线对齐与墨迹在界内）；已改为 lift 模型（`baseline=base.baseline+lift`、`height=base.height+lift`、新增 `baseTop`/`supTop` 非负偏移），MathView 的 SupBox 绘制分支同步更新，测试锁定新契约并补嵌套幂单级脚本用例（12 测）。其余备注（非阻塞）：嵌套上标不二次缩小为有意决策（真机单级脚本）；TextMeasure 的 0.8h 基线启发式由 UI 层包装时对齐真实基线；SubBox 仅向下扩展（仅 log 底数用，实际不触发）。
- 2026-08-24（Task 3 实现子代理回报+控制器排查）：①计划代码误用 `intersperse`——该函数**不在 Kotlin 标准库**（子代理已对缓存的 stdlib jar 实证 grep 为零）；已在 MathBuilder.kt 末尾补私有扩展实现。②MathBuilderTest 的 `build()` 助手误把单语句也走 Program 包装（返回 RowBox 而非表达式自身的盒子），导致 7 个结构断言失败；已改为单语句取 `statements[0]`。③`flatten` 曾不产出容器节点自身，致 `log with base has sub box` 的 SubBox 断言永假；已改为先含自身再递归。
- 2026-08-24（Task 2 质量审查发现）：`Settings` 缺 **Payment（期初/期末）** 与 **dn（CI/SI 奇数期利息）** 两项——它们是 CMPD 求解器的必填形参且属说明书 CN-19 设置屏，非计划留白。已补入 Settings（CalcState.kt）、Prefs 序列化（计划 5 Task 4 块）、CalcStateTest 回归（6 测）。其余备注（非阻塞）：`history` 公有 MutableList 的越界隐患（建议后续改只读视图）；Ans 双写无害重复。
- 2026-08-24（Task 1 实现子代理回报）：NumberFormatterTest 两处期望值错误——①`12345678901` 的 10 位舍入结果应为 `1.23456789E10` 而非 `1.234567891E10`，已改输入为 `12345678905.0`（保留进位路径覆盖）；②`100.0` 在 Norm2 下应为 `"100"` 而非 `"1E2"`（整百不转指数，符合真机）。已修正（计划+代码同步）。质量审查后补测 Norm 下界精确值与进位跨界两例（8 测）。

---

### Task 1: 显示格式化 NumberFormatter（core/format）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/format/NumberFormatter.kt`
- Create: `app/src/test/java/com/fincalc/app/core/format/NumberFormatterTest.kt`

- [ ] **Step 1: NumberFormatter.kt**

```kotlin
package com.fincalc.app.core.format

import com.fincalc.app.core.expr.DisplayMode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * 卡西欧显示规则（说明书 CN-25/26 + CN-165）：
 * 最多 10 位有效数字；Fix 四舍五入到指定小数位；Sci 指定有效数字位数（恒指数）；
 * Norm1：|x| < 10⁻² 或 |x| ≥ 10¹⁰ 转指数；Norm2：|x| < 10⁻⁹ 或 |x| ≥ 10¹⁰ 转指数。
 * 输出指数形态为 "mantissaE指数"（如 1.23456789E-12）。
 */
object NumberFormatter {

    fun format(value: Double, mode: DisplayMode): String {
        if (value.isNaN() || value.isInfinite()) return "Math ERROR"
        if (value == 0.0) return "0"
        return when (mode) {
            is DisplayMode.Fix -> fix(value, mode.digits)
            is DisplayMode.Sci -> sci(value, mode.digits)
            is DisplayMode.Norm1 -> norm(value, -2)
            is DisplayMode.Norm2 -> norm(value, -9)
        }
    }

    /** Fix：四舍五入到 digits 位小数。 */
    private fun fix(value: Double, digits: Int): String =
        BigDecimal.valueOf(value).setScale(digits, RoundingMode.HALF_UP).toPlainString()

    /** Sci：digits 位有效数字，恒指数。 */
    private fun sci(value: Double, digits: Int): String {
        val bd = BigDecimal.valueOf(value).round(MathContext(digits, RoundingMode.HALF_UP))
        return toScientific(bd)
    }

    /** Norm：10 位有效数字；幅度在 (10^lowExp, 10^10) 之外转指数；否则普通小数并去尾零。 */
    private fun norm(value: Double, lowExp: Int): String {
        val bd = BigDecimal.valueOf(value).round(MathContext(10, RoundingMode.HALF_UP))
        val exp = bd.precision() - bd.scale() - 1
        if (exp < lowExp || exp >= 10) return toScientific(bd)
        return bd.stripTrailingZeros().toPlainString()
    }

    /** BigDecimal → "mantissaEexp"（mantissa 去尾零）。 */
    private fun toScientific(bd: BigDecimal): String {
        val exp = bd.precision() - bd.scale() - 1
        val mantissa = bd.movePointLeft(exp).stripTrailingZeros().toPlainString()
        return "${mantissa}E$exp"
    }
}
```

- [ ] **Step 2: NumberFormatterTest.kt**

```kotlin
package com.fincalc.app.core.format

import com.fincalc.app.core.expr.DisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {
    @Test
    fun `norm1 examples from manual`() {
        // 说明书 CN-25/26：1÷6 = 0.1666666667（Norm1 10 位）
        assertEquals("0.1666666667", NumberFormatter.format(1.0 / 6, DisplayMode.Norm1))
        // 1÷200 = 5E-3（Norm1 转指数）；0.005（Norm2 不转）
        assertEquals("5E-3", NumberFormatter.format(1.0 / 200, DisplayMode.Norm1))
        assertEquals("0.005", NumberFormatter.format(1.0 / 200, DisplayMode.Norm2))
    }

    @Test
    fun `norm large magnitude switches at 1e10`() {
        assertEquals("1234567890", NumberFormatter.format(1234567890.0, DisplayMode.Norm1))
        // 12345678905 → 10 位有效数字进位（第 11 位为 5）→ 1.234567891E10
        assertEquals("1.234567891E10", NumberFormatter.format(12345678905.0, DisplayMode.Norm1))
    }

    @Test
    fun `norm strips trailing zeros`() {
        assertEquals("0.4", NumberFormatter.format(0.4, DisplayMode.Norm1))
        assertEquals("-3.75", NumberFormatter.format(-3.75, DisplayMode.Norm1))
        assertEquals("100", NumberFormatter.format(100.0, DisplayMode.Norm2))
    }

    @Test
    fun `fix mode`() {
        // 说明书 CN-25：100÷7 = 14.286（Fix3）、14.29（Fix2）
        assertEquals("14.286", NumberFormatter.format(100.0 / 7, DisplayMode.Fix(3)))
        assertEquals("14.29", NumberFormatter.format(100.0 / 7, DisplayMode.Fix(2)))
        assertEquals("-2.68", NumberFormatter.format(-2.675, DisplayMode.Fix(2)))
        assertEquals("5", NumberFormatter.format(4.5, DisplayMode.Fix(0)))
    }

    @Test
    fun `sci mode`() {
        // 说明书 CN-26：1÷7 = 1.4286E-1（Sci5）
        assertEquals("1.4286E-1", NumberFormatter.format(1.0 / 7, DisplayMode.Sci(5)))
        assertEquals("1.428571429E-1", NumberFormatter.format(1.0 / 7, DisplayMode.Sci(10)))
        assertEquals("-1.2E3", NumberFormatter.format(-1200.0, DisplayMode.Sci(2)))
    }

    @Test
    fun `edge cases`() {
        assertEquals("0", NumberFormatter.format(0.0, DisplayMode.Norm1))
        assertEquals("Math ERROR", NumberFormatter.format(Double.NaN, DisplayMode.Norm1))
        assertEquals("Math ERROR", NumberFormatter.format(Double.POSITIVE_INFINITY, DisplayMode.Norm1))
        assertEquals("Math ERROR", NumberFormatter.format(Double.NEGATIVE_INFINITY, DisplayMode.Norm1))
    }

    @Test
    fun `norm exact lower boundary`() {
        // 审查补测："严格小于"才转指数——0.01（Norm1）与 1e-9（Norm2）不转
        assertEquals("0.01", NumberFormatter.format(0.01, DisplayMode.Norm1))
        assertEquals("0.000000001", NumberFormatter.format(1e-9, DisplayMode.Norm2))
        assertEquals("9.9E-3", NumberFormatter.format(0.0099, DisplayMode.Norm1))
    }

    @Test
    fun `norm carry crosses upper boundary`() {
        // 审查补测：先 10 位舍入后判界——9999999999.9 进位跨界
        assertEquals("1E10", NumberFormatter.format(9999999999.9, DisplayMode.Norm1))
    }
}
```

- [ ] **Step 3: 跑测试并提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.format.NumberFormatterTest"
git add app/src/main/java/com/fincalc/app/core/format/ app/src/test/java/com/fincalc/app/core/format/
git commit -m "feat(core/format): 卡西欧显示规则格式化（Fix/Sci/Norm 10 位有效数字）"
```

预期：6 个测试全过。

---

### Task 2: state 状态机（纯 Kotlin）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/state/CalcState.kt`
- Create: `app/src/test/java/com/fincalc/app/state/CalcStateTest.kt`

- [ ] **Step 1: CalcState.kt**

```kotlin
package com.fincalc.app.state

import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.core.expr.EvalContext
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 12 个计算模式（设计文档 §5）。 */
enum class Mode { COMP, SMPL, CMPD, CASH, AMRT, CNVR, COST, DAYS, DEPR, BOND, BEVN, STAT }

/** 全部设置项（说明书 CN-19~26 配置设定）。 */
data class Settings(
    val angle: AngleUnit = AngleUnit.DEG,
    val display: DisplayMode = DisplayMode.Norm1,
    val payment: Cmpd.Payment = Cmpd.Payment.END,      // Payment：期初 Begin/期末 End（CMPD/AMRT）
    val dn: Cmpd.OddPeriod = Cmpd.OddPeriod.CI,        // dn：奇数期利息 CI 复利/SI 单利（CMPD）
    val days360: Boolean = false,                    // Date Mode：false=365（默认）/true=360
    val dateFormat: Days.DateFormat = Days.DateFormat.MDY,
    val bondTerm: Boolean = false,                   // Bond Date：false=Date/true=Term
    val periodsPerYear: Int = 1,                     // Periods/Y：1=Annual/2=Semi
    val prfRatio: Boolean = false,                   // PRF/Ratio：false=PRF/true=r%
    val bevenSales: Boolean = false,                 // B-Even：false=Quantity/true=Sales
    val statFreq: Boolean = false,                   // STAT：FREQ 栏显示
    val chinese: Boolean = true                      // 界面语言：默认中文
)

/** COMP 历史条目（表达式 + 结果）。 */
data class HistoryEntry(val input: String, val result: Double)

/**
 * 计算器状态机（纯 Kotlin）。持有模式、变量、设置、历史。
 * 变量：A~D、X、Y、M、Ans + 金融 VARS（n、I%、PV、PMT、FV、P/Y、C/Y、PM1、PM2、Dys……计划 6 接线）。
 */
class CalcState(
    settings: Settings = Settings()
) {
    /** Compose 可观察（mutableStateOf）：UI 直接订阅，改动即触发重组（Task 5 审查修复）。 */
    var settings by mutableStateOf(settings)

    var mode: Mode by mutableStateOf(Mode.COMP)
        private set

    var shift: Boolean by mutableStateOf(false)
        private set

    private val vars = mutableMapOf<String, Double>()
    val history = mutableListOf<HistoryEntry>()
    var historyCursor = -1
        private set

    fun switchMode(m: Mode) {
        mode = m
        shift = false
    }

    fun toggleShift() {
        shift = !shift
    }

    /** 真机行为：SHIFT 只作用于下一次按键，插入后自动解除。 */
    fun clearShift() {
        shift = false
    }

    fun getVar(name: String): Double = vars[name] ?: 0.0

    fun setVar(name: String, value: Double) {
        vars[name] = value
    }

    /** 执行表达式求值成功后的状态更新：Ans、历史、光标（说明书 CN-42）。 */
    fun onEvaluated(input: String, result: Double) {
        setVar("Ans", result)
        history += HistoryEntry(input, result)
        if (history.size > HISTORY_CAP) history.removeAt(0)
        historyCursor = -1
    }

    /** 历史回溯：先向旧（▲）再向新（▼）。返回条目或 null（无更多）。 */
    fun historyBack(): HistoryEntry? {
        if (history.isEmpty()) return null
        if (historyCursor == -1) historyCursor = history.size - 1
        else if (historyCursor > 0) historyCursor--
        return history[historyCursor]
    }

    fun historyForward(): HistoryEntry? {
        if (historyCursor == -1) return null
        if (historyCursor < history.size - 1) {
            historyCursor++
            return history[historyCursor]
        }
        historyCursor = -1
        return null
    }

    /** 表达式求值上下文（core/expr 的 EvalContext 适配：变量直通本状态机）。 */
    fun exprContext(): EvalContext = object : EvalContext {
        override val angle: AngleUnit get() = settings.angle
        override val display: DisplayMode get() = settings.display
        override fun getVar(name: String): Double = this@CalcState.getVar(name)
        override fun setVar(name: String, value: Double) = this@CalcState.setVar(name, value)
        override fun nextRandom(): Double = kotlin.random.Random.nextDouble()
    }

    companion object {
        const val HISTORY_CAP = 50
    }
}
```

- [ ] **Step 2: CalcStateTest.kt**

```kotlin
package com.fincalc.app.state

import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.finance.Cmpd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcStateTest {
    @Test
    fun `eval context wires vars and ans`() {
        val s = CalcState()
        ExprEngine.eval("3×4", s.exprContext())          // Ans=12
        assertEquals(12.0, s.getVar("Ans"), 0.0)
        ExprEngine.eval("Ans÷30", s.exprContext())       // 0.4
        assertEquals(0.4, s.getVar("Ans"), 1e-12)
        ExprEngine.eval("2A", s.exprContext())           // A 未赋值 → 0
        s.setVar("A", 5.0)
        assertEquals(10.0, ExprEngine.eval("2A", s.exprContext()), 1e-12)
        // Pol 写回 X/Y 直通 state
        ExprEngine.eval("Pol(√(2),√(2))", s.exprContext())
        assertEquals(2.0, s.getVar("X"), 1e-9)
        assertEquals(45.0, s.getVar("Y"), 1e-9)
    }

    @Test
    fun `settings flow through to context`() {
        val s = CalcState(Settings(angle = AngleUnit.RAD))
        assertEquals(-1.0, ExprEngine.eval("cos(π)", s.exprContext()), 1e-9)
        val s2 = CalcState(Settings(display = DisplayMode.Fix(3)))
        assertEquals(28.571, ExprEngine.eval("Rnd(200÷7)", s2.exprContext()), 1e-12)
    }

    @Test
    fun `on evaluated records history with cap`() {
        val s = CalcState()
        repeat(60) { s.onEvaluated("$it", it.toDouble()) }
        assertEquals(CalcState.HISTORY_CAP, s.history.size)
        assertEquals(59.0, s.history.last().result, 0.0)
    }

    @Test
    fun `history navigation`() {
        val s = CalcState()
        s.onEvaluated("1+1", 2.0)
        s.onEvaluated("2+2", 4.0)
        assertEquals(4.0, s.historyBack()!!.result, 0.0)   // 最新
        assertEquals(2.0, s.historyBack()!!.result, 0.0)   // 次新
        assertEquals(2.0, s.historyBack()!!.result, 0.0)   // 到头停住
        assertEquals(4.0, s.historyForward()!!.result, 0.0)
        assertNull(s.historyForward())                     // 越过最新回到 null
    }

    @Test
    fun `mode switch resets shift`() {
        val s = CalcState()
        s.toggleShift()
        s.setMode(Mode.CMPD)
        assertEquals(false, s.shift)
        assertEquals(Mode.CMPD, s.mode)
    }

    @Test
    fun `settings carry payment and dn for cmpd wiring`() {
        // 审查发现补测：Payment/dn 是 CMPD 求解器必填形参（计划 6 接线来源）
        val s = CalcState()
        assertEquals(Cmpd.Payment.END, s.settings.payment)
        assertEquals(Cmpd.OddPeriod.CI, s.settings.dn)
        val s2 = CalcState(Settings(payment = Cmpd.Payment.BEGIN, dn = Cmpd.OddPeriod.SI))
        assertEquals(Cmpd.Payment.BEGIN, s2.settings.payment)
        assertEquals(Cmpd.OddPeriod.SI, s2.settings.dn)
    }
}
```

- [ ] **Step 3: 跑测试并提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.state.CalcStateTest"
git add app/src/main/java/com/fincalc/app/state/ app/src/test/java/com/fincalc/app/state/
git commit -m "feat(state): 计算器状态机（变量/设置/历史/EvalContext 适配）"
```

预期：5 个测试全过。

---

### Task 3: 自研 LaTeX 排版器——布局模型（core/render）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/render/MathBox.kt`
- Create: `app/src/main/java/com/fincalc/app/core/render/MathBuilder.kt`
- Create: `app/src/test/java/com/fincalc/app/core/render/MathBuilderTest.kt`

- [ ] **Step 1: MathBox.kt（盒子模型：所有尺寸在构建期经 TextMeasure 确定）**

```kotlin
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
```

- [ ] **Step 2: MathBuilder.kt（AST → MathBox 树；排版约定与 core/expr 的 Latex.kt 对齐）**

```kotlin
package com.fincalc.app.core.render

import com.fincalc.app.core.expr.FuncName
import com.fincalc.app.core.expr.Node
import com.fincalc.app.core.expr.Program

/**
 * AST → MathBox 排版树。排版约定与 Latex.kt 一致（分数、幂、根号、隐式乘并置、负式加括号）。
 * 上标/下标用 SCRIPT 缩放。
 */
object MathBuilder {

    const val SCRIPT = 0.7f
    private const val BASELINE_FRAC = 0.8f

    fun build(program: Program, m: TextMeasure, em: Float): MathBox =
        RowBox(program.statements.map { build(it, m, em) }.intersperse(text(" : ", m, em)))

    fun build(node: Node, m: TextMeasure, em: Float, scale: Float = 1f): MathBox = when (node) {
        is Node.Num -> num(node, m, em, scale)
        Node.Pi -> text("π", m, em, scale)
        Node.EConst -> text("e", m, em, scale)
        is Node.Var -> text(node.name, m, em, scale)
        Node.Ran -> text("Ran#", m, em, scale)
        is Node.Add -> row(m, em, scale, build(node.l, m, em, scale), text(" + ", m, em, scale), build(node.r, m, em, scale))
        is Node.Sub -> row(m, em, scale, build(node.l, m, em, scale), text(" − ", m, em, scale), factor(node.r, m, em, scale))
        is Node.Mul -> row(m, em, scale, factor(node.l, m, em, scale), text(" × ", m, em, scale), factor(node.r, m, em, scale))
        is Node.Div -> FracBox(build(node.l, m, em, scale), build(node.r, m, em, scale), em)
        is Node.ImplicitMul -> row(m, em, scale, factor(node.l, m, em, scale), text(" ", m, em, scale), factor(node.r, m, em, scale))
        is Node.Neg -> row(m, em, scale, text("−", m, em, scale), factor(node.e, m, em, scale))
        is Node.Pow -> SupBox(base(node.base, m, em, scale), build(node.exp, m, em, SCRIPT), em)
        is Node.XRoot -> SqrtBox(build(node.radicand, m, em, scale), build(node.degree, m, em, SCRIPT), em)
        is Node.Sqrt -> SqrtBox(build(node.e, m, em, scale), null, em)
        is Node.Cbrt -> SqrtBox(build(node.e, m, em, scale), text("3", m, em, SCRIPT), em)
        is Node.Fact -> row(m, em, scale, factor(node.e, m, em, scale), text("!", m, em, scale))
        is Node.Percent -> row(m, em, scale, factor(node.e, m, em, scale), text("%", m, em, scale))
        is Node.Perm -> row(m, em, scale, factor(node.n, m, em, scale), text("P", m, em, scale), factor(node.r, m, em, scale))
        is Node.Comb -> row(m, em, scale, factor(node.n, m, em, scale), text("C", m, em, scale), factor(node.r, m, em, scale))
        is Node.Func -> func(node, m, em, scale)
    }

    /** 指数计数法自然显示：1.2E3 → 1.2 × 10^3。 */
    private fun num(n: Node.Num, m: TextMeasure, em: Float, scale: Float): MathBox {
        val e = n.raw.indexOf('E')
        return if (e > 0) {
            row(
                m, em, scale,
                text(n.raw.substring(0, e), m, em, scale),
                text(" × ", m, em, scale),
                SupBox(text("10", m, em, scale), text(n.raw.substring(e + 1), m, em, SCRIPT), em)
            )
        } else {
            text(n.raw, m, em, scale)
        }
    }

    /** 因子位置（乘/隐式乘/阶乘/%/负号/减法右侧）：加减式与负式加括号。 */
    private fun factor(n: Node, m: TextMeasure, em: Float, scale: Float): MathBox = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg -> paren(n, m, em, scale)
        else -> build(n, m, em, scale)
    }

    /** 幂底数：加减/负/乘/隐式乘需加括号。 */
    private fun base(n: Node, m: TextMeasure, em: Float, scale: Float): MathBox = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg, is Node.Mul, is Node.ImplicitMul -> paren(n, m, em, scale)
        else -> build(n, m, em, scale)
    }

    private fun paren(n: Node, m: TextMeasure, em: Float, scale: Float): MathBox =
        row(m, em, scale, text("(", m, em, scale), build(n, m, em, scale), text(")", m, em, scale))

    private val FUNC_LABELS = mapOf(
        FuncName.SIN to "sin", FuncName.COS to "cos", FuncName.TAN to "tan",
        FuncName.ASIN to "sin⁻¹", FuncName.ACOS to "cos⁻¹", FuncName.ATAN to "tan⁻¹",
        FuncName.SINH to "sinh", FuncName.COSH to "cosh", FuncName.TANH to "tanh",
        FuncName.ASINH to "sinh⁻¹", FuncName.ACOSH to "cosh⁻¹", FuncName.ATANH to "tanh⁻¹",
        FuncName.LN to "ln", FuncName.RND to "Rnd", FuncName.POL to "Pol", FuncName.REC to "Rec"
    )

    private fun func(node: Node.Func, m: TextMeasure, em: Float, scale: Float): MathBox = when (node.fn) {
        FuncName.LOG ->
            if (node.args.size == 2) {
                row(
                    m, em, scale,
                    SubBox(text("log", m, em, scale), build(node.args[0], m, em, SCRIPT), em),
                    text("(", m, em, scale),
                    build(node.args[1], m, em, scale),
                    text(")", m, em, scale)
                )
            } else {
                plainFunc("log", node.args, m, em, scale)
            }
        FuncName.ABS -> row(m, em, scale, text("|", m, em, scale), build(node.args[0], m, em, scale), text("|", m, em, scale))
        else -> plainFunc(FUNC_LABELS.getValue(node.fn), node.args, m, em, scale)
    }

    private fun plainFunc(label: String, args: List<Node>, m: TextMeasure, em: Float, scale: Float): MathBox =
        row(
            m, em, scale,
            listOf(text(label, m, em, scale), text("(", m, em, scale)) +
                args.map { build(it, m, em, scale) }.intersperse(text(", ", m, em, scale)) +
                listOf(text(")", m, em, scale))
        )

    internal fun text(s: String, m: TextMeasure, em: Float, scale: Float = 1f): TextBox {
        val (w, h) = m.measure(s, scale)
        return TextBox(s, scale, w, h, h * BASELINE_FRAC)
    }

    private fun row(m: TextMeasure, em: Float, scale: Float, vararg boxes: MathBox): RowBox =
        RowBox(boxes.toList())

    private fun row(m: TextMeasure, em: Float, scale: Float, boxes: List<MathBox>): RowBox =
        RowBox(boxes)

    /** 在元素间插入分隔符（Kotlin 标准库无此函数，自行实现）。 */
    private fun <T> List<T>.intersperse(sep: T): List<T> =
        flatMapIndexed { i, item -> if (i == 0) listOf(item) else listOf(sep, item) }
}
```

- [ ] **Step 3: MathBuilderTest.kt（假测量器：宽=字符数×10×scale、高=20×scale、基线=16×scale）**

```kotlin
package com.fincalc.app.core.render

import com.fincalc.app.core.expr.ExprEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathBuilderTest {
    private val fake = TextMeasure { t, s -> (t.length * 10f * s) to (20f * s) }
    private val em = 20f

    private fun build(input: String): MathBox {
        val program = ExprEngine.parse(input)
        // 单语句直接取该表达式的盒子；多语句走 Program 拼接（" : " 分隔）
        return if (program.statements.size == 1) {
            MathBuilder.build(program.statements[0], fake, em)
        } else {
            MathBuilder.build(program, fake, em)
        }
    }

    @Test
    fun `plain number is text box`() {
        val b = build("123")
        assertTrue(b is TextBox)
        assertEquals(30f, b.width, 1e-4f)
        assertEquals(20f, b.height, 1e-4f)
        assertEquals(16f, b.baseline, 1e-4f)
    }

    @Test
    fun `division becomes frac box with centered geometry`() {
        val b = build("1÷2")
        assertTrue(b is FracBox)
        val f = b as FracBox
        // num/den 各 1 字符宽 10；宽 = max + 2×pad(0.2em=4)
        assertEquals(10f + 8f, f.width, 1e-4f)
        // lineY = num.height(20) + gap(3) + line/2(0.6)
        assertEquals(23.6f, f.lineY, 1e-4f)
        assertEquals(23.6f, f.baseline, 1e-4f)
        // 高 = 20 + 2×3 + 1.2 + 20
        assertEquals(47.2f, f.height, 1e-4f)
    }

    @Test
    fun `implicit mul renders juxtaposed after fraction`() {
        // 权威对照（同 Latex 约定）：1÷2π → \frac{1}{2} π
        val b = build("1÷2π")
        assertTrue(b is RowBox)
        val children = (b as RowBox).children
        assertEquals(3, children.size)
        assertTrue(children[0] is FracBox)
        assertTrue(children[2] is TextBox && (children[2] as TextBox).text == "π")
    }

    @Test
    fun `sup lifts content to keep ink inside box`() {
        val b = build("2^3")
        assertTrue(b is SupBox)
        val s = b as SupBox
        // 底 2：宽 10 基线 16；上标 3：scale 0.7 宽 7 高 14 基线 11.2
        assertEquals(17f, s.width, 1e-4f)
        // lift = max(0, 11.2 + 9 − 16) = 4.2 → 基线 20.2、高 24.2、sup 顶恰为 0
        assertEquals(20.2f, s.baseline, 1e-3f)
        assertEquals(24.2f, s.height, 1e-3f)
        assertEquals(0f, s.supTop, 1e-3f)
        assertEquals(4.2f, s.baseTop, 1e-3f)
    }

    @Test
    fun `nested power uses single level script scale`() {
        // 嵌套上标不二次缩小（有意决策：卡西欧真机只有一级脚本，与 TeX 惯例不同）
        val b = build("2^3^2")
        val outer = b as SupBox
        val inner = outer.sup as SupBox
        assertEquals(0.7f, (inner.sup as TextBox).scale, 1e-6f)
    }

    @Test
    fun `sqrt without and with index`() {
        val plain = build("√(2)")
        assertTrue(plain is SqrtBox)
        assertEquals(0.55f * em + 10f + 0.1f * em, plain.width, 1e-4f)

        val indexed = build("∛(5)")
        assertTrue(indexed is SqrtBox)
        val s = indexed as SqrtBox
        // index "3" 宽 7（scale 0.7）
        assertEquals(7f + 0.55f * em + 10f + 0.1f * em, s.width, 1e-4f)
    }

    @Test
    fun `factor parenthesizes add and neg`() {
        // 2×(3+4)：右操作数为加式 → 带括号
        val b = build("2×(3+4)")
        assertTrue(b is RowBox)
        val texts = collectTexts(b)
        assertEquals(listOf("2", " × ", "(", "3", " + ", "4", ")"), texts)
        // −3!：Neg 的因子位置
        val n = build("-3!")
        assertEquals(listOf("−", "3", "!"), collectTexts(n))
    }

    @Test
    fun `scientific literal natural display`() {
        val b = build("1.2E3")
        val texts = collectTexts(b)
        assertEquals(listOf("1.2", " × ", "10", "3"), texts)
        assertTrue(b is RowBox && (b as RowBox).children.last() is SupBox)
    }

    @Test
    fun `log with base has sub box`() {
        val b = build("log(2,16)")
        val boxes = flatten(b)
        assertTrue(boxes.any { it is SubBox })
        assertEquals(listOf("log", "2", "(", "16", ")"), collectTexts(b))
    }

    @Test
    fun `function and constants`() {
        assertEquals(listOf("sin", "(", "30", ")"), collectTexts(build("sin(30)")))
        assertEquals(listOf("sin⁻¹", "(", "0.5", ")"), collectTexts(build("asin(0.5)")))
        assertEquals(listOf("|", "2", " − ", "7", "|"), collectTexts(build("Abs(2-7)")))
        assertEquals(listOf("10", "P", "4"), collectTexts(build("10 nPr 4")))
        assertEquals(listOf("Ran#"), collectTexts(build("Ran#")))
    }

    @Test
    fun `multi statement joined with colon`() {
        val b = build("3+3:3×3")
        assertEquals(listOf("3", " + ", "3", " : ", "3", " × ", "3"), collectTexts(b))
    }

    @Test
    fun `row geometry aggregates`() {
        val b = build("2+3") as RowBox
        // 宽 = 10("2") + 30(" + " 三字符) + 10("3") = 50；高 = 16 + 4 = 20；基线 = 16
        assertEquals(50f, b.width, 1e-4f)
        assertEquals(20f, b.height, 1e-4f)
        assertEquals(16f, b.baseline, 1e-4f)
    }

    private fun collectTexts(b: MathBox): List<String> = flatten(b).filterIsInstance<TextBox>().map { it.text }

    private fun flatten(b: MathBox): List<MathBox> = when (b) {
        is RowBox -> listOf(b) + b.children.flatMap { flatten(it) }
        is FracBox -> listOf(b) + flatten(b.num) + flatten(b.den)
        is SupBox -> listOf(b) + flatten(b.base) + flatten(b.sup)
        is SubBox -> listOf(b) + flatten(b.base) + flatten(b.sub)
        is SqrtBox -> listOf(b) + (b.index?.let { flatten(it) } ?: emptyList()) + flatten(b.content)
        else -> listOf(b)
    }
}
```

- [ ] **Step 4: 跑测试并提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.render.MathBuilderTest"
git add app/src/main/java/com/fincalc/app/core/render/ app/src/test/java/com/fincalc/app/core/render/
git commit -m "feat(core/render): 自研 LaTeX 排版器布局模型（AST → MathBox 树）"
```

预期：11 个测试全过。

---

### Task 4: DataStore 持久化 + 液晶屏主题

**Files:**
- Modify: `app/build.gradle.kts`（dependencies 末尾加一行 datastore）
- Create: `app/src/main/java/com/fincalc/app/data/Prefs.kt`
- Create: `app/src/main/res/values/themes.xml`（替换为深色液晶屏配色）
- Modify: `app/src/main/res/values/strings.xml`、`app/src/main/res/values-zh-rCN/strings.xml`（补充 UI 词条）

**联网提醒**：本任务首次同步会下载 datastore-preferences 1.1.7（约几百 KB）。若 `./gradlew` 拉取依赖失败，停下来通知用户挂代理节点，不要改版本号硬扛。

- [ ] **Step 1: app/build.gradle.kts 的 dependencies 块末尾追加一行**

```kotlin
    implementation("androidx.datastore:datastore-preferences:1.1.7")
```

- [ ] **Step 2: Prefs.kt**

```kotlin
package com.fincalc.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.HistoryEntry
import com.fincalc.app.state.Settings
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "fincalc")

/** DataStore 持久化：设置 + COMP 历史（变量不持久化，计划 6 再议）。 */
object Prefs {

    private val KEY_ANGLE = stringPreferencesKey("angle")
    private val KEY_DISPLAY = stringPreferencesKey("display")
    private val KEY_PAYMENT = stringPreferencesKey("payment")
    private val KEY_DN = stringPreferencesKey("dn")
    private val KEY_DAYS360 = booleanPreferencesKey("days360")
    private val KEY_DATE_FORMAT = stringPreferencesKey("dateFormat")
    private val KEY_BOND_TERM = booleanPreferencesKey("bondTerm")
    private val KEY_PPY = intPreferencesKey("periodsPerYear")
    private val KEY_PRF_RATIO = booleanPreferencesKey("prfRatio")
    private val KEY_BEVEN_SALES = booleanPreferencesKey("bevenSales")
    private val KEY_STAT_FREQ = booleanPreferencesKey("statFreq")
    private val KEY_CHINESE = booleanPreferencesKey("chinese")
    private val KEY_HISTORY = stringPreferencesKey("history")

    suspend fun load(context: Context, state: CalcState) {
        val p = context.dataStore.data.first()
        val settings = Settings(
            angle = p[KEY_ANGLE]?.let { runCatching { AngleUnit.valueOf(it) }.getOrNull() } ?: AngleUnit.DEG,
            display = parseDisplay(p[KEY_DISPLAY]),
            payment = p[KEY_PAYMENT]?.let { runCatching { Cmpd.Payment.valueOf(it) }.getOrNull() }
                ?: Cmpd.Payment.END,
            dn = p[KEY_DN]?.let { runCatching { Cmpd.OddPeriod.valueOf(it) }.getOrNull() } ?: Cmpd.OddPeriod.CI,
            days360 = p[KEY_DAYS360] ?: false,
            dateFormat = p[KEY_DATE_FORMAT]?.let { runCatching { Days.DateFormat.valueOf(it) }.getOrNull() }
                ?: Days.DateFormat.MDY,
            bondTerm = p[KEY_BOND_TERM] ?: false,
            periodsPerYear = p[KEY_PPY] ?: 1,
            prfRatio = p[KEY_PRF_RATIO] ?: false,
            bevenSales = p[KEY_BEVEN_SALES] ?: false,
            statFreq = p[KEY_STAT_FREQ] ?: false,
            chinese = p[KEY_CHINESE] ?: true
        )
        state.settings = settings
        val hist = p[KEY_HISTORY].orEmpty()
        if (hist.isNotEmpty()) {
            state.history.clear()
            state.history += hist.split("\n").mapNotNull { line ->
                val i = line.indexOf('\t')
                if (i <= 0) null else HistoryEntry(line.substring(0, i), line.substring(i + 1).toDoubleOrNull() ?: return@mapNotNull null)
            }
        }
    }

    suspend fun save(context: Context, state: CalcState) {
        context.dataStore.edit { p ->
            p[KEY_ANGLE] = state.settings.angle.name
            p[KEY_DISPLAY] = formatDisplay(state.settings.display)
            p[KEY_PAYMENT] = state.settings.payment.name
            p[KEY_DN] = state.settings.dn.name
            p[KEY_DAYS360] = state.settings.days360
            p[KEY_DATE_FORMAT] = state.settings.dateFormat.name
            p[KEY_BOND_TERM] = state.settings.bondTerm
            p[KEY_PPY] = state.settings.periodsPerYear
            p[KEY_PRF_RATIO] = state.settings.prfRatio
            p[KEY_BEVEN_SALES] = state.settings.bevenSales
            p[KEY_STAT_FREQ] = state.settings.statFreq
            p[KEY_CHINESE] = state.settings.chinese
            p[KEY_HISTORY] = state.history.joinToString("\n") { "${it.input}\t${it.result}" }
        }
    }

    private fun parseDisplay(s: String?): DisplayMode = when {
        s == null -> DisplayMode.Norm1
        s == "Norm1" -> DisplayMode.Norm1
        s == "Norm2" -> DisplayMode.Norm2
        s.startsWith("Fix:") -> s.removePrefix("Fix:").toIntOrNull()?.let { DisplayMode.Fix(it) } ?: DisplayMode.Norm1
        s.startsWith("Sci:") -> s.removePrefix("Sci:").toIntOrNull()?.let { DisplayMode.Sci(it) } ?: DisplayMode.Norm1
        else -> DisplayMode.Norm1
    }

    private fun formatDisplay(d: DisplayMode): String = when (d) {
        DisplayMode.Norm1 -> "Norm1"
        DisplayMode.Norm2 -> "Norm2"
        is DisplayMode.Fix -> "Fix:${d.digits}"
        is DisplayMode.Sci -> "Sci:${d.digits}"
    }
}
```

注意：`Settings` 目前是不可变 data class（val），而 `Prefs.load` 需要整体替换——因此 **Task 2 的 CalcState.kt 需要一个小改动**：`var settings: Settings`（var 即可，已如此声明）✓，无需其他改动。

- [ ] **Step 3: themes.xml（替换为深色液晶屏配色）**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FinCalc" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/black</item>
        <!-- 审查发现：Material.Light 默认浅色状态栏图标，黑底下不可见，改深色底配浅色图标 -->
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

- [ ] **Step 4: 双语 strings.xml 补充词条**

`app/src/main/res/values/strings.xml`（英文回退）替换为：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FinCalc</string>
    <string name="placeholder_title">FinCalc — FC-200V style financial calculator (under construction)</string>
    <string name="settings">Settings</string>
    <string name="mode_select">Mode</string>
    <string name="mode_coming_soon">This mode is coming in the next update</string>
    <string name="angle_unit">Angle unit</string>
    <string name="display_mode">Number format</string>
    <string name="language">Language</string>
    <string name="date_mode">Date Mode (days in year)</string>
    <string name="date_input">Date Input format</string>
    <string name="history_empty">No history</string>
</resources>
```

`app/src/main/res/values-zh-rCN/strings.xml` 替换为：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FinCalc</string>
    <string name="placeholder_title">FinCalc —— 对标 FC-200V 的金融计算器（建设中）</string>
    <string name="settings">设置</string>
    <string name="mode_select">模式选择</string>
    <string name="mode_coming_soon">该模式将在下个版本实现</string>
    <string name="angle_unit">角度单位</string>
    <string name="display_mode">数值显示</string>
    <string name="language">界面语言</string>
    <string name="date_mode">一年中的天数（Date Mode）</string>
    <string name="date_input">日期输入格式</string>
    <string name="history_empty">暂无历史</string>
</resources>
```

- [ ] **Step 5: 构建验证并提交**

```bash
source .dev/env.sh
./gradlew assembleDebug
git add app/build.gradle.kts app/src/main/java/com/fincalc/app/data/ app/src/main/res/
git commit -m "feat(data): DataStore 持久化（设置+历史）与深色液晶屏主题、双语词条"
```

预期：`BUILD SUCCESSFUL`（依赖下载完成），APK 产出正常。

---

### Task 5: UI——MathView 绘制 + 键盘 + COMP 界面 + MainActivity 集成

**Files:**
- Create: `app/src/main/java/com/fincalc/app/ui/math/MathView.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/comp/CompController.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/comp/CompScreen.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/keyboard/Keyboard.kt`
- Create: `app/src/test/java/com/fincalc/app/ui/comp/CompControllerTest.kt`（审查修复后新增）
- Modify: `app/src/main/java/com/fincalc/app/MainActivity.kt`（整体替换为应用入口）

**说明（实现子代理）**：本任务为参考实现（计划设计决策 8）。结构、类名、行为必须与计划一致；Compose API 细节若编译报错，允许修正用法（如签名/参数名），不许改变结构与行为。完成标准：`assembleDebug` 通过 + 行为走查清单逐项可验。

- [ ] **Step 1: MathView.kt（AST → MathBox → Canvas 矢量绘制）**

```kotlin
package com.fincalc.app.ui.math

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.expr.Program
import com.fincalc.app.core.render.FracBox
import com.fincalc.app.core.render.MathBox
import com.fincalc.app.core.render.MathBuilder
import com.fincalc.app.core.render.RowBox
import com.fincalc.app.core.render.SqrtBox
import com.fincalc.app.core.render.SubBox
import com.fincalc.app.core.render.SupBox
import com.fincalc.app.core.render.TextBox
import com.fincalc.app.core.render.TextMeasure

/** 数学公式视图：自研排版器矢量绘制（深色液晶屏上的亮色文字）。 */
@Composable
fun MathView(
    program: Program,
    modifier: Modifier = Modifier,
    baseTextSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = Color(0xFFE8F5E9)
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val em = with(density) { baseTextSize.toPx() }
    val box = remember(program, baseTextSize) {
        MathBuilder.build(program, TextMeasure { text, scale ->
            val layout = textMeasurer.measure(
                text, TextStyle(fontSize = baseTextSize * scale, fontFamily = FontFamily.Serif)
            )
            layout.size.width.toFloat() to layout.size.height.toFloat()
        }, em)
    }
    val w = with(density) { box.width.toDp() }
    val h = with(density) { box.height.toDp() }
    Canvas(modifier.width(w).height(h)) {
        drawBox(box, 0f, 0f, textMeasurer, baseTextSize, color)
    }
}

/** 便捷重载：直接给表达式字符串。解析失败时降级为线性文本（设计文档风险表）。 */
@Composable
fun MathView(
    input: String,
    modifier: Modifier = Modifier,
    baseTextSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = Color(0xFFE8F5E9)
) {
    val program = remember(input) {
        try {
            ExprEngine.parse(input)
        } catch (e: Exception) {
            null
        }
    }
    if (program != null) {
        MathView(program, modifier, baseTextSize, color)
    } else {
        androidx.compose.material3.Text(input, color = color, fontSize = baseTextSize, modifier = modifier)
    }
}

/** 递归绘制。[x]/[yTop] 为盒子左上角坐标。 */
private fun DrawScope.drawBox(box: MathBox, x: Float, yTop: Float, m: TextMeasurer, baseTextSize: androidx.compose.ui.unit.TextUnit, color: Color) {
    when (box) {
        is TextBox -> drawText(
            m, box.text, Offset(x, yTop),
            TextStyle(color = color, fontSize = baseTextSize * box.scale, fontFamily = FontFamily.Serif)
        )
        is RowBox -> {
            var cx = x
            for (child in box.children) {
                drawBox(child, cx, yTop + box.baseline - child.baseline, m, baseTextSize, color)
                cx += child.width
            }
        }
        is FracBox -> {
            drawBox(box.num, x + (box.width - box.num.width) / 2, yTop, m, baseTextSize, color)
            drawLine(
                color,
                Offset(x, yTop + box.lineY),
                Offset(x + box.width, yTop + box.lineY),
                strokeWidth = box.lineThickness
            )
            drawBox(box.den, x + (box.width - box.den.width) / 2, yTop + box.denTop, m, baseTextSize, color)
        }
        is SupBox -> {
            drawBox(box.base, x, yTop + box.baseTop, m, baseTextSize, color)
            drawBox(box.sup, x + box.base.width, yTop + box.supTop, m, baseTextSize, color)
        }
        is SubBox -> {
            drawBox(box.base, x, yTop, m, baseTextSize, color)
            drawBox(box.sub, x + box.base.width, yTop + box.baseline + box.shiftDown - box.sub.baseline, m, baseTextSize, color)
        }
        is SqrtBox -> {
            val rx = x + box.indexWidth
            val top = yTop
            val bottom = yTop + box.height
            val midX = rx + box.radicalWidth * 0.35f
            // 根号三段折线 + 顶横线
            drawLine(color, Offset(rx, bottom - box.height * 0.3f), Offset(midX, bottom), strokeWidth = 2f)
            drawLine(color, Offset(midX, bottom), Offset(rx + box.radicalWidth, top), strokeWidth = 2f)
            drawLine(
                color,
                Offset(rx + box.radicalWidth, top + 1f),
                Offset(rx + box.radicalWidth + box.content.width + box.padRight, top + 1f),
                strokeWidth = 2f
            )
            box.index?.let { drawBox(it, x, top, m, baseTextSize, color) }
            drawBox(box.content, rx + box.radicalWidth, yTop + box.padTop, m, baseTextSize, color)
        }
    }
}
```

- [ ] **Step 2: CompController.kt（COMP 界面状态与按键逻辑；compose runtime 的 mutableStateOf 可在 JVM 用）**

```kotlin
package com.fincalc.app.ui.comp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/** COMP 模式控制器：输入行（String + 光标）、求值、错误、历史回溯、Ans 续算。 */
class CompController(val state: CalcState) {

    var input by mutableStateOf("")
        private set
    var cursor by mutableStateOf(0)
        private set
    var resultText by mutableStateOf<String?>(null)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    private var justEvaluated = false

    /** 插入文本（函数名等调用方自带括号）。EXE 后按运算符自动续 Ans（说明书 CN-42）。 */
    fun insert(text: String) {
        if (justEvaluated) {
            if (text.isOperatorStart()) {
                input = "Ans"
                cursor = 3
            } else {
                input = ""
                cursor = 0
            }
            justEvaluated = false
            resultText = null
        }
        errorText = null
        input = input.substring(0, cursor) + text + input.substring(cursor)
        cursor += text.length
        state.clearShift()   // SHIFT 只作用于下一次按键（真机行为）
    }

    fun delete() {
        if (cursor > 0) {
            input = input.substring(0, cursor - 1) + input.substring(cursor)
            cursor--
        }
    }

    fun clear() {
        input = ""
        cursor = 0
        resultText = null
        errorText = null
        justEvaluated = false
    }

    fun moveLeft() {
        if (cursor > 0) cursor--
    }

    fun moveRight() {
        if (cursor < input.length) cursor++
    }

    fun execute() {
        if (input.isBlank()) return
        try {
            val r = ExprEngine.eval(input, state.exprContext())
            state.onEvaluated(input, r)
            resultText = NumberFormatter.format(r, state.settings.display)
            errorText = null
            justEvaluated = true
        } catch (e: CalcException) {
            errorText = e.kind.display
            resultText = null
            justEvaluated = false
        }
    }

    /** 历史回溯：把条目载入输入行。 */
    fun historyBack() {
        state.historyBack()?.let {
            input = it.input
            cursor = it.input.length
            resultText = NumberFormatter.format(it.result, state.settings.display)
            errorText = null
            justEvaluated = false
        }
    }

    fun historyForward() {
        val entry = state.historyForward()
        if (entry != null) {
            input = entry.input
            cursor = entry.input.length
            resultText = NumberFormatter.format(entry.result, state.settings.display)
        } else {
            clear()
        }
        errorText = null
        justEvaluated = false
    }

    private fun String.isOperatorStart(): Boolean =
        first() in "+-×÷^!%²³" || this == "ˣ√(" || this == " nPr " || this == " nCr "
}
```

- [ ] **Step 2b: CompControllerTest.kt（审查修复后新增，锁定 Ans 续算与 SHIFT 消费行为）**

```kotlin
package com.fincalc.app.ui.comp

import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** COMP 控制器行为测试（Task 5 质量审查发现的缺陷回归锁定）。 */
class CompControllerTest {

    @Test
    fun `operator after exe continues with Ans`() {
        val c = CompController(CalcState())
        c.insert("2"); c.insert("+"); c.insert("3"); c.execute()   // 5
        c.insert("×"); c.insert("2")                               // Ans×2
        c.execute()
        assertEquals("10", c.resultText)
    }

    @Test
    fun `digit after exe starts fresh`() {
        val c = CompController(CalcState())
        c.insert("2"); c.execute()
        c.insert("5")
        assertEquals("5", c.input)
    }

    @Test
    fun `postfix after exe continues with Ans`() {
        // 审查发现：EXE 后按 x² 应得 Ans² 而非孤立 ²
        val c = CompController(CalcState())
        c.insert("3"); c.execute()   // 3
        c.insert("²")                // Ans²
        c.execute()
        assertEquals("9", c.resultText)
    }

    @Test
    fun `infix function after exe continues with Ans`() {
        val c = CompController(CalcState())
        c.insert("10"); c.execute()      // 10
        c.insert(" nCr "); c.insert("2") // Ans nCr 2
        c.execute()
        assertEquals("45", c.resultText)
    }

    @Test
    fun `shift is consumed after insert`() {
        val s = CalcState()
        val c = CompController(s)
        s.toggleShift()
        c.insert("³")
        assertFalse(s.shift)
    }

    @Test
    fun `error keeps input editable`() {
        val c = CompController(CalcState())
        c.insert("1"); c.insert("÷"); c.insert("0"); c.execute()
        assertEquals("Math ERROR", c.errorText)
        c.delete(); c.insert("2"); c.execute()
        assertEquals("0.5", c.resultText)
    }
}
```

- [ ] **Step 3: Keyboard.kt（键面网格组件）**

```kotlin
package com.fincalc.app.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 键定义：label 主功能；shiftLabel 第二功能（SHIFT 态显示并触发）。 */
data class Key(
    val label: String,
    val shiftLabel: String? = null,
    val onPress: () -> Unit,
    val onShiftPress: (() -> Unit)? = null
)

/** 卡西欧风格键面网格（6 列）。按键高亮反馈由 Button 自带；振动由调用层统一处理。 */
@Composable
fun Keypad(
    rows: List<List<Key>>,
    shift: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (key in row) {
                    val active = shift && key.shiftLabel != null
                    Button(
                        onClick = { if (active) (key.onShiftPress ?: key.onPress)() else key.onPress() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) Color(0xFF39493B) else Color(0xFF232B25)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (active) key.shiftLabel!! else key.label,
                            fontSize = 13.sp,
                            color = Color(0xFFE8F5E9),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: CompScreen.kt（COMP 界面：显示屏 + 键盘）**

```kotlin
package com.fincalc.app.ui.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.keyboard.Key
import com.fincalc.app.ui.keyboard.Keypad
import com.fincalc.app.ui.math.MathView

/** COMP 模式界面。上屏（输入实时排版 + 结果）下键（仿真键盘）。 */
@Composable
fun CompScreen(controller: CompController, onOpenModes: () -> Unit, onOpenSettings: () -> Unit) {
    val state = controller.state
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        // 显示屏（深色液晶屏底色）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1B2A1E))
                .padding(12.dp)
        ) {
            // 状态行（模式/角度/SHIFT 指示符）
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildString {
                        append(state.mode.name)
                        append("  ")
                        append(state.settings.angle.name)
                        if (state.shift) append("  SHIFT")
                    },
                    color = Color(0xFF9DBA9F),
                    fontSize = 12.sp
                )
            }
            // 输入行（实时 LaTeX 排版，横向可滚动）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (controller.input.isEmpty()) {
                    Text("0", color = Color(0xFFE8F5E9), fontSize = 22.sp, fontFamily = FontFamily.Serif)
                } else {
                    MathView(controller.input, baseTextSize = 22.sp)
                }
            }
            // 结果/错误行（右对齐）
            Row(modifier = Modifier.fillMaxWidth()) {
                val error = controller.errorText
                val result = controller.resultText
                when {
                    error != null -> Text(error, color = Color(0xFFFFB4A2), fontSize = 20.sp)
                    result != null -> Text(
                        result,
                        color = Color(0xFFE8F5E9),
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    else -> Text("", fontSize = 26.sp)
                }
            }
        }
        // 键盘区
        Keypad(rows = compKeys(controller, onOpenModes, onOpenSettings), shift = state.shift)
    }
}

/** COMP 键面（SHIFT 层为第二功能）。 */
private fun compKeys(c: CompController, onOpenModes: () -> Unit, onOpenSettings: () -> Unit): List<List<Key>> {
    val s = c.state
    fun ins(text: String): Key = Key(text, onPress = { c.insert(text) })
    fun insShift(label: String, shiftLabel: String, text: String, shiftText: String): Key =
        Key(label, shiftLabel, onPress = { c.insert(text) }, onShiftPress = { c.insert(shiftText) })

    return listOf(
        listOf(
            Key("SHIFT", onPress = { s.toggleShift() }),
            Key("MODE", onPress = onOpenModes),
            Key("◀", onPress = { c.moveLeft() }),
            Key("▶", onPress = { c.moveRight() }),
            Key("DEL", onPress = { c.delete() }),
            Key("AC", onPress = { c.clear() })
        ),
        listOf(
            insShift("x²", "x³", "²", "³"),
            insShift("√(", "∛(", "√(", "∛("),
            insShift("^", "ˣ√(", "^(", "ˣ√("),
            insShift("ln(", "e^(", "ln(", "e^("),
            insShift("log(", "10^(", "log(", "10^("),
            insShift("(-)", "Abs(", "-", "Abs(")
        ),
        listOf(
            insShift("sin(", "sin⁻¹(", "sin(", "asin("),
            insShift("cos(", "cos⁻¹(", "cos(", "acos("),
            insShift("tan(", "tan⁻¹(", "tan(", "atan("),
            insShift("π", "e", "π", "e"),
            insShift("nPr", "nCr", " nPr ", " nCr "),
            insShift("%", "!", "%", "!")
        ),
        listOf(
            ins("7"), ins("8"), ins("9"), ins("("), ins(")"), insShift(":", "Ran#", ":", "Ran#")
        ),
        listOf(
            ins("4"), ins("5"), ins("6"), ins("×"), ins("÷"), insShift(",", "Pol(", ",", "Pol(")
        ),
        listOf(
            ins("1"), ins("2"), ins("3"), ins("+"), ins("-"), insShift("Ans", "Rnd(", "Ans", "Rnd(")
        ),
        listOf(
            ins("0"), ins("."), ins("E"),
            Key("=", onPress = { c.execute() }),
            Key("▲", onPress = { c.historyBack() }),
            Key("▼", onPress = { c.historyForward() })
        ),
        listOf(
            Key("SET", onPress = onOpenSettings),
            ins("A"), ins("B"), ins("C"), ins("X"), ins("Y")
        )
    )
}
```

- [ ] **Step 5: MainActivity.kt（整体替换）**

```kotlin
package com.fincalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.comp.CompController
import com.fincalc.app.ui.comp.CompScreen

class MainActivity : ComponentActivity() {

    private val calcState = CalcState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinCalcApp(calcState)
                }
            }
        }
    }
}

/** 应用入口：按当前模式分发界面；模式菜单与设置由对话框弹出。 */
@Composable
fun FinCalcApp(state: CalcState) {
    var showModes by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    when (state.mode) {
        Mode.COMP -> CompScreen(
            controller = remember { CompController(state) },
            onOpenModes = { showModes = true },
            onOpenSettings = { showSettings = true }
        )
        else -> {
            // 计划 6 实现其余模式；占位界面
            Text("Mode ${state.mode.name} — under construction")
        }
    }

    if (showModes) ModeDialog(state, onDismiss = { showModes = false })
    if (showSettings) SettingsDialog(state, onDismiss = { showSettings = false })
}

/** 占位实现：Task 6 提供正式版。 */
@Composable
fun ModeDialog(state: CalcState, onDismiss: () -> Unit) {
    onDismiss()
}

/** 占位实现：Task 6 提供正式版。 */
@Composable
fun SettingsDialog(state: CalcState, onDismiss: () -> Unit) {
    onDismiss()
}
```

- [ ] **Step 6: 构建验证并提交**

```bash
source .dev/env.sh
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/
git commit -m "feat(ui): MathView 矢量绘制 + 键盘 + COMP 端到端界面"
```

预期：`BUILD SUCCESSFUL`，APK 产出。

---

### Task 6: 模式菜单 + 设置界面 + 双语切换

**Files:**
- Create: `app/src/main/java/com/fincalc/app/ui/dialogs/ModeDialog.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/dialogs/SettingsDialog.kt`
- Modify: `app/src/main/java/com/fincalc/app/MainActivity.kt`（删除两个占位对话框，从 ui.dialogs 导入）

**说明（实现子代理）**：参考实现，同 Task 5 的修正授权（可修 API 细节，不可改结构与行为）。

- [ ] **Step 1: ModeDialog.kt（12 模式选择网格；非 COMP 弹"下个版本实现"提示）**

```kotlin
package com.fincalc.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fincalc.app.R
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode

/** 模式选择对话框：12 模式网格。非 COMP 模式选中后弹提示并切回（计划 6 实现界面）。 */
@Composable
fun ModeDialog(state: CalcState, onDismiss: () -> Unit) {
    val modes = Mode.entries.toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mode_select)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in modes.chunked(3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (m in row) {
                            Button(
                                onClick = {
                                    if (m == Mode.COMP) {
                                        state.switchMode(m)
                                    } else {
                                        // 计划 6 实现：先切模式显示占位界面
                                        state.switchMode(m)
                                    }
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(m.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
```

**实现说明**：`Arrangement.spacedBy(4.dp)` 需要 `import androidx.compose.ui.unit.dp`（已含在上面的 import 中）；如 AlertDialog 参数签名有版本差异，允许修正用法。

- [ ] **Step 2: SettingsDialog.kt（设置项 + 语言切换）**

```kotlin
package com.fincalc.app.ui.dialogs

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fincalc.app.R
import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.state.CalcState
import java.util.Locale

/** 设置对话框：角度单位、数值显示、界面语言（切换即重建 Activity 生效）。 */
@Composable
fun SettingsDialog(state: CalcState, onDismiss: () -> Unit) {
    val activity = LocalContext.current as? Activity
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column {
                // 角度单位
                Text(stringResource(R.string.angle_unit))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (unit in AngleUnit.entries) {
                        RadioButton(
                            selected = state.settings.angle == unit,
                            onClick = { state.settings = state.settings.copy(angle = unit) }
                        )
                        Text(unit.name)
                    }
                }
                // 数值显示
                Text(stringResource(R.string.display_mode))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.settings.display == DisplayMode.Norm1,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Norm1) }
                    )
                    Text("Norm1")
                    RadioButton(
                        selected = state.settings.display == DisplayMode.Norm2,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Norm2) }
                    )
                    Text("Norm2")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.settings.display is DisplayMode.Fix,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Fix(3)) }
                    )
                    Text("Fix 3")
                    RadioButton(
                        selected = state.settings.display is DisplayMode.Sci,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Sci(5)) }
                    )
                    Text("Sci 5")
                }
                // 界面语言
                Text(stringResource(R.string.language))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.settings.chinese,
                        onClick = {
                            state.settings = state.settings.copy(chinese = true)
                            Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
                            activity?.recreate()
                        }
                    )
                    Text("中文")
                    RadioButton(
                        selected = !state.settings.chinese,
                        onClick = {
                            state.settings = state.settings.copy(chinese = false)
                            Locale.setDefault(Locale.ENGLISH)
                            activity?.recreate()
                        }
                    )
                    Text("English")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
```

- [ ] **Step 3: MainActivity.kt 修正**

删除 Task 5 的两个占位对话框函数，顶部添加导入：

```kotlin
import com.fincalc.app.ui.dialogs.ModeDialog
import com.fincalc.app.ui.dialogs.SettingsDialog
```

- [ ] **Step 4: 构建验证并提交**

```bash
source .dev/env.sh
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/
git commit -m "feat(ui): 模式菜单 + 设置界面（角度/显示/语言）+ 双语切换"
```

预期：`BUILD SUCCESSFUL`。

---

### Task 7: 收尾验证（本计划验收点，无新文件、无提交）

- [ ] **Step 1: 全量测试 + 完整构建**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
ls -la app/build/outputs/apk/debug/app-debug.apk
```

预期：全绿（181 + 6 + 5 + 11 = 203 测）；APK 产出。

- [ ] **Step 2: core 纯 Kotlin 验证**

```bash
grep -rn "import android" app/src/main/java/com/fincalc/app/core/ app/src/main/java/com/fincalc/app/state/ ; echo "grep exit=$?"
```

预期：core 与 state 均无匹配（exit=1）。

- [ ] **Step 3: 真机人工验证清单（写入提交信息/验收报告）**

在手机上安装 `app-debug.apk`，逐项走查：
1. COMP 输入 `1÷2π` → 屏幕显示分数 + π 并置的自然排版；EXE → 1.570796327
2. `2500+15%` → 2875；`-2²` → -4；`2(5+4)` → 18
3. sin(30)（Deg）→ 0.5；设置切 Rad 后 `cos(π)` → -1
4. 设置 Fix 3 后 `1÷6` → 0.167；Norm1 恢复 → 0.1666666667
5. 出错：`1÷0` → Math ERROR；编辑后重算正常
6. 历史：算几笔后 ▲▼ 回溯载入正确
7. SHIFT 第二功能键面切换（sin↔sin⁻¹ 等）
8. MODE 菜单弹出，12 模式在列；切换非 COMP 显示占位
9. 设置切 English → 界面文字变英文；切回中文
10. 杀掉 App 重开：设置与历史保留（DataStore）

- [ ] **Step 4: 仓库卫生**

```bash
git log --oneline -8
git status --short
```

---

## 完成标准（计划 5 验收）

- [ ] `./gradlew testDebugUnitTest` 全绿（203 测）
- [ ] `./gradlew assembleDebug` 产出 APK
- [ ] core/state 包无 `import android.*`
- [ ] 真机走查清单 10 项逐项通过（用户人工执行）
- [ ] COMP 模式端到端可用：自然显示输入（分数/根号/幂为矢量排版）+ 求值 + 错误显示 + 历史 + SHIFT + 设置 + 双语
- [ ] 提交历史清晰，工作区干净
