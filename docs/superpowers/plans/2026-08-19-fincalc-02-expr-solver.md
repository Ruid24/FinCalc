# 计划 2：core/expr 表达式引擎 + LaTeX 排版器 + core/solver 数值求解器

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用纯 Kotlin（不依赖安卓框架）实现计算器核心：线性输入 → 分词 → 递归下降解析 → AST；AST 两个消费者——求值器（Double）与 LaTeX 排版器；外加牛顿法为主、二分法兜底的数值求根器。全部 TDD，测试用例以说明书例题/技术信息章节为权威依据。

**Architecture:** 单模块 `:app` 内新增两个包：`com.fincalc.app.core.expr` 与 `com.fincalc.app.core.solver`（源码在 `app/src/main/java/`，测试在 `app/src/test/java/`，JUnit4，`./gradlew testDebugUnitTest` 运行）。core 包禁止 `import android.*`（Task 7 有 grep 验证步骤）。

**Tech Stack:** Kotlin 2.0.20、JUnit 4.13.2（均已就位）。**本计划不引入任何新依赖，全程无需联网**（Gradle 依赖在计划 1 已缓存）。

**环境前提：** 每次新开 shell 先 `source .dev/env.sh`。项目路径含中文，`gradle.properties` 已含 `android.overridePathCheck=true`（计划 1 修复），裸跑 `./gradlew` 即可。

**权威依据（说明书 OCR 文本，`说明书/MinerU_markdown_FC-200V_100V_CN.md`）：**

| 规则 | 依据 |
|---|---|
| 优先级表（函数括号 > 后缀² ³ ⁻¹ ! % 与 ^ ˣ√ > 一元负号 > nPr/nCr > ×÷与隐式乘 > +-） | CN-162「计算优先级」（L5170-5194） |
| **隐式乘法与 ×÷ 同级、从左到右**：`1÷2π = 1.570796327`（即 (1÷2)×π） | CN-162/163（L5196-5212）。注意：FC-200V 与 fx-ES 的"隐式乘法优先"不同 |
| `-2² = -4`、`(-2)² = 4`（² 优先于负号） | L5200-5202 |
| % 语义：独立/乘除语境 = ÷100（`2% = 0.02`、`150×20% = 30`、`660÷880% = 75`）；**加减语境 = 基数的百分之 y**（`2500+15% = 2875`、`3500-25% = 2625`、`1000-20% = 800`） | CN-37/38 百分比计算（L943-1042） |
| 多语句 `:`：从左至右依序执行，逐段显示中间结果 | CN-39（L1113-1137） |
| Ans：每段计算结束更新；变量 A B C D X Y 与独立存储器 M；未赋值变量为 0 | CN-42 起（L1178-1226） |
| 内部 15 位计算；显示 10 位有效数字；Rnd 按显示设定真实舍入（Norm→10 位尾数，Fix/Sci→指定位数） | CN-128/129（L3923-3985）、CN-165（L5233） |
| 函数输入范围表（sin/cos 输入范围、x! 0..69 整数、nPr/nCr 0≤r≤n<1e10、x^y 定义域等） | CN-165（L5237） |
| 错误体系：Math / Stack / Syntax / Insufficient MEM / Argument ERROR | CN-169（L5263-5335） |
| Ran#：小于 1 的 3 位假随机数（1000Ran# → 583/182/85 示例） | L3875-3893 |
| Pol/Rec：结果代入 X/Y；表达式内只取第一值（`Pol(√2,√2)+5 = 7`）；θ ∈ (-180°,180°] | CN-125/126（L3800-3822） |
| 结尾右括号可省略 | CN-36（L924-941） |
| 求解：说明书仅说明"牛顿法"，无收敛判据/初值/迭代上限；失败报错即 Math ERROR | L1672-1704、L1850-1858、L2725-2729 |

**设计决策（说明书未覆盖处，本计划自行规定并在此声明）：**

1. **规范输入语法**（计划 5 的虚拟键盘逐键产生，测试直接书写）：数字 `123`、`1.5`、`.5`、`1.2E3`（大写 E 为 ×10^x）；常量 `π`、`e`；变量 `A B C D X Y M Ans`；运算符 `+ - × ÷`（只接受 ×÷，不接受 `* /` 别名）；括号 `( )`；逗号 `,`；多语句 `:`；后缀 `² ³ ⁻¹ ! %`；`√( ∛(`（函数式带括号）；中缀 `ˣ√`（如 `5ˣ√(32)`）、`^`；函数 `sin cos tan asin acos atan sinh cosh tanh asinh acosh atanh log ln Abs Rnd Pol Rec`（输入时键盘自动补 `(`）；中缀 `nPr nCr`；`Ran#`。隐式乘法触发：完整后缀表达式之后紧跟 数字/`(`/函数/`√`/`∛`/π/e/变量/`Ran#`/一元负号以外的 primary 起始符。
2. `² ³ ⁻¹` 在解析期归一为 `Pow(e, 2|3|-1)`；`e^x`、`10^x` 不设专门函数，就是幂运算。
3. `%` 为后缀节点 `Percent`；求值器仅在 `Add/Sub` 的**直接右操作数**为 `Percent` 时按"基数百分比"语义（`a±b% = a ± a×b/100`），其余场合 `Percent(x) = x/100`。
4. `^` 右结合（`2^3^2 = 2^9 = 512`，说明书未规定，取数学惯例）；`^` 右操作数允许前导负号（`2^-3`）。负数底数的指数须为整数（说明书允许 `m/(2n+1)` 奇分母，本计划简化为仅整数，LaTeX/错误提示不受影响）。
5. `x√` 负被开方数仅允许奇整数次根（说明书三情形规则的整数化简），如 `3ˣ√(-27) = -3`。
6. 嵌套深度上限 24（模拟真机指令堆栈 24 级，CN-162 附近），超限报 Stack ERROR；属近似实现。
7. `Ran#` 通过 `EvalContext.nextRandom()` 注入随机源（返回 [0,1)），引擎取 `floor(r×1000)/1000`，测试可注入定值。
8. 多语句 `Program`：逐段求值、每段结束写回 `Ans`，返回各段结果列表，末段即答案。
9. **已知留白（明确不做，属后续计划）**：角度后缀 `° ʳ ᵍ`（DRG►，计划 5 键盘层处理）；金融 VARS 变量（`n`、`I%`、`PV` 等，计划 3 接入）；显示格式化 Fix/Sci/Norm 输出（属 `state`/UI，本计划引擎返回裸 Double）；Insufficient MEM 字节数限制（键盘输入层职责）；STAT 的估计值符号（计划 4）。
10. 说明书函数输入范围表（CN-165）只实现常用域检查 + 终值 Inf/NaN 兜底（→ Math ERROR），不逐条复刻范围表。

**LaTeX 排版约定（Task 5 测试逐字锁定；只使用 AndroidMath 保守支持子集）：**

- `a×b` → `a \times b`；`a÷b` → `\frac{a}{b}`；隐式乘 → 并置（`2 \pi`、`2 \sqrt{3}`）
- 幂 → `{base}^{exp}`；`√` → `\sqrt{...}`；`∛` → `\sqrt[3]{...}`；`ˣ√` → `\sqrt[n]{...}`
- `a!` → `a!`；`a%` → `a\%`；`nPr/nCr` → `n\mathrm{P}r`、`n\mathrm{C}r`
- 函数 → `\sin(...)`、`\sin^{-1}(...)`、`\ln(...)`、`\log(...)`、`\log_{m}(n)`、`\mathrm{Pol}(x, y)`、`\mathrm{Rec}(r, \theta)`、`\mathrm{Rnd}(x)`、`|x|`；`Ran#` → `\mathrm{Ran\#}`
- 指数计数法 `1.2E3` → `1.2 \times 10^{3}`
- 多语句 → `stmt1 : stmt2`
- 加括号规则：作为乘/隐式乘/阶乘/%/负号操作数的加减式加 `(...)`；幂底数为加减/负/乘/隐式乘时加 `(...)`；减法右操作数为加减时加 `(...)`

**修订记录（执行期）：**

- 2026-08-19（Task 3 质量审查发现）：说明书 L5182 将 x² 与 ^( 同列优先权 2、L5174 同级由左至右，真机 `2²^3 = (2²)³ = 64` 合法；初版 Parser 的 `parsePostfix` 循环未为 `^`/`ˣ√` 回流，误拒此类输入（Syntax ERROR）。已修订：`parsePower` 并入 `parsePostfix` 循环（同级左结合，`^` 经 `parsePowerOperand` 保持右结合），并新增回归测试 `postfix followed by caret or xroot`。ParserTest 总数 20。
- 2026-08-19（Task 4 质量审查发现，三项）：①`checkFinite` 只看 Double 的 Inf（上界约 1.8e308），未落实说明书 CN-165 的 ±9.999999999×10^99 计算范围，且存在中间超范围被后续运算"消化"的逃逸路径（如 `1÷(9E99×9E99)` 返回 0）；已改为 `checkRange`（|v| ≥ 1e100 即 Math ERROR）并对加减乘除/隐式乘结果逐节点检查（CN-169"中间或最后结果超范围"）。②`BigDecimal(v)` 构造器取二进制精确展开，致 `Rnd(2.675)` Fix2 得 2.67（真机 BCD 为 2.68）；已改 `BigDecimal.valueOf`。③`permComb` 大输入空转（约 1e10 次迭代才报错）；已加单调性提前报错。新增 4 个回归测试，EvaluatorTest 总数 26。
- 2026-08-19（Task 5 质量审查发现）：`factor()` 不包负式，导致 `2(-3)` 排成 `2 -3`（与减法 `2 - 3` 显示歧义）、`2500+(-3)%` 与 `2500+-3%` 同输出不同值等 3 处歧义。已修订：`factor()` 对 `Node.Neg` 同样加 `(...)`（`2 (-3)`、`(-3)\%`、`(-3)!`），既有 11 测试零回归。新增回归测试 `neg operand parenthesized to avoid ambiguity`，LatexTest 总数 12。
- 2026-08-19（Task 6 质量审查备注，均非阻塞未改码）：①异号函数值下溢（1e-300 量级）会使二分法乘积判号出现 -0.0 误判，金融量级物理不可达，后续如需根除可改用符号位比较。②牛顿 |Δx| 收敛轨道不校验残差，对 NPV 类温和斜率函数非实际问题。③二分法对区间端点 f 非有限零容忍——**计划 3 集成 IRR/NPV 时调用方必须把端点内缩（如 lower=-0.9999），不得在 i=-1 奇异点上设端点**；已在 Solver.solve 的 KDoc 中补充该注意事项。
- 2026-08-19（整体终审备注，均非阻塞未改码）：①浮点整数域边界：`log(10,1000)!` 这类"数学整数、Double 差 1 ulp"的输入会误报 Math ERROR（`ln1000/ln10=2.9999999999999996`），后续可选做近整数吸附。②`Num`/`Neg` 叶子不走 checkRange（`1E100×0` 得 0 不报错）；真机指数输入最多 2 位，计划 5 键盘层可自然堵死。③`expectRParen` 只允许全文末尾省略右括号，多语句分段末尾不允许（设计裁量）。④计划 5 实现"报错后光标定位"时，`CalcException` 需加结构化 `pos: Int?` 字段（增量改动）。⑤`DisplayMode.Fix/Sci` 的 digits 无 0-9 校验（UI 层约束即可）。
- 2026-08-19：Task 3 计划文本原写"18 个测试"，实际 19 个；Task 4 原写"19 个"，实际 22 个（计数小误，以代码为准）。

---

### Task 1: 错误模型与求值上下文

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/expr/CalcException.kt`
- Create: `app/src/main/java/com/fincalc/app/core/expr/Context.kt`
- Create: `app/src/test/java/com/fincalc/app/core/expr/ContextTest.kt`

- [ ] **Step 1: CalcException.kt**

```kotlin
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
```

- [ ] **Step 2: Context.kt**

```kotlin
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
```

- [ ] **Step 3: ContextTest.kt（先写测试）**

```kotlin
package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextTest {
    @Test
    fun `undefined variables default to zero`() {
        val ctx = DefaultContext()
        assertEquals(0.0, ctx.getVar("A"), 0.0)
        assertEquals(0.0, ctx.getVar("Ans"), 0.0)
    }

    @Test
    fun `set and get variable round trips`() {
        val ctx = DefaultContext()
        ctx.setVar("B", 57.0)
        assertEquals(57.0, ctx.getVar("B"), 0.0)
    }

    @Test
    fun `defaults are DEG and Norm1`() {
        val ctx = DefaultContext()
        assertEquals(AngleUnit.DEG, ctx.angle)
        assertEquals(DisplayMode.Norm1, ctx.display)
    }

    @Test
    fun `random source is injectable`() {
        val ctx = DefaultContext(random = { 0.583 })
        assertEquals(0.583, ctx.nextRandom(), 0.0)
    }

    @Test
    fun `angle unit conversion factors`() {
        assertEquals(Math.PI / 180, AngleUnit.DEG.toRadians, 1e-15)
        assertEquals(1.0, AngleUnit.RAD.toRadians, 0.0)
        assertEquals(Math.PI / 200, AngleUnit.GRA.toRadians, 1e-15)
    }
}
```

- [ ] **Step 4: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.expr.ContextTest"
```

预期：`BUILD SUCCESSFUL`，5 个测试全过。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/ app/src/test/java/com/fincalc/app/core/
git commit -m "feat(core/expr): 错误模型与求值上下文"
```

---

### Task 2: 分词器

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/expr/Token.kt`
- Create: `app/src/main/java/com/fincalc/app/core/expr/Tokenizer.kt`
- Create: `app/src/test/java/com/fincalc/app/core/expr/TokenizerTest.kt`

- [ ] **Step 1: Token.kt**

```kotlin
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
```

- [ ] **Step 2: Tokenizer.kt**

```kotlin
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
```

- [ ] **Step 3: TokenizerTest.kt（先写测试）**

```kotlin
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
```

- [ ] **Step 4: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.expr.TokenizerTest"
```

预期：`BUILD SUCCESSFUL`，13 个测试全过。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/ app/src/test/java/com/fincalc/app/core/
git commit -m "feat(core/expr): 分词器"
```

---

### Task 3: AST 与递归下降解析器

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/expr/Ast.kt`
- Create: `app/src/main/java/com/fincalc/app/core/expr/Parser.kt`
- Create: `app/src/test/java/com/fincalc/app/core/expr/ParserTest.kt`

- [ ] **Step 1: Ast.kt**

```kotlin
package com.fincalc.app.core.expr

/** 语法树节点。² ³ ⁻¹ 在解析期归一为 Pow；两个消费者：求值器、LaTeX 排版器。 */
sealed class Node {
    data class Num(val raw: String, val value: Double) : Node()
    object Pi : Node()
    object EConst : Node()
    data class Var(val name: String) : Node()
    object Ran : Node()
    data class Add(val l: Node, val r: Node) : Node()
    data class Sub(val l: Node, val r: Node) : Node()
    data class Mul(val l: Node, val r: Node) : Node()
    data class Div(val l: Node, val r: Node) : Node()
    data class ImplicitMul(val l: Node, val r: Node) : Node()
    data class Neg(val e: Node) : Node()
    data class Pow(val base: Node, val exp: Node) : Node()
    data class XRoot(val degree: Node, val radicand: Node) : Node()
    data class Sqrt(val e: Node) : Node()
    data class Cbrt(val e: Node) : Node()
    data class Fact(val e: Node) : Node()
    data class Percent(val e: Node) : Node()
    data class Perm(val n: Node, val r: Node) : Node()
    data class Comb(val n: Node, val r: Node) : Node()
    data class Func(val fn: FuncName, val args: List<Node>) : Node()
}

/** 一段完整输入：多语句用 : 连接，从左至右执行（说明书 CN-39）。 */
data class Program(val statements: List<Node>)
```

- [ ] **Step 2: Parser.kt**

```kotlin
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

        /** postfix := primary (²|³|⁻¹|!|%|'^' powerOperand|'ˣ√' '(' expr ')')* —— 优先级 2，同级左结合（^ 经 powerOperand 保持右结合） */
        private fun parsePostfix(depth: Int): Node {
            var e = parsePrimary(depth)
            while (true) {
                e = when (peek()) {
                    Token.Square -> { next(); Node.Pow(e, Node.Num("2", 2.0)) }
                    Token.Cube -> { next(); Node.Pow(e, Node.Num("3", 3.0)) }
                    Token.Recip -> { next(); Node.Pow(e, Node.Num("-1", -1.0)) }
                    Token.Bang -> { next(); Node.Fact(e) }
                    Token.Percent -> { next(); Node.Percent(e) }
                    Token.Caret -> { next(); Node.Pow(e, parsePowerOperand(depth)) }
                    Token.XRootTok -> {
                        next()
                        expectLParen()
                        val rad = parseExpr(depth + 1)
                        expectRParen()
                        Node.XRoot(e, rad)
                    }
                    else -> return e
                }
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
```

- [ ] **Step 3: ParserTest.kt（先写测试）**

```kotlin
package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ParserTest {
    private fun n(v: Double) =
        Node.Num(if (v == Math.floor(v) && v.isFinite()) v.toLong().toString() else v.toString(), v)

    @Test
    fun `simple precedence mul over add`() {
        val p = Parser.parse("2+3×4")
        assertEquals(Program(listOf(Node.Add(n(2.0), Node.Mul(n(3.0), n(4.0))))), p)
    }

    @Test
    fun `implicit mul same level as div and left associative`() {
        // 权威语义（说明书 CN-162）：1÷2π = (1÷2)×π
        val p = Parser.parse("1÷2π")
        assertEquals(Program(listOf(Node.ImplicitMul(Node.Div(n(1.0), n(2.0)), Node.Pi))), p)
    }

    @Test
    fun `implicit mul parenthesized binds inside parens`() {
        val p = Parser.parse("1÷(2π)")
        assertEquals(Program(listOf(Node.Div(n(1.0), Node.ImplicitMul(n(2.0), Node.Pi)))), p)
    }

    @Test
    fun `square binds tighter than unary minus`() {
        // 权威语义（说明书 L5200）：-2² = -(2²)
        val p = Parser.parse("-2²")
        assertEquals(Program(listOf(Node.Neg(Node.Pow(n(2.0), n(2.0))))), p)
    }

    @Test
    fun `paren neg square`() {
        val p = Parser.parse("(-2)²")
        assertEquals(Program(listOf(Node.Pow(Node.Neg(n(2.0)), n(2.0)))), p)
    }

    @Test
    fun `implicit mul before paren func var`() {
        assertEquals(
            Program(listOf(Node.ImplicitMul(n(2.0), Node.Add(n(5.0), n(4.0))))),
            Parser.parse("2(5+4)")
        )
        assertEquals(
            Program(listOf(Node.ImplicitMul(n(2.0), Node.Func(FuncName.SIN, listOf(n(30.0)))))),
            Parser.parse("2sin(30)")
        )
        assertEquals(
            Program(listOf(Node.ImplicitMul(n(5.0), Node.Var("A")))),
            Parser.parse("5A")
        )
        assertEquals(
            Program(listOf(Node.ImplicitMul(Node.Var("A"), Node.Func(FuncName.SIN, listOf(n(30.0)))))),
            Parser.parse("Asin(30)")
        )
    }

    @Test
    fun `power right associative and negative exponent`() {
        assertEquals(
            Program(listOf(Node.Pow(n(2.0), Node.Pow(n(3.0), n(2.0))))),
            Parser.parse("2^3^2")
        )
        assertEquals(
            Program(listOf(Node.Pow(n(2.0), Node.Neg(n(3.0))))),
            Parser.parse("2^-3")
        )
    }

    @Test
    fun `postfix normalization`() {
        assertEquals(
            Program(listOf(Node.Pow(n(2.0), n(3.0)))),
            Parser.parse("2³")
        )
        assertEquals(
            Program(listOf(Node.Pow(n(4.0), Node.Num("-1", -1.0)))),
            Parser.parse("4⁻¹")
        )
        assertEquals(
            Program(listOf(Node.Fact(Node.Add(n(5.0), n(3.0))))),
            Parser.parse("(5+3)!")
        )
        assertEquals(
            Program(listOf(Node.Percent(n(15.0)))),
            Parser.parse("15%")
        )
    }

    @Test
    fun `postfix followed by caret or xroot`() {
        // 说明书 L5182：x² 与 ^( 同属优先权 2，同级由左至右 → 2²^3 = (2²)³ = 64
        assertEquals(
            Program(listOf(Node.Pow(Node.Pow(n(2.0), n(2.0)), n(3.0)))),
            Parser.parse("2²^3")
        )
        assertEquals(
            Program(listOf(Node.XRoot(Node.Fact(n(3.0)), n(64.0)))),
            Parser.parse("3!ˣ√(64)")
        )
    }

    @Test
    fun `perm comb are infix above muldiv`() {
        assertEquals(
            Program(listOf(Node.Mul(Node.Perm(n(10.0), n(4.0)), n(2.0)))),
            Parser.parse("10 nPr 4×2")
        )
        assertEquals(
            Program(listOf(Node.Comb(n(10.0), n(4.0)))),
            Parser.parse("10 nCr 4")
        )
    }

    @Test
    fun `xroot infix with parenthesized radicand`() {
        assertEquals(
            Program(listOf(Node.XRoot(n(5.0), n(32.0)))),
            Parser.parse("5ˣ√(32)")
        )
    }

    @Test
    fun `sqrt cbrt function form`() {
        assertEquals(
            Program(listOf(
                Node.Add(
                    Node.ImplicitMul(Node.Add(Node.Sqrt(n(2.0)), n(1.0)), Node.Sub(Node.Sqrt(n(2.0)), n(1.0))),
                    Node.Cbrt(n(5.0))
                )
            )),
            Parser.parse("(√(2)+1)(√(2)-1)+∛(5)")
        )
    }

    @Test
    fun `two arg functions`() {
        assertEquals(
            Program(listOf(Node.Func(FuncName.LOG, listOf(n(2.0), n(16.0))))),
            Parser.parse("log(2,16)")
        )
        assertEquals(
            Program(listOf(Node.Func(FuncName.POL, listOf(Node.Sqrt(n(2.0)), Node.Sqrt(n(2.0)))))),
            Parser.parse("Pol(√(2),√(2))")
        )
    }

    @Test
    fun `multi statement program`() {
        val p = Parser.parse("3+3:3×3")
        assertEquals(
            Program(listOf(Node.Add(n(3.0), n(3.0)), Node.Mul(n(3.0), n(3.0)))),
            p
        )
    }

    @Test
    fun `trailing rparen may be omitted`() {
        assertEquals(
            Program(listOf(Node.Mul(Node.Add(n(2.0), n(3.0)), Node.Sub(n(4.0), n(1.0))))),
            Parser.parse("(2+3)×(4-1")
        )
    }

    @Test
    fun `empty input syntax error`() {
        assertThrows(CalcException::class.java) { Parser.parse("") }
    }

    @Test
    fun `dangling operator syntax error`() {
        assertThrows(CalcException::class.java) { Parser.parse("1+") }
    }

    @Test
    fun `mismatched paren syntax error`() {
        assertThrows(CalcException::class.java) { Parser.parse("(1+2))") }
    }

    @Test
    fun `deep nesting stack error`() {
        val deep = "(".repeat(30) + "1" + ")".repeat(30)
        val e = assertThrows(CalcException::class.java) { Parser.parse(deep) }
        assertEquals(CalcException.Kind.STACK, e.kind)
    }

    @Test
    fun `moderate nesting ok`() {
        val ok = "(".repeat(20) + "1" + ")".repeat(20)
        assertEquals(Program(listOf(n(1.0))), Parser.parse(ok))
    }
}
```

- [ ] **Step 4: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.expr.ParserTest"
```

预期：`BUILD SUCCESSFUL`，18 个测试全过。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/ app/src/test/java/com/fincalc/app/core/
git commit -m "feat(core/expr): AST 与递归下降解析器"
```

---

### Task 4: 求值器与引擎门面

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/expr/Evaluator.kt`
- Create: `app/src/main/java/com/fincalc/app/core/expr/ExprEngine.kt`
- Create: `app/src/test/java/com/fincalc/app/core/expr/EvaluatorTest.kt`

- [ ] **Step 1: Evaluator.kt**

```kotlin
package com.fincalc.app.core.expr

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * 求值器：AST → Double。Double 精度（15-16 位）近似真机 15 位内部计算（说明书 CN-165）。
 * 终值 Inf/NaN 一律报 Math ERROR。
 */
object Evaluator {

    fun eval(program: Program, ctx: EvalContext): Double = evalAll(program, ctx).last()

    /** 逐段求值；每段结束写回 Ans（说明书 CN-42：E 执行计算时更新答案存储器）。 */
    fun evalAll(program: Program, ctx: EvalContext): List<Double> =
        program.statements.map { stmt ->
            val v = checkRange(evalNode(stmt, ctx))
            ctx.setVar("Ans", v)
            v
        }

    /**
     * 说明书 CN-165：计算范围 ±9.999999999×10^99；CN-169：中间或最后结果超出容许范围即 Math ERROR。
     * 注意 Double 上界（约 1.8e308）远大于真机范围，故必须显式按 1e100 检查，不能只看 Inf。
     */
    private fun checkRange(v: Double): Double =
        if (v.isNaN() || v.isInfinite() || abs(v) >= 1e100) {
            throw CalcException(CalcException.Kind.MATH, "结果超出计算范围")
        } else {
            v
        }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)

    fun evalNode(node: Node, ctx: EvalContext): Double = when (node) {
        is Node.Num -> node.value
        Node.Pi -> Math.PI
        Node.EConst -> Math.E
        is Node.Var -> ctx.getVar(node.name)
        // 说明书：Ran# 产生小于 1 的 3 位假随机数
        Node.Ran -> floor(ctx.nextRandom() * 1000) / 1000

        // 说明书 CN-37/38：加减语境中 b% 意为"基数 a 的百分之 b"
        is Node.Add -> {
            val l = evalNode(node.l, ctx)
            val r = node.r
            checkRange(if (r is Node.Percent) l + l * evalNode(r.e, ctx) / 100 else l + evalNode(r, ctx))
        }
        is Node.Sub -> {
            val l = evalNode(node.l, ctx)
            val r = node.r
            checkRange(if (r is Node.Percent) l - l * evalNode(r.e, ctx) / 100 else l - evalNode(r, ctx))
        }
        is Node.Mul -> checkRange(evalNode(node.l, ctx) * evalNode(node.r, ctx))
        is Node.Div -> {
            val r = evalNode(node.r, ctx)
            if (r == 0.0) mathErr("除以 0")
            checkRange(evalNode(node.l, ctx) / r)
        }
        is Node.ImplicitMul -> checkRange(evalNode(node.l, ctx) * evalNode(node.r, ctx))
        is Node.Neg -> -evalNode(node.e, ctx)
        is Node.Pow -> powChecked(evalNode(node.base, ctx), evalNode(node.exp, ctx))
        is Node.XRoot -> {
            val d = evalNode(node.degree, ctx)
            if (d == 0.0) mathErr("0 次根")
            val x = evalNode(node.radicand, ctx)
            if (x < 0) {
                // 说明书范围表的整数化简：负被开方数仅允许奇整数次根
                if (d != floor(d) || abs(d) >= 1e15 || abs(d) % 2.0 != 1.0) mathErr("负数的偶次根")
                -powChecked(-x, 1.0 / d)
            } else {
                powChecked(x, 1.0 / d)
            }
        }
        is Node.Sqrt -> {
            val v = evalNode(node.e, ctx)
            if (v < 0) mathErr("负数开平方")
            sqrt(v)
        }
        is Node.Cbrt -> cbrt(evalNode(node.e, ctx))
        is Node.Fact -> factorial(evalNode(node.e, ctx))
        is Node.Percent -> evalNode(node.e, ctx) / 100
        is Node.Perm -> permComb(evalNode(node.n, ctx), evalNode(node.r, ctx), true)
        is Node.Comb -> permComb(evalNode(node.n, ctx), evalNode(node.r, ctx), false)
        is Node.Func -> evalFunc(node, ctx)
    }

    /** x^y 定义域（说明书 CN-165 的整数化简）：x>0 任意 y；x=0 要求 y>0；x<0 要求 y 为整数。 */
    private fun powChecked(base: Double, exp: Double): Double {
        val v = when {
            base > 0 -> base.pow(exp)
            base == 0.0 -> {
                if (exp <= 0) mathErr("0 的非正次幂")
                0.0
            }
            else -> {
                if (exp != floor(exp) || abs(exp) >= 1e15) mathErr("负数的非整数次幂")
                base.pow(exp)
            }
        }
        return checkRange(v)
    }

    /** 说明书：x! 自变量为 0 ≤ x ≤ 69 的整数。 */
    private fun factorial(x: Double): Double {
        if (x < 0 || x > 69 || x != floor(x)) mathErr("阶乘自变量须为 0..69 的整数")
        var r = 1.0
        var i = 2L
        while (i <= x.toLong()) {
            r *= i
            i++
        }
        return r
    }

    /** 说明书：n、r 为整数且 0 ≤ r ≤ n < 1e10，结果须 < 1e100。结果单调不减，超范围即提前报错。 */
    private fun permComb(n0: Double, r0: Double, perm: Boolean): Double {
        if (n0 != floor(n0) || r0 != floor(r0) || n0 < 0 || r0 < 0 || r0 > n0 || n0 >= 1e10) {
            mathErr("nPr/nCr 自变量超出范围")
        }
        var result = 1.0
        var k = 0.0
        if (perm) {
            while (k < r0) {
                result *= (n0 - k)
                if (result >= 1e100 || !result.isFinite()) mathErr("结果超出计算范围")
                k++
            }
        } else {
            val rr = minOf(r0, n0 - r0)
            while (k < rr) {
                result = result * (n0 - k) / (k + 1)
                if (result >= 1e100 || !result.isFinite()) mathErr("结果超出计算范围")
                k++
            }
            result = round(result)
        }
        return checkRange(result)
    }

    /**
     * Rnd（说明书 CN-128/129）：Norm → 尾数舍入至 10 位；Fix → 指定小数位；Sci → 指定有效位。
     * 用 BigDecimal.valueOf（十进制最短表示）而非构造器（二进制精确展开），
     * 使 Rnd(2.675) 按十进制的 2.675 舍入为 2.68，与真机 BCD 行为一致。
     */
    private fun rnd(v: Double, mode: DisplayMode): Double = when (mode) {
        is DisplayMode.Fix -> BigDecimal.valueOf(v).setScale(mode.digits, RoundingMode.HALF_UP).toDouble()
        is DisplayMode.Sci ->
            if (v == 0.0) 0.0 else BigDecimal.valueOf(v).round(MathContext(mode.digits, RoundingMode.HALF_UP)).toDouble()
        else ->
            if (v == 0.0) 0.0 else BigDecimal.valueOf(v).round(MathContext(10, RoundingMode.HALF_UP)).toDouble()
    }

    private fun evalFunc(node: Node.Func, ctx: EvalContext): Double {
        val f = ctx.angle.toRadians
        fun arity(n: Int) {
            if (node.args.size != n) {
                throw CalcException(CalcException.Kind.SYNTAX, "${node.fn} 需要 $n 个参数")
            }
        }
        fun a(i: Int) = evalNode(node.args[i], ctx)
        return when (node.fn) {
            FuncName.SIN -> { arity(1); sin(a(0) * f) }
            FuncName.COS -> { arity(1); cos(a(0) * f) }
            FuncName.TAN -> {
                arity(1)
                val t = a(0) * f
                val r = t % Math.PI
                if (abs(abs(r) - Math.PI / 2) < 1e-12) mathErr("tan 奇点")
                tan(t)
            }
            FuncName.ASIN -> { arity(1); val v = a(0); if (abs(v) > 1) mathErr("asin 定义域"); asin(v) / f }
            FuncName.ACOS -> { arity(1); val v = a(0); if (abs(v) > 1) mathErr("acos 定义域"); acos(v) / f }
            FuncName.ATAN -> { arity(1); atan(a(0)) / f }
            FuncName.SINH -> { arity(1); sinh(a(0)) }
            FuncName.COSH -> { arity(1); cosh(a(0)) }
            FuncName.TANH -> { arity(1); tanh(a(0)) }
            FuncName.ASINH -> { arity(1); asinh(a(0)) }
            FuncName.ACOSH -> { arity(1); val v = a(0); if (v < 1) mathErr("acosh 定义域"); acosh(v) }
            FuncName.ATANH -> { arity(1); val v = a(0); if (abs(v) >= 1) mathErr("atanh 定义域"); atanh(v) }
            FuncName.LN -> { arity(1); val v = a(0); if (v <= 0) mathErr("ln 定义域"); ln(v) }
            FuncName.LOG -> when (node.args.size) {
                1 -> { val v = a(0); if (v <= 0) mathErr("log 定义域"); log10(v) }
                2 -> {
                    val m = a(0)
                    val v = a(1)
                    if (m <= 0 || m == 1.0 || v <= 0) mathErr("log 定义域")
                    ln(v) / ln(m)
                }
                else -> throw CalcException(CalcException.Kind.SYNTAX, "log 需要 1 或 2 个参数")
            }
            FuncName.ABS -> { arity(1); abs(a(0)) }
            FuncName.RND -> { arity(1); rnd(a(0), ctx.display) }
            // 说明书 CN-125/126：Pol 结果 r→X、θ→Y（θ 按当前角度单位，∈(-180°,180°]）；表达式内取第一值
            FuncName.POL -> {
                arity(2)
                val x = a(0)
                val y = a(1)
                val r = hypot(x, y)
                val theta = atan2(y, x) / f
                ctx.setVar("X", r)
                ctx.setVar("Y", theta)
                r
            }
            FuncName.REC -> {
                arity(2)
                val r = a(0)
                val theta = a(1) * f
                val x = r * cos(theta)
                val y = r * sin(theta)
                ctx.setVar("X", x)
                ctx.setVar("Y", y)
                x
            }
        }
    }
}
```

- [ ] **Step 2: ExprEngine.kt（门面，UI 层未来只与本类交互）**

```kotlin
package com.fincalc.app.core.expr

/** 表达式引擎门面：线性输入串 → 解析 / 求值 / LaTeX。 */
object ExprEngine {
    fun parse(input: String): Program = Parser.parse(input)

    fun eval(input: String, ctx: EvalContext = DefaultContext()): Double =
        Evaluator.eval(parse(input), ctx)

    fun evalAll(input: String, ctx: EvalContext = DefaultContext()): List<Double> =
        Evaluator.evalAll(parse(input), ctx)

    fun latex(input: String): String = Latex.render(parse(input))
}
```

- [ ] **Step 3: EvaluatorTest.kt（先写测试；数值断言全部来自说明书例题或其直接推论）**

```kotlin
package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvaluatorTest {
    private fun ev(input: String, ctx: EvalContext = DefaultContext()) = ExprEngine.eval(input, ctx)

    // ---- 算术与优先级（说明书 CN-35 起） ----

    @Test
    fun `manual arithmetic examples`() {
        assertEquals(36.0, ev("7×8-4×5"), 1e-12)              // L888
        assertEquals(15.0, ev("(2+3)×(4-1)"), 1e-12)         // L924
        assertEquals(24.0, ev("2(5+4)-2×(-3)"), 1e-12)       // L671
    }

    @Test
    fun `implicit mul same level as div`() {
        assertEquals(Math.PI / 2, ev("1÷2π"), 1e-9)          // L5196：1.570796327
        assertEquals(0.1591549431, ev("1÷(2π)"), 1e-9)
    }

    @Test
    fun `neg and square precedence`() {
        assertEquals(-4.0, ev("-2²"), 0.0)                   // L5200
        assertEquals(4.0, ev("(-2)²"), 0.0)
    }

    // ---- 百分比（说明书 CN-37/38） ----

    @Test
    fun `percent semantics`() {
        assertEquals(0.02, ev("2%"), 1e-15)                  // L945
        assertEquals(30.0, ev("150×20%"), 1e-12)             // L957
        assertEquals(75.0, ev("660÷880%"), 1e-9)             // L972
        assertEquals(2875.0, ev("2500+15%"), 1e-9)           // L984
        assertEquals(2625.0, ev("3500-25%"), 1e-9)           // L1000
    }

    @Test
    fun `percent with ans`() {
        val ctx = DefaultContext()
        assertEquals(1000.0, ev("168+98+734", ctx), 1e-12)   // L1018
        assertEquals(800.0, ev("Ans-20%", ctx), 1e-9)        // L1018-1042
    }

    // ---- 多语句与 Ans（说明书 CN-39/42） ----

    @Test
    fun `multi statement evaluates left to right`() {
        val ctx = DefaultContext()
        val results = ExprEngine.evalAll("3+3:3×3", ctx)
        assertEquals(listOf(6.0, 9.0), results)              // L1113
        assertEquals(9.0, ctx.getVar("Ans"), 0.0)
    }

    @Test
    fun `ans chain`() {
        val ctx = DefaultContext()
        assertEquals(12.0, ev("3×4", ctx), 0.0)
        assertEquals(0.4, ev("Ans÷30", ctx), 1e-12)          // L1196
        assertEquals(579.0, ev("123+456", ctx), 0.0)
        assertEquals(210.0, ev("789-Ans", ctx), 1e-12)       // L1214
    }

    @Test
    fun `variables`() {
        val ctx = DefaultContext()
        ctx.setVar("B", 57.0)                                // 9×6+3，L1323
        ctx.setVar("C", 40.0)                                // 5×8
        assertEquals(1.425, ev("B÷C", ctx), 1e-12)
    }

    // ---- 三角与角度（说明书函数章 CN-118 起） ----

    @Test
    fun `trig in degrees`() {
        assertEquals(0.5, ev("sin(30)"), 1e-9)               // L3549
        assertEquals(30.0, ev("asin(0.5)"), 1e-9)            // L3567
        assertEquals(180.0, ev("acos(-1)"), 1e-9)            // L3624
    }

    @Test
    fun `trig in radians and grads`() {
        val rad = DefaultContext(angle = AngleUnit.RAD)
        assertEquals(-1.0, ev("cos(π)", rad), 1e-9)          // L3614
        assertEquals(Math.PI, ev("acos(-1)", rad), 1e-9)     // L3648
        val gra = DefaultContext(angle = AngleUnit.GRA)
        assertEquals(0.0, ev("cos(100)", gra), 1e-9)         // L3622
    }

    @Test
    fun `hyperbolic`() {
        assertEquals(1.175201194, ev("sinh(1)"), 1e-9)       // L3571
        assertEquals(0.0, ev("acosh(1)"), 0.0)               // L3590
    }

    // ---- 对数指数幂根 ----

    @Test
    fun `log ln exp`() {
        assertEquals(4.0, ev("log(2,16)"), 1e-9)             // L3656
        assertEquals(1.204119983, ev("log(16)"), 1e-9)       // L3667
        assertEquals(4.49980967, ev("ln(90)"), 1e-9)         // L3678
        assertEquals(1.0, ev("ln(e)"), 1e-12)                // L3689
        assertEquals(kotlin.math.exp(10.0), ev("e^10"), 1e-6) // L3697：22026.46579
        assertEquals(1200.0, ev("1.2×10^3"), 1e-12)          // L3712
    }

    @Test
    fun `powers and roots`() {
        assertEquals(16.0, ev("(1+1)^(2+2)"), 1e-12)         // L3720
        assertEquals(8.0, ev("2³"), 0.0)                     // L3733
        assertEquals(1.0, ev("(√(2)+1)(√(2)-1)"), 1e-9)      // L3739
        assertEquals(2.0, ev("5ˣ√(32)"), 1e-9)               // L3749
        assertEquals(-1.290024053, ev("∛(5)+∛(-27)"), 1e-9)  // L3760
        assertEquals(-3.0, ev("3ˣ√(-27)"), 1e-9)
        assertEquals(0.125, ev("2^-3"), 1e-12)
    }

    // ---- Pol/Rec（说明书 CN-125/126） ----

    @Test
    fun `pol assigns X Y and returns r`() {
        val ctx = DefaultContext()
        assertEquals(2.0, ev("Pol(√(2),√(2))", ctx), 1e-9)
        assertEquals(2.0, ctx.getVar("X"), 1e-9)
        assertEquals(45.0, ctx.getVar("Y"), 1e-9)
    }

    @Test
    fun `rec assigns X Y and returns x`() {
        val ctx = DefaultContext()
        assertEquals(1.732050808, ev("Rec(2,30)", ctx), 1e-9)
        assertEquals(1.732050808, ctx.getVar("X"), 1e-9)
        assertEquals(1.0, ctx.getVar("Y"), 1e-9)
    }

    @Test
    fun `pol inside expression yields first value`() {
        assertEquals(7.0, ev("Pol(√(2),√(2))+5"), 1e-9)      // L3800
    }

    // ---- 阶乘、Abs、nPr/nCr、Ran#、Rnd ----

    @Test
    fun `factorial abs perm comb`() {
        assertEquals(40320.0, ev("(5+3)!"), 0.0)             // L3847
        assertEquals(5.0, ev("Abs(2-7)"), 0.0)               // L3863
        assertEquals(5040.0, ev("10 nPr 4"), 0.0)            // L3905
        assertEquals(210.0, ev("10 nCr 4"), 0.0)             // L3921
    }

    @Test
    fun `ran hash uses injected random`() {
        val ctx = DefaultContext(random = { 0.5839 })
        assertEquals(0.583, ev("Ran#", ctx), 0.0)            // L3875：3 位假随机数
        assertEquals(583.0, ev("1000Ran#", ctx), 1e-12)      // L3877
    }

    @Test
    fun `rnd respects display mode`() {
        val fix3 = DefaultContext(display = DisplayMode.Fix(3))
        // 说明书 L3935-3985：Fix3 下 Rnd(200÷7)=28.571，再 ×14 = 399.994
        assertEquals(399.994, ev("Rnd(200÷7)×14", fix3), 1e-9)
        assertEquals(400.0, ev("200÷7×14", fix3), 1e-9)      // 不 Rnd 时内部 15 位计算
        val norm = DefaultContext()
        assertEquals(0.3333333333, ev("Rnd(1÷3)", norm), 1e-12)
    }

    @Test
    fun `constants`() {
        assertEquals(Math.PI, ev("π"), 0.0)
        assertEquals(Math.E, ev("e"), 0.0)
    }

    // ---- 计算范围（说明书 CN-165：±9.999999999×10^99；CN-169：中间结果超范围即 Math ERROR） ----

    @Test
    fun `range enforcement`() {
        assertEquals(9.9E99, ev("9.9E99"), 1e85)             // 范围内最大量级合法
        assertMathErr("1E100")                               // 终值超范围
        assertMathErr("9E99+9E99")                           // 中间加法超范围（1.8E100，Double 内有限但真机报错）
        assertMathErr("9E99×9E99")                           // 中间乘法超范围
        assertMathErr("1÷(9E99×9E99)")                       // 中间超范围不得被后续运算"消化"成 0
    }

    @Test
    fun `rnd decimal boundary`() {
        // BigDecimal.valueOf 按十进制舍入：2.675 在真机 BCD 中精确存在 → 2.68
        val fix2 = DefaultContext(display = DisplayMode.Fix(2))
        assertEquals(2.68, ev("Rnd(2.675)", fix2), 0.0)
    }

    @Test
    fun `perm comb overflow fails fast`() {
        assertMathErr("999999999 nPr 999999999")             // 结果单调增长，超 1e100 即报错，不空转
        assertMathErr("999999999 nCr 499999999")
    }

    @Test
    fun `chained percent`() {
        // 卡西欧逐步加成：100+5%+10% = (105)+105×10/100 = 115.5
        assertEquals(115.5, ev("100+5%+10%"), 1e-9)
    }

    // ---- 错误（说明书 CN-169） ----

    @Test
    fun `math errors`() {
        assertMathErr("1÷0")
        assertMathErr("√(-1)")
        assertMathErr("log(0)")
        assertMathErr("ln(-5)")
        assertMathErr("asin(2)")
        assertMathErr("acos(1.5)")
        assertMathErr("tan(90)")
        assertMathErr("70!")
        assertMathErr("2.5!")
        assertMathErr("(-2)^0.5")
        assertMathErr("0^0")
        assertMathErr("2ˣ√(-16)")
        assertMathErr("10 nPr 4.5")
        assertMathErr("atanh(1)")
    }

    @Test
    fun `syntax error on wrong arity`() {
        val e = assertThrows(CalcException::class.java) { ev("log(1,2,3)") }
        assertEquals(CalcException.Kind.SYNTAX, e.kind)
    }

    private fun assertMathErr(input: String) {
        val e = assertThrows(CalcException::class.java) { ev(input) }
        assertEquals("输入 $input 应报 Math ERROR", CalcException.Kind.MATH, e.kind)
    }
}
```

- [ ] **Step 4: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.expr.EvaluatorTest"
```

预期：`BUILD SUCCESSFUL`，19 个测试全过。若有个别断言因 Double 精度在 1e-9 容差边缘失败，先核对计算值再按实际值放宽到 1e-8 并在测试旁注释原因，不得改实现逻辑凑测试。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/ app/src/test/java/com/fincalc/app/core/
git commit -m "feat(core/expr): 求值器与引擎门面（说明书例题全过）"
```

---

### Task 5: LaTeX 排版器

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/expr/Latex.kt`
- Create: `app/src/test/java/com/fincalc/app/core/expr/LatexTest.kt`

- [ ] **Step 1: Latex.kt**

```kotlin
package com.fincalc.app.core.expr

/**
 * 排版器：AST → LaTeX 字符串。
 * 排版约定见计划 2 头部，测试逐字锁定。UI 的自然显示由 core/render 的自研排版器承担
 * （AndroidMath 路线已弃用）；本类保留为 LaTeX 文本通道（调试/日志/将来换库的备用）。
 */
object Latex {

    fun render(program: Program): String = program.statements.joinToString(" : ") { node(it) }

    private fun node(n: Node): String = when (n) {
        is Node.Num -> num(n)
        Node.Pi -> "\\pi"
        Node.EConst -> "e"
        is Node.Var -> n.name
        Node.Ran -> "\\mathrm{Ran\\#}"
        is Node.Add -> "${node(n.l)} + ${node(n.r)}"
        is Node.Sub -> "${node(n.l)} - ${factor(n.r)}"
        is Node.Mul -> "${factor(n.l)} \\times ${factor(n.r)}"
        is Node.Div -> "\\frac{${node(n.l)}}{${node(n.r)}}"
        is Node.ImplicitMul -> "${factor(n.l)} ${factor(n.r)}"
        is Node.Neg -> "-${factor(n.e)}"
        is Node.Pow -> "{${base(n.base)}}^{${node(n.exp)}}"
        is Node.XRoot -> "\\sqrt[${node(n.degree)}]{${node(n.radicand)}}"
        is Node.Sqrt -> "\\sqrt{${node(n.e)}}"
        is Node.Cbrt -> "\\sqrt[3]{${node(n.e)}}"
        is Node.Fact -> "${factor(n.e)}!"
        is Node.Percent -> "${factor(n.e)}\\%"
        is Node.Perm -> "${factor(n.n)}\\mathrm{P}${factor(n.r)}"
        is Node.Comb -> "${factor(n.n)}\\mathrm{C}${factor(n.r)}"
        is Node.Func -> func(n)
    }

    /** 指数计数法自然显示：1.2E3 → 1.2 \times 10^{3} */
    private fun num(n: Node.Num): String {
        val e = n.raw.indexOf('E')
        return if (e > 0) {
            "${n.raw.substring(0, e)} \\times 10^{${n.raw.substring(e + 1)}}"
        } else {
            n.raw
        }
    }

    /** 因子位置（乘/隐式乘/阶乘/%/负号/减法右侧）：加减式与负式加 (...) 保持语义、避免显示歧义（如 2(-3) 不排成 2 -3） */
    private fun factor(n: Node): String = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg -> "(${node(n)})"
        else -> node(n)
    }

    /** 幂底数：加减/负/乘/隐式乘需加 (...) */
    private fun base(n: Node): String = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg, is Node.Mul, is Node.ImplicitMul -> "(${node(n)})"
        else -> node(n)
    }

    private fun func(n: Node.Func): String {
        val a = n.args
        return when (n.fn) {
            FuncName.SIN -> "\\sin(${node(a[0])})"
            FuncName.COS -> "\\cos(${node(a[0])})"
            FuncName.TAN -> "\\tan(${node(a[0])})"
            FuncName.ASIN -> "\\sin^{-1}(${node(a[0])})"
            FuncName.ACOS -> "\\cos^{-1}(${node(a[0])})"
            FuncName.ATAN -> "\\tan^{-1}(${node(a[0])})"
            FuncName.SINH -> "\\sinh(${node(a[0])})"
            FuncName.COSH -> "\\cosh(${node(a[0])})"
            FuncName.TANH -> "\\tanh(${node(a[0])})"
            FuncName.ASINH -> "\\sinh^{-1}(${node(a[0])})"
            FuncName.ACOSH -> "\\cosh^{-1}(${node(a[0])})"
            FuncName.ATANH -> "\\tanh^{-1}(${node(a[0])})"
            FuncName.LN -> "\\ln(${node(a[0])})"
            FuncName.LOG -> if (a.size == 2) "\\log_{${node(a[0])}}(${node(a[1])})" else "\\log(${node(a[0])})"
            FuncName.ABS -> "|${node(a[0])}|"
            FuncName.RND -> "\\mathrm{Rnd}(${node(a[0])})"
            FuncName.POL -> "\\mathrm{Pol}(${node(a[0])}, ${node(a[1])})"
            FuncName.REC -> "\\mathrm{Rec}(${node(a[0])}, ${node(a[1])})"
        }
    }
}
```

- [ ] **Step 2: LatexTest.kt（先写测试，逐字锁定排版约定）**

```kotlin
package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Test

class LatexTest {
    private fun lx(input: String) = ExprEngine.latex(input)

    @Test
    fun `arithmetic`() {
        assertEquals("2 + 3 \\times 4", lx("2+3×4"))
        assertEquals("2 \\times (3 + 4)", lx("2×(3+4)"))
        assertEquals("5 - (2 + 3)", lx("5-(2+3)"))
        assertEquals("5 - 2 - 3", lx("5-2-3"))
        assertEquals("5 - (2 - 3)", lx("5-(2-3)"))
    }

    @Test
    fun `division renders as fraction`() {
        // 权威语义对照：1÷2π 按 (1÷2)×π 排版
        assertEquals("\\frac{1}{2} \\pi", lx("1÷2π"))
        assertEquals("\\frac{1}{2 \\pi}", lx("1÷(2π)"))
        assertEquals("\\frac{2 + 3}{4 - 1}", lx("(2+3)÷(4-1)"))
    }

    @Test
    fun `implicit multiplication juxtaposed`() {
        assertEquals("2 \\pi", lx("2π"))
        assertEquals("5 A", lx("5A"))
        assertEquals("2 (5 + 4)", lx("2(5+4)"))
        assertEquals("2 \\sqrt{3}", lx("2√(3)"))
    }

    @Test
    fun `powers`() {
        assertEquals("{2}^{3}", lx("2^3"))
        assertEquals("-{2}^{2}", lx("-2²"))
        assertEquals("{(-2)}^{2}", lx("(-2)²"))
        assertEquals("{2}^{{3}^{2}}", lx("2^3^2"))
        assertEquals("{2}^{-3}", lx("2^-3"))
        assertEquals("{e}^{10}", lx("e^10"))
        assertEquals("{X}^{-1}", lx("X⁻¹"))
    }

    @Test
    fun `roots`() {
        assertEquals("\\sqrt{2}", lx("√(2)"))
        assertEquals("\\sqrt[3]{5}", lx("∛(5)"))
        assertEquals("\\sqrt[5]{32}", lx("5ˣ√(32)"))
    }

    @Test
    fun `postfix`() {
        assertEquals("(5 + 3)!", lx("(5+3)!"))
        assertEquals("5!", lx("5!"))
        assertEquals("2500 + 15\\%", lx("2500+15%"))
    }

    @Test
    fun `perm comb`() {
        assertEquals("10\\mathrm{P}4", lx("10 nPr 4"))
        assertEquals("10\\mathrm{C}4", lx("10 nCr 4"))
    }

    @Test
    fun `functions`() {
        assertEquals("\\sin(30)", lx("sin(30)"))
        assertEquals("\\sin^{-1}(0.5)", lx("asin(0.5)"))
        assertEquals("\\sinh(1)", lx("sinh(1)"))
        assertEquals("\\cosh^{-1}(1)", lx("acosh(1)"))
        assertEquals("\\ln(90)", lx("ln(90)"))
        assertEquals("\\log(16)", lx("log(16)"))
        assertEquals("\\log_{2}(16)", lx("log(2,16)"))
        assertEquals("|2 - 7|", lx("Abs(2-7)"))
        assertEquals("\\mathrm{Rnd}(Ans)", lx("Rnd(Ans)"))
        assertEquals("\\mathrm{Pol}(\\sqrt{2}, \\sqrt{2})", lx("Pol(√(2),√(2))"))
        assertEquals("\\mathrm{Rec}(2, 30)", lx("Rec(2,30)"))
    }

    @Test
    fun `ran hash`() {
        assertEquals("\\mathrm{Ran\\#}", lx("Ran#"))
        assertEquals("1000 \\mathrm{Ran\\#}", lx("1000Ran#"))
    }

    @Test
    fun `scientific literal natural display`() {
        assertEquals("1.2 \\times 10^{3}", lx("1.2E3"))
        assertEquals("1.2 \\times 10^{-3}", lx("1.2E-3"))
    }

    @Test
    fun `multi statement`() {
        assertEquals("3 + 3 : 3 \\times 3", lx("3+3:3×3"))
    }

    @Test
    fun `neg operand parenthesized to avoid ambiguity`() {
        // 审查发现：隐式乘/后缀运算符的负操作数不加括号会与减法等产生显示歧义
        assertEquals("2 (-3)", lx("2(-3)"))
        assertEquals("2 \\times (-3)", lx("2×-3"))
        assertEquals("5 - (-3)", lx("5--3"))
        assertEquals("2500 + (-3)\\%", lx("2500+(-3)%"))
        assertEquals("(-3)!", lx("(-3)!"))
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.expr.LatexTest"
```

预期：`BUILD SUCCESSFUL`，11 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/ app/src/test/java/com/fincalc/app/core/
git commit -m "feat(core/expr): LaTeX 排版器"
```

---

### Task 6: core/solver 数值求根器

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/solver/Solver.kt`
- Create: `app/src/test/java/com/fincalc/app/core/solver/SolverTest.kt`

- [ ] **Step 1: Solver.kt**

```kotlin
package com.fincalc.app.core.solver

import com.fincalc.app.core.expr.CalcException
import kotlin.math.abs

/**
 * 数值求根：牛顿法为主、二分法兜底（设计文档 §4）。
 * 用于 IRR、CMPD 求 I%、债券 YLD 等无解析解场景（说明书：这些计算在真机上也是牛顿法近似）。
 * 失败抛 CalcException(MATH)，与真机报错一致。
 */
object Solver {

    /**
     * 求 f(x)=0 在 [lower, upper] 内的一个根。
     * 先以 [x0] 起跑牛顿法（中心差分导数）；出界、导数为 0、|f| 停滞或迭代超限则切换二分法（要求端点异号）。
     *
     * 注意：区间端点处 f 必须取有限值。含奇异点的函数（如 NPV 在 i=-1 发散）请把端点内缩（如 lower=-0.9999），
     * 否则二分兜底会在端点求值失败而误报求解失败。
     */
    fun solve(
        f: (Double) -> Double,
        x0: Double = 1.0,
        lower: Double = -1.0e9,
        upper: Double = 1.0e9,
        tol: Double = 1.0e-12,
        maxIterations: Int = 100
    ): Double {
        newton(f, x0.coerceIn(lower, upper), lower, upper, tol, maxIterations)?.let { return it }
        return bisection(f, lower, upper, tol, maxIterations)
    }

    private fun newton(
        f: (Double) -> Double,
        x0: Double,
        lower: Double,
        upper: Double,
        tol: Double,
        maxIter: Int
    ): Double? {
        var x = x0
        var best = Double.MAX_VALUE
        var stalls = 0
        repeat(maxIter) {
            val fx = f(x)
            if (!fx.isFinite()) return null
            val ax = abs(fx)
            if (ax <= tol) return x
            if (ax >= best - tol) {
                stalls++
                if (stalls >= 10) return null
            } else {
                stalls = 0
                best = ax
            }
            val h = 1e-7 * maxOf(1.0, abs(x))
            val d = (f(x + h) - f(x - h)) / (2 * h)
            if (!d.isFinite() || d == 0.0) return null
            val next = x - fx / d
            if (!next.isFinite() || next < lower || next > upper) return null
            if (abs(next - x) <= tol * maxOf(1.0, abs(next))) return next
            x = next
        }
        return null
    }

    private fun bisection(
        f: (Double) -> Double,
        lower: Double,
        upper: Double,
        tol: Double,
        maxIter: Int
    ): Double {
        var lo = lower
        var hi = upper
        var flo = f(lo)
        val fhi = f(hi)
        if (!flo.isFinite() || !fhi.isFinite()) fail("区间端点函数值非法")
        if (flo == 0.0) return lo
        if (fhi == 0.0) return hi
        if (flo * fhi > 0) fail("区间内无符号变化")
        repeat(maxIter) {
            val mid = (lo + hi) / 2
            val fm = f(mid)
            if (!fm.isFinite()) fail("函数值非法")
            if (abs(fm) <= tol || (hi - lo) / 2 <= tol * maxOf(1.0, abs(mid))) return mid
            if (flo * fm < 0) {
                hi = mid
            } else {
                lo = mid
                flo = fm
            }
        }
        fail("达到最大迭代次数仍未收敛")
    }

    private fun fail(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
```

- [ ] **Step 2: SolverTest.kt（先写测试）**

```kotlin
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
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.solver.SolverTest"
```

预期：`BUILD SUCCESSFUL`，8 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/ app/src/test/java/com/fincalc/app/core/
git commit -m "feat(core/solver): 牛顿法+二分法数值求根器"
```

---

### Task 7: 收尾验证（本计划验收点，无新文件、无提交）

- [ ] **Step 1: 全量单元测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest
```

预期：`BUILD SUCCESSFUL`；`app/build/test-results/testDebugUnitTest/` 下有 ContextTest/TokenizerTest/ParserTest/EvaluatorTest/LatexTest/SolverTest 及既有 SanityTest 的 XML 报告，failures/errors 均为 0。

- [ ] **Step 2: 验证 core 包为纯 Kotlin（无安卓依赖）**

```bash
grep -rn "import android" app/src/main/java/com/fincalc/app/core/ ; echo "grep exit=$?"
```

预期：无任何匹配行，`grep exit=1`。

- [ ] **Step 3: 仓库卫生检查**

```bash
git log --oneline -8
git status --short
```

预期：6 个 feat 提交依次在案；`git status --short` 为空。

---

## 完成标准（计划 2 验收）

- [ ] `./gradlew testDebugUnitTest` 全绿（新增 6 个测试类 + 既有 SanityTest）
- [ ] core 包无 `import android.*`，纯 JVM 可测
- [ ] 说明书例题断言全部通过：算术/百分比/Ans/多语句/三角/对数指数/幂根/Pol/Rec/阶乘/nPr/nCr/Rnd（计划 5 的 UI 直接复用这套引擎）
- [ ] 求解器收敛（牛顿）与兜底（二分）及失败路径（无符号变化、超迭代）均有测试覆盖
- [ ] 提交历史清晰（6 个 feat 提交），工作区干净
