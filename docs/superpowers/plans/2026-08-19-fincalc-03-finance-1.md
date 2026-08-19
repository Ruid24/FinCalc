# 计划 3：core/finance 金融引擎第一批（SMPL/CMPD/CASH/AMRT/CNVR/COST）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用纯 Kotlin 实现 FC-200V 前 6 个金融模式的公式引擎：SMPL（单利）、CMPD（复利/TVM）、CASH（现金流 NPV/IRR/NFV/PBP）、AMRT（摊销）、CNVR（APR⇄EFF）、COST（成本/售价/毛利）。全部 TDD，测试以说明书例题及其公式体系为权威验收标准。

**Architecture:** 新增包 `com.fincalc.app.core.finance`（源码 `app/src/main/java/`，测试 `app/src/test/java/`，JUnit4，`./gradlew testDebugUnitTest`）。依赖既有 `core/expr`（CalcException 错误模型）与 `core/solver`（Newton+二分求根，用于 CMPD 求 I% 与 CASH 求 IRR）。纯函数 API（设计文档 §4：公式引擎为纯函数）；core 包禁止 `import android.*`。

**Tech Stack:** Kotlin 2.0.20、JUnit 4.13.2（均已就位）。**本计划不引入任何新依赖，全程无需联网。**

**环境前提：** 每次新开 shell 先 `source .dev/env.sh`。

**权威依据（说明书 OCR 文本 `说明书/MinerU_markdown_FC-200V_100V_CN.md`，行号即该文件行号）：**

| 模式 | 公式章节 | 关键规则 |
|---|---|---|
| SMPL | CN-52（L1484-1514）：SI′ = Dys/365 或 360 × PV × i；SI = −SI′；SFV = −(PV+SI′) | Date Mode 360/365（CN-21，L411-424）；只能顺求 SI/SFV（L1446-1468） |
| CMPD | CN-57/58（L1632-1704）：PV/PMT/FV/n 四个解式 + I%=0 特例 + α/β/γ/i/S 定义（L1669） | Payment 期初/期末（CN-20）；P/Y、C/Y 1~9999 自然数（L5251）；dn 奇数期 CI/SI（CN-21，L426-437）；奇周期假定在首个完整付款期之前（L1616-1618）；I% 用牛顿法解 γPV+αPMT+βFV=0（L1672-1684） |
| CASH | CN-63/64（L1834-1868）：NPV、NFV=NPV×(1+i)ⁿ、IRR 牛顿法、PBP 含内插 | ≤80 项（L1814-1822）；全部同号 → Math ERROR、IRR ≤ −50 → Math ERROR（CN-168，L5253-5257）；IRR 结果写入共享变量 I（L1804，属 state 层，本计划不涉及） |
| AMRT | CN-69/70（L1999-2033）：INT/PRN/BAL/ΣINT/ΣPRN 递推公式 + Begin 特例（INT₁=0, PRN₁=PMT） | PM1、PM2：1~9999 整数且 PM1 < PM2（L5251）；与 CMPD 共享变量（L1923）；利率名义→实际转换同 CMPD（L2035-2047） |
| CNVR | CN-73（L2106-2120）：EFF=((1+APR/100/n)ⁿ−1)×100；APR=((1+EFF/100)^(1/n)−1)×n×100 | n = 年复利计算数；n ≤ 0 → Math ERROR（L5257，[OCR 存疑]处按卡西欧惯例归属） |
| COST | CN-75（L2184-2202）：CST=SEL×(1−MRG/100)；SEL=CST÷(1−MRG/100)；MRG=(1−CST/SEL)×100 | MRG 是毛利（占售价），不是加价率（由设定值表 40/100/60 与公式双重确认） |

**设计决策（说明书未覆盖处，本计划规定并声明）：**

1. **纯函数 API**：每个模式一个 object，入参为显式数值，返回值即结果。VARS 共享变量存储（n/I%/PV/PMT/FV 等跨模式共享）由后续 state 模块实现，引擎不持有状态。
2. **错误模型**：复用 `CalcException`。`MATH` 用于计算错误（除零、无解、IRR 同号/超限、n≤0、I%≤−100）；`ARGUMENT` 用于财务条件/设定不足（P/Y、C/Y、PM1/PM2 越界，Date Mode 非 360/365，现金流为空或 >80 项）——对应真机 Argument ERROR（L5331）。
3. **例题答案来源（重要）**：SMPL 与 CASH 的 NPV 答案为说明书原文精确值（SI=−164.3835616、SFV=−10164.38356、NPV=1400.464293）；CMPD 的 FV、AMRT 五量、CASH 的 IRR/PBP/NFV、CNVR 的 EFF 的答案在说明书中为**结果截图**，OCR 与 pdftotext 均无法提取（已实测，文本层只有 $16,760 这个表格近似值）。本计划的参考期望值是**用说明书公式对其例题输入做高精度计算的结果**（实现语言无关的 IEEE 双精度），其中 NPV 计算值 1400.4642931226808 与说明书文本值精确吻合，交叉验证了公式实现与数据。测试断言均给容差并在注释标明出处。
4. **AMRT 的 BAL₀ = PV 为工程推断**（说明书未明示）：由 PRN/BAL 递推公式与现金流量惯例确定，并经 ΣPRN(15→28) = BAL₁₄ − BAL₂₈ 自洽验证。
5. **CMPD 求 I%**：Solver 区间为 (−0.9999, 10)，避开 i=−1 奇异点（Solver KDoc 要求调用方内缩端点）；初值 0.05；i=0 处 α 取极限 Intg(n) 使残差函数在 0 邻域连续。结果 ≤ −100 → Math ERROR（CN-168）。
6. **CMPD 求 n**：直接采用说明书公式 n = log{((1+iS)PMT − FV·i)/((1+iS)PMT + PV·i)} / log(1+i)（dn 的奇数期差异不体现在该反解式中）；真数或分母非法 → Math ERROR。
7. **PBP 的 n 条件**：OCR 原文"NPVₙ ≡ 0，NPVₙ₊₁ ≡ 0"符号存疑，按公式语义与卡西欧惯例解读为"首个满足 NPVₙ ≤ 0 且 NPVₙ₊₁ ≥ 0 的非负整数 n"（含 n=0，即 CF₀<0 但首期即回收的情形）。NPV 永不变号 → Math ERROR（说明书未规定，本计划裁定）。
8. **已知留白**：VARS 共享存储、模式菜单、"7/=" 求解指示、定制快捷键、Date Mode/Payment/dn 的设置持久化，均属后续 state/UI 计划；本计划引擎按入参计算。

**参考期望值表（测试断言用；计算依据见各任务注释）：**

| 用例 | 输入 | 期望值 |
|---|---|---|
| SMPL 例1 | 365 基准，Dys=120，I%=5，PV=10000 | SI=−164.3835616；SFV=−10164.38356（说明书原文） |
| SMPL 360 基准 | 同上但 360 | SI=−166.6666667（=−120/360×10000×0.05） |
| CMPD 例1 | End，n=48，I%=4，PV=−1000，PMT=−300，P/Y=C/Y=12 | FV=16761.07896780279（说明书表格 ≈$16,760） |
| CMPD 例1 Begin 变体 | 同上但 Begin | FV=16813.03856879555 |
| CMPD 奇数期 n=48.5 | 同上（End），dn=CI / dn=SI | FV=16763.032672186913 / 16763.034298919418 |
| CMPD I%=0 | n=10，PV=−1000，PMT=−100 | FV=2000；反解 n=10、PMT=−100、PV=−1000 |
| CMPD P/Y≠C/Y | n=10，I%=6，PV=−1000，PMT=0，P/Y=1，C/Y=2 | i=0.0609；FV=1806.1112346694129 |
| CASH 例 | I%=3；CF=[−10000,−1000,4500,5000,4000] | NPV=1400.464293（说明书原文）；NFV=1576.2349；IRR=7.443619297%；PBP=3.605941275 |
| CNVR 例 | n=6，APR=3 | EFF=3.0377509393765045；回算 APR=3 |
| COST 例 | CST=40，SEL=100 | MRG=60；CST(SEL=100,MRG=60)=40；SEL(CST=40,MRG=60)=100 |
| AMRT 例 | End，PM1=15，PM2=28，I%=2，PV=100000，PMT=−920，P/Y=C/Y=12 | INT=−148.89718761877987；PRN=−771.1028123812201；BAL=78425.13934866441；ΣINT=−1966.8267773985376；ΣPRN=−10913.173222601463 |
| AMRT Begin（自构造） | Begin，PM1=1，PM2=2，I%=12，PV=1000，PMT=−100，P/Y=C/Y=1 | BAL=908；INT=0；PRN=−100；ΣINT=−108；ΣPRN=−92 |

**修订记录（执行期）：**

- 2026-08-19（Task 1 实现子代理回报）：SmplTest 的 SI/SFV 期望常量误用说明书 10 位显示值（−164.3835616 等），与 1e-9 断言容差不匹配（实际全精度 −164.3835616438356，差 4.4e-8）。已修正为全精度期望值并在注释中保留说明书 10 位显示值对照。教训：后续任务涉及"说明书 10 位显示值"的断言，一律用全精度参考值。
- 2026-08-19（Task 2 质量审查备注，均非阻塞未改码）：①IRR 超过 1000%（如 CF=[−1,20]）时报"区间内无符号变化" Math ERROR 而非静默截断，行为可接受。②潜伏边缘：现金流 ≥78 项且末项非零时 f(−0.9999) 可能因次正规下溢得 Inf，若牛顿法恰又失败转入二分会误报"端点函数值非法"；现实影响面极窄（NPV 函数光滑，牛顿极少失败），如需加固可将 lower 改为 −0.99。③Cnvr 未限制 n 为自然数（输入约束属 UI 层职责）。④Cnvr 在 APR < −100n 且 n 非整数时 pow 得 NaN 静默透出（极端输入，UI 层拦截）。
- 2026-08-19（Task 3 质量审查发现，N1-N3 已修复）：①I%=0 捷径曾绕过 n≤0 校验（`solvePMT(0,0,…)` 静默返回 −Infinity）；已把 n≤0 检查提前到各 solve* 入口。②solveN 对"投入多拿回少"场景曾按公式直译返回负 n；已加结果非有限或 ≤0 → Math ERROR（CN-168 n≤0 精神的延伸），同时覆盖极偏态 NaN（I%≈1e-14 且走 pow 分支时）。新增 2 个回归测试，CmpdTest 总数 16。备注（信息性未改码）：小数 n 且真利率恰为 0 时 solveI 不返回 0（i=0 极限与特例公式的固有张力，真机按说明书公式推演应同样表现）；主例 solveI 的牛顿法第 3 步越界、实际靠二分兜底收敛（设计内行为）。
- 2026-08-19（Task 4 质量审查备注，均非阻塞未改码）：①pm2 超过还清点（余额变负）后 INT/PRN 拆分不再满足恒等式——说明书未定义该域（AMRT 连 n 都不是输入），当前按原公式机械延续、不崩溃，可接受。②PMT=0 时 INT+PRN≠0，同属说明书未定义域。③测试缺口建议（后续加固）：9999 上界、i=0 零利率摊销断言、P/Y≠C/Y 期望值断言、PMT>0 收款场景。说明书例五量期望值已经"参考值 + 闭式自洽 + 付款合计恒等式"三重验证。
- 2026-08-19（计划编写期自查）：CmpdTest `solve i negative rate` 的 FV 期望值由错误的 800 修正为 1000×0.95¹⁰=598.7369392383787。

---

### Task 1: SMPL 单利 + COST 成本毛利

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Smpl.kt`
- Create: `app/src/main/java/com/fincalc/app/core/finance/Cost.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/SmplTest.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/CostTest.kt`

- [ ] **Step 1: Smpl.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/** SMPL 单利（说明书 CN-50~52）：由 Dys/I%/PV 顺求 SI 与 SFV（无反解）。 */
object Smpl {

    /**
     * 单利 SI = −SI′，SI′ = Dys/daysInYear × PV × i（i = I%/100，CN-52）。
     * [daysInYear]：Date Mode 设定的一年天数基准，仅 360 或 365（CN-21）。
     */
    fun si(dys: Double, iPercent: Double, pv: Double, daysInYear: Int): Double {
        checkDays(daysInYear)
        return -(dys / daysInYear) * pv * (iPercent / 100)
    }

    /** 简单终值 SFV = −(PV + SI′)。 */
    fun sfv(dys: Double, iPercent: Double, pv: Double, daysInYear: Int): Double {
        checkDays(daysInYear)
        val siPrime = (dys / daysInYear) * pv * (iPercent / 100)
        return -(pv + siPrime)
    }

    private fun checkDays(daysInYear: Int) {
        if (daysInYear != 360 && daysInYear != 365) {
            throw CalcException(CalcException.Kind.ARGUMENT, "Date Mode 仅支持 360 或 365")
        }
    }
}
```

- [ ] **Step 2: Cost.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/** COST 成本/售价/毛利（说明书 CN-74/75）。MRG 为毛利（占售价的百分比），非加价率。 */
object Cost {

    /** CST = SEL × (1 − MRG/100) */
    fun cst(sel: Double, mrg: Double): Double = sel * (1 - mrg / 100)

    /** SEL = CST ÷ (1 − MRG/100)。MRG=100 时除零 → Math ERROR。 */
    fun sel(cst: Double, mrg: Double): Double {
        val d = 1 - mrg / 100
        if (d == 0.0) throw CalcException(CalcException.Kind.MATH, "除以 0")
        return cst / d
    }

    /** MRG(%) = (1 − CST/SEL) × 100。SEL=0 时除零 → Math ERROR。 */
    fun mrg(cst: Double, sel: Double): Double {
        if (sel == 0.0) throw CalcException(CalcException.Kind.MATH, "除以 0")
        return (1 - cst / sel) * 100
    }
}
```

- [ ] **Step 3: SmplTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SmplTest {
    @Test
    fun `manual example 365 basis`() {
        // 说明书例1（L1424-1474）：Set=365，Dys=120，I%=5，PV=10000
        // 说明书 10 位显示 SI=-164.3835616、SFV=-10164.38356；此处按全精度期望值断言
        assertEquals(-164.3835616438356, Smpl.si(120.0, 5.0, 10000.0, 365), 1e-9)
        assertEquals(-10164.383561643835, Smpl.sfv(120.0, 5.0, 10000.0, 365), 1e-9)
    }

    @Test
    fun `basis 360`() {
        // SI′ = 120/360 × 10000 × 0.05 = 166.66…
        assertEquals(-166.66666666666666, Smpl.si(120.0, 5.0, 10000.0, 360), 1e-9)
        assertEquals(-10166.666666666666, Smpl.sfv(120.0, 5.0, 10000.0, 360), 1e-9)
    }

    @Test
    fun `sign convention for negative pv`() {
        // 借入（PV 为负）时符号翻转
        assertEquals(164.3835616438356, Smpl.si(120.0, 5.0, -10000.0, 365), 1e-9)
    }

    @Test
    fun `invalid days basis throws argument error`() {
        val e = assertThrows(CalcException::class.java) { Smpl.si(120.0, 5.0, 10000.0, 363) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
    }
}
```

- [ ] **Step 4: CostTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CostTest {
    @Test
    fun `manual example mrg`() {
        // 说明书设定值表（L2137）：CST=40，SEL=100 → MRG=60（占售价，非加价率）
        assertEquals(60.0, Cost.mrg(40.0, 100.0), 1e-12)
    }

    @Test
    fun `manual example reverse directions`() {
        assertEquals(40.0, Cost.cst(100.0, 60.0), 1e-12)
        assertEquals(100.0, Cost.sel(40.0, 60.0), 1e-12)
    }

    @Test
    fun `zero margin`() {
        assertEquals(0.0, Cost.mrg(50.0, 50.0), 0.0)
        assertEquals(50.0, Cost.sel(50.0, 0.0), 0.0)
    }

    @Test
    fun `math errors on division by zero`() {
        assertThrows(CalcException::class.java) { Cost.sel(40.0, 100.0) }
        assertThrows(CalcException::class.java) { Cost.mrg(40.0, 0.0) }
    }
}
```

- [ ] **Step 5: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.SmplTest" --tests "com.fincalc.app.core.finance.CostTest"
```

预期：`BUILD SUCCESSFUL`，共 8 个测试全过。

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): SMPL 单利与 COST 成本毛利"
```

---

### Task 2: CNVR 利率转换 + CASH 现金流

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Cnvr.kt`
- Create: `app/src/main/java/com/fincalc/app/core/finance/Cash.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/CnvrTest.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/CashTest.kt`

- [ ] **Step 1: Cnvr.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.pow

/** CNVR 名义利率(APR) ⇄ 实际利率(EFF)（说明书 CN-71~73）。n = 年复利计算数。 */
object Cnvr {

    /** EFF = ((1 + APR/100/n)^n − 1) × 100（CN-73）。n ≤ 0 → Math ERROR（CN-168）。 */
    fun eff(apr: Double, n: Double): Double {
        checkN(n)
        return ((1 + apr / 100 / n).pow(n) - 1) * 100
    }

    /** APR = ((1 + EFF/100)^(1/n) − 1) × n × 100（CN-73）。 */
    fun apr(eff: Double, n: Double): Double {
        checkN(n)
        return ((1 + eff / 100).pow(1 / n) - 1) * n * 100
    }

    private fun checkN(n: Double) {
        if (n <= 0) throw CalcException(CalcException.Kind.MATH, "n 必须为正数")
    }
}
```

- [ ] **Step 2: Cash.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.solver.Solver
import kotlin.math.pow

/** CASH 现金流（说明书 CN-60~64）：NPV、IRR、NFV、PBP。列表第 1 项为 CF₀。 */
object Cash {

    /** 最大数据项数 80（CF₀~CF₇₉，CN-63）。 */
    const val MAX_ITEMS = 80

    /** NPV = CF₀ + Σ CFₖ/(1+i)^k，i = I%/100（CN-63）。 */
    fun npv(iPercent: Double, cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        val i = iPercent / 100
        return cashFlows.foldIndexed(0.0) { k, acc, cf -> acc + cf / (1 + i).pow(k) }
    }

    /** NFV = NPV × (1+i)^n，n = 项数 − 1（CN-64）。 */
    fun nfv(iPercent: Double, cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        return npv(iPercent, cashFlows) * (1 + iPercent / 100).pow(cashFlows.size - 1)
    }

    /**
     * IRR（牛顿法，CN-63/64）：NPV = 0 的 i，返回百分比。
     * 全部现金流同号 → Math ERROR；结果 ≤ −50 → Math ERROR（CN-168）。
     */
    fun irr(cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        if (cashFlows.all { it >= 0 } || cashFlows.all { it <= 0 }) {
            throw CalcException(CalcException.Kind.MATH, "所有收入/付款值符号相同")
        }
        val i = Solver.solve(
            f = { r -> cashFlows.foldIndexed(0.0) { k, acc, cf -> acc + cf / (1 + r).pow(k) } },
            x0 = 0.1,
            lower = -0.9999,   // 避开 i=-1 奇异点（Solver KDoc 注意事项）
            upper = 10.0
        )
        val percent = i * 100
        if (percent <= -50) throw CalcException(CalcException.Kind.MATH, "IRR ≤ −50")
        return percent
    }

    /**
     * PBP（贴现回收期，CN-64，含线性内插；I=0 时为单回收期 SPP，CN-60）：
     * CF₀ ≥ 0 → 0；否则取首个满足 NPVₙ ≤ 0 ≤ NPVₙ₊₁ 的非负整数 n，
     * PBP = n − NPVₙ/(NPVₙ₊₁ − NPVₙ)。NPV 永不变号 → Math ERROR（本计划裁定）。
     */
    fun pbp(iPercent: Double, cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        if (cashFlows[0] >= 0) return 0.0
        val i = iPercent / 100
        var acc = 0.0
        var prev = 0.0
        for (k in cashFlows.indices) {
            acc += cashFlows[k] / (1 + i).pow(k)
            if (k > 0 && prev <= 0 && acc >= 0) {
                return (k - 1) - prev / (acc - prev)
            }
            prev = acc
        }
        throw CalcException(CalcException.Kind.MATH, "回收期不存在（NPV 未变号）")
    }

    private fun checkFlows(cashFlows: List<Double>) {
        if (cashFlows.isEmpty() || cashFlows.size > MAX_ITEMS) {
            throw CalcException(CalcException.Kind.ARGUMENT, "现金流项数须为 1 至 $MAX_ITEMS")
        }
    }
}
```

- [ ] **Step 3: CnvrTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CnvrTest {
    @Test
    fun `manual example apr to eff`() {
        // 说明书例1（L2066-2088）：n=6，APR=3 → EFF
        assertEquals(3.0377509393765045, Cnvr.eff(3.0, 6.0), 1e-9)
    }

    @Test
    fun `eff to apr round trip`() {
        // 说明书例2（L2090-2094）：EFF→APR 演示可逆性
        assertEquals(3.0, Cnvr.apr(3.0377509393765045, 6.0), 1e-9)
    }

    @Test
    fun `n equals one`() {
        assertEquals(5.0, Cnvr.eff(5.0, 1.0), 1e-12)
    }

    @Test
    fun `non positive n throws math error`() {
        assertThrows(CalcException::class.java) { Cnvr.eff(3.0, 0.0) }
        assertThrows(CalcException::class.java) { Cnvr.apr(3.0, -1.0) }
    }
}
```

- [ ] **Step 4: CashTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CashTest {
    private val cfs = listOf(-10000.0, -1000.0, 4500.0, 5000.0, 4000.0)

    @Test
    fun `manual example npv`() {
        // 说明书例1（L1748-1796）：I%=3 → NPV=1400.464293（原文精确值）
        assertEquals(1400.464293, Cash.npv(3.0, cfs), 1e-6)
    }

    @Test
    fun `manual example nfv`() {
        // NFV = NPV × 1.03^4（L1844-1848）
        assertEquals(1576.2349, Cash.nfv(3.0, cfs), 1e-6)
    }

    @Test
    fun `manual example irr`() {
        // 说明书例2（L1800-1804，答案为截图）：按公式体系参考值 7.443619297
        assertEquals(7.443619297, Cash.irr(cfs), 1e-6)
    }

    @Test
    fun `manual example pbp`() {
        // 说明书例3（L1806-1808，答案为截图）：NPV₃=−2153.48、NPV₄=+1400.46 → 3.605941275
        assertEquals(3.605941275, Cash.pbp(3.0, cfs), 1e-9)
    }

    @Test
    fun `irr writes percent scale`() {
        // IRR 返回百分比（7.44 而非 0.0744）
        val r = Cash.irr(cfs)
        assertEquals(7.443619297, r, 1e-6)
    }

    @Test
    fun `irr same sign throws math error`() {
        // CN-168：所有收入/付款值符号相同 → Math ERROR
        assertThrows(CalcException::class.java) { Cash.irr(listOf(1.0, 2.0, 3.0)) }
        assertThrows(CalcException::class.java) { Cash.irr(listOf(-1.0, -2.0)) }
    }

    @Test
    fun `irr result below minus 50 throws math error`() {
        // CF=[-1, 0.4]：IRR = -60% ≤ -50 → Math ERROR（CN-168）
        assertThrows(CalcException::class.java) { Cash.irr(listOf(-1.0, 0.4)) }
    }

    @Test
    fun `pbp zero when cf0 non negative`() {
        assertEquals(0.0, Cash.pbp(3.0, listOf(100.0, -50.0, -50.0)), 0.0)
    }

    @Test
    fun `pbp simple payback at zero rate`() {
        // I=0 时退化为单回收期（CN-60）：[-100, 30, 40, 50] → 2.6 年
        assertEquals(2.6, Cash.pbp(0.0, listOf(-100.0, 30.0, 40.0, 50.0)), 1e-12)
    }

    @Test
    fun `pbp never recovers throws math error`() {
        // I%=50 时例题现金流 NPV 恒为负
        assertThrows(CalcException::class.java) { Cash.pbp(50.0, cfs) }
    }

    @Test
    fun `item count validation`() {
        assertThrows(CalcException::class.java) { Cash.npv(3.0, emptyList()) }
        assertThrows(CalcException::class.java) { Cash.npv(3.0, List(81) { 1.0 }) }
    }

    @Test
    fun `max 80 items accepted`() {
        // 80 项合法（CF₀~CF₇₉）
        val flows = List(80) { if (it == 0) -1.0 else 1.0 }
        Cash.npv(3.0, flows)
    }
}
```

- [ ] **Step 5: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.CnvrTest" --tests "com.fincalc.app.core.finance.CashTest"
```

预期：`BUILD SUCCESSFUL`，共 16 个测试全过。

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): CNVR 利率转换与 CASH 现金流（NPV/IRR/NFV/PBP）"
```

---

### Task 3: CMPD 复利（TVM 任求其一）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Cmpd.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/CmpdTest.kt`

- [ ] **Step 1: Cmpd.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.solver.Solver
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * CMPD 复利（说明书 CN-53~58）：n、I%、PV、PMT、FV 任求其一。
 * 前置设置：Payment（期初/期末，CN-20）、P/Y（年付款数）、C/Y（年复利数）、
 * dn（奇数期利息算法 CI/SI，CN-21；奇周期假定在首个完整付款期之前，CN-56）。
 */
object Cmpd {

    enum class Payment { BEGIN, END }
    enum class OddPeriod { CI, SI }

    /**
     * 每期实际利率 i（CN-57）：P/Y=C/Y=1 时 i = I%/100；
     * 否则 i = (1 + I%/(100·C/Y))^(C/Y÷P/Y) − 1。I% ≤ −100 → Math ERROR（CN-168）。
     */
    fun periodRate(iPercent: Double, py: Int, cy: Int): Double {
        if (iPercent <= -100) mathErr("I% ≤ −100")
        checkPyCy(py, cy)
        return if (py == 1 && cy == 1) {
            iPercent / 100
        } else {
            (1 + iPercent / (100.0 * cy)).pow(cy.toDouble() / py) - 1
        }
    }

    private class Coeffs(val alpha: Double, val beta: Double, val gamma: Double)

    /**
     * α/β/γ 系数（CN-57/58 L1669）。n ≤ 0 → Math ERROR（CN-168）。
     * i=0 时 α 取极限 Intg(n)（供求 I% 的残差函数在 0 邻域连续）。
     */
    private fun coeffs(n: Double, i: Double, payment: Payment, dn: OddPeriod): Coeffs {
        if (n <= 0) mathErr("n ≤ 0")
        val s = if (payment == Payment.BEGIN) 1.0 else 0.0
        val intg = floor(n)
        val frac = n - intg
        val beta = (1 + i).pow(-intg)
        val gamma = if (dn == OddPeriod.CI) (1 + i).pow(frac) else 1 + i * frac
        val alpha = if (i != 0.0) (1 + i * s) * (1 - beta) / i else intg
        return Coeffs(alpha, beta, gamma)
    }

    /** 求 PV（CN-57）：PV = (−α·PMT − β·FV)/γ；I%=0 时 PV = −(PMT×n + FV)（CN-58）。n ≤ 0 → Math ERROR（CN-168）。 */
    fun solvePV(
        n: Double, iPercent: Double, pmt: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (n <= 0) mathErr("n ≤ 0")
        if (iPercent == 0.0) return -(pmt * n + fv)
        val c = coeffs(n, periodRate(iPercent, py, cy), payment, dn)
        return (-c.alpha * pmt - c.beta * fv) / c.gamma
    }

    /** 求 PMT（CN-57）：PMT = (−γ·PV − β·FV)/α；I%=0 时 PMT = −(PV+FV)/n。n ≤ 0 → Math ERROR。 */
    fun solvePMT(
        n: Double, iPercent: Double, pv: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (n <= 0) mathErr("n ≤ 0")
        if (iPercent == 0.0) return -(pv + fv) / n
        val c = coeffs(n, periodRate(iPercent, py, cy), payment, dn)
        return (-c.gamma * pv - c.beta * fv) / c.alpha
    }

    /** 求 FV（CN-57）：FV = (−γ·PV − α·PMT)/β；I%=0 时 FV = −(PMT×n + PV)。n ≤ 0 → Math ERROR。 */
    fun solveFV(
        n: Double, iPercent: Double, pv: Double, pmt: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (n <= 0) mathErr("n ≤ 0")
        if (iPercent == 0.0) return -(pmt * n + pv)
        val c = coeffs(n, periodRate(iPercent, py, cy), payment, dn)
        return (-c.gamma * pv - c.alpha * pmt) / c.beta
    }

    /**
     * 求 n（CN-57）：n = log{((1+iS)PMT − FV·i) / ((1+iS)PMT + PV·i)} / log(1+i)。
     * I%=0 时 n = −(PV+FV)/PMT。真数或分母非法、结果非有限或不为正 → Math ERROR。
     */
    fun solveN(
        iPercent: Double, pv: Double, pmt: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (iPercent == 0.0) {
            if (pmt == 0.0) mathErr("除以 0")
            val n0 = -(pv + fv) / pmt
            if (!n0.isFinite() || n0 <= 0) mathErr("n 无解")
            return n0
        }
        val i = periodRate(iPercent, py, cy)
        val s = if (payment == Payment.BEGIN) 1.0 else 0.0
        val num = (1 + i * s) * pmt - fv * i
        val den = (1 + i * s) * pmt + pv * i
        if (den == 0.0 || num / den <= 0) mathErr("n 无解")
        val result = ln(num / den) / ln(1 + i)
        if (!result.isFinite() || result <= 0) mathErr("n 无解")
        return result
    }

    /**
     * 求 I%（牛顿法，CN-58）：解 γ×PV + α×PMT + β×FV = 0，再换算回名义利率。
     * 说明书警告（L1700-1702）：近似值，精度受计算条件影响。结果 ≤ −100 → Math ERROR（CN-168）。
     */
    fun solveI(
        n: Double, pv: Double, pmt: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        checkPyCy(py, cy)
        val i = Solver.solve(
            f = { r ->
                val c = coeffs(n, r, payment, dn)
                c.gamma * pv + c.alpha * pmt + c.beta * fv
            },
            x0 = 0.05,
            lower = -0.9999,   // 避开 i=-1 奇异点（Solver KDoc 注意事项）
            upper = 10.0
        )
        val percent = if (py == 1 && cy == 1) {
            i * 100
        } else {
            ((1 + i).pow(py.toDouble() / cy) - 1) * cy * 100
        }
        if (percent <= -100) mathErr("I% ≤ −100")
        return percent
    }

    private fun checkPyCy(py: Int, cy: Int) {
        if (py < 1 || py > 9999 || cy < 1 || cy > 9999) {
            throw CalcException(CalcException.Kind.ARGUMENT, "P/Y、C/Y 须为 1 至 9999 的自然数")
        }
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
```

- [ ] **Step 2: CmpdTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CmpdTest {
    private val end = Cmpd.Payment.END
    private val bgn = Cmpd.Payment.BEGIN
    private val ci = Cmpd.OddPeriod.CI
    private val si = Cmpd.OddPeriod.SI

    // ---- 说明书例1（L1545-1583）：End，n=48，I%=4，PV=−1000，PMT=−300，P/Y=C/Y=12 ----

    @Test
    fun `manual example solve fv`() {
        // 期望值按说明书公式体系高精度计算（答案在说明书为截图；表格近似值 $16,760）
        assertEquals(
            16761.07896780279,
            Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci),
            1e-4
        )
    }

    @Test
    fun `manual example begin variant`() {
        // 同例改为期初付款（说明书无 Begin 例题，按公式体系参考值）
        assertEquals(
            16813.03856879555,
            Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, bgn, ci),
            1e-4
        )
    }

    @Test
    fun `odd period dn ci vs si`() {
        // n=48.5（16 个月 20 天式输入，CN-56）：dn=CI 与 dn=SI 结果不同
        assertEquals(
            16763.032672186913,
            Cmpd.solveFV(48.5, 4.0, -1000.0, -300.0, 12, 12, end, ci),
            1e-4
        )
        assertEquals(
            16763.034298919418,
            Cmpd.solveFV(48.5, 4.0, -1000.0, -300.0, 12, 12, end, si),
            1e-4
        )
    }

    @Test
    fun `zero interest special case`() {
        // CN-58 特例：PV=−(PMT×n+FV)；PMT=−(PV+FV)/n；FV=−(PMT×n+PV)；n=−(PV+FV)/PMT
        assertEquals(2000.0, Cmpd.solveFV(10.0, 0.0, -1000.0, -100.0, 1, 1, end, ci), 1e-12)
        assertEquals(-100.0, Cmpd.solvePMT(10.0, 0.0, -1000.0, 2000.0, 1, 1, end, ci), 1e-12)
        assertEquals(-1000.0, Cmpd.solvePV(10.0, 0.0, -100.0, 2000.0, 1, 1, end, ci), 1e-12)
        assertEquals(10.0, Cmpd.solveN(0.0, -1000.0, -100.0, 2000.0, 1, 1, end, ci), 1e-12)
    }

    @Test
    fun `py differs from cy`() {
        // P/Y=1、C/Y=2（年付、半年复利）：i = (1+0.06/2)^2 − 1 = 0.0609
        assertEquals(0.0609, Cmpd.periodRate(6.0, 1, 2), 1e-15)
        assertEquals(
            1806.1112346694129,
            Cmpd.solveFV(10.0, 6.0, -1000.0, 0.0, 1, 2, end, ci),
            1e-6
        )
    }

    // ---- 往返测试（说明书无此类例题：先顺求 FV，再以其反解其余四变量） ----

    @Test
    fun `solve i round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        assertEquals(4.0, Cmpd.solveI(48.0, -1000.0, -300.0, fv, 12, 12, end, ci), 1e-6)
    }

    @Test
    fun `solve n round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        assertEquals(48.0, Cmpd.solveN(4.0, -1000.0, -300.0, fv, 12, 12, end, ci), 1e-9)
    }

    @Test
    fun `solve pv pmt round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        assertEquals(-1000.0, Cmpd.solvePV(48.0, 4.0, -300.0, fv, 12, 12, end, ci), 1e-6)
        assertEquals(-300.0, Cmpd.solvePMT(48.0, 4.0, -1000.0, fv, 12, 12, end, ci), 1e-6)
    }

    @Test
    fun `solve i begin round trip`() {
        val fv = Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 12, 12, bgn, ci)
        assertEquals(4.0, Cmpd.solveI(48.0, -1000.0, -300.0, fv, 12, 12, bgn, ci), 1e-6)
    }

    @Test
    fun `solve i negative rate`() {
        // 亏损场景：PV=−1000，PMT=0，I%=−5，n=10 → FV = 1000×0.95^10，再反解利率
        val fv = Cmpd.solveFV(10.0, -5.0, -1000.0, 0.0, 1, 1, end, ci)
        assertEquals(598.7369392383787, fv, 1e-9)
        assertEquals(-5.0, Cmpd.solveI(10.0, -1000.0, 0.0, fv, 1, 1, end, ci), 1e-9)
    }

    // ---- 错误条件（CN-168 与范围表） ----

    @Test
    fun `i percent at or below minus 100 throws math error`() {
        assertThrows(CalcException::class.java) {
            Cmpd.solveFV(48.0, -100.0, -1000.0, -300.0, 12, 12, end, ci)
        }
        assertThrows(CalcException::class.java) {
            Cmpd.periodRate(-101.0, 12, 12)
        }
    }

    @Test
    fun `non positive n throws math error`() {
        assertThrows(CalcException::class.java) {
            Cmpd.solveFV(0.0, 4.0, -1000.0, -300.0, 12, 12, end, ci)
        }
    }

    @Test
    fun `solve i with all same sign throws math error`() {
        // 残差 γ·PV+α·PMT+β·FV 恒为负（系数恒正），无解 → Math ERROR
        assertThrows(CalcException::class.java) {
            Cmpd.solveI(48.0, -1000.0, -300.0, -100.0, 12, 12, end, ci)
        }
    }

    @Test
    fun `invalid py cy throws argument error`() {
        val e = assertThrows(CalcException::class.java) {
            Cmpd.solveFV(48.0, 4.0, -1000.0, -300.0, 0, 12, end, ci)
        }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
        assertThrows(CalcException::class.java) {
            Cmpd.periodRate(4.0, 12, 10000)
        }
    }

    @Test
    fun `zero interest with non positive n throws math error`() {
        // 审查发现：I%=0 捷径曾绕过 n≤0 校验，静默返回 −Infinity
        assertThrows(CalcException::class.java) {
            Cmpd.solvePMT(0.0, 0.0, -1000.0, 2000.0, 1, 1, end, ci)
        }
        assertThrows(CalcException::class.java) {
            Cmpd.solvePV(-5.0, 0.0, -100.0, 2000.0, 1, 1, end, ci)
        }
    }

    @Test
    fun `solve n non positive result throws math error`() {
        // 审查发现：PV=−1000、PMT=−100、FV=+500、I%=4 场景公式直译得负 n（约 −4.89），
        // 正利率下投入多拿回少属无解除 → Math ERROR（CN-168 n≤0 精神的延伸）
        assertThrows(CalcException::class.java) {
            Cmpd.solveN(4.0, -1000.0, -100.0, 500.0, 12, 12, end, ci)
        }
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.CmpdTest"
```

预期：`BUILD SUCCESSFUL`，14 个测试全过。若 solveI 的牛顿法在个别用例不收敛而落入二分兜底，属设计内行为（Solver 自动处理），仅当最终仍失败才需要回报，不得改实现逻辑凑测试。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): CMPD 复利 TVM 任求其一（含牛顿法求 I%）"
```

---

### Task 4: AMRT 年限摊销

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Amrt.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/AmrtTest.kt`

- [ ] **Step 1: Amrt.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.abs

/**
 * AMRT 年限摊销（说明书 CN-65~70）。
 * BAL：PM2 付款完毕时的本金余额；INT/PRN：PM1 那笔的利息/本金部分；
 * ΣINT/ΣPRN：PM1 至 PM2 的合计。变量 n、I%、PV、PMT、FV、P/Y、C/Y 与 CMPD 共享（CN-67）。
 */
object Amrt {

    data class Result(
        val bal: Double,
        val int: Double,
        val prn: Double,
        val sumInt: Double,
        val sumPrn: Double
    )

    /**
     * 递推（CN-69/70）：BAL₀ = PV；
     * INTⱼ = |BALⱼ₋₁ × i| × (PMT 符号)；PRNⱼ = PMT + BALⱼ₋₁ × i；BALⱼ = BALⱼ₋₁ + PRNⱼ。
     * 期初（Payment.BEGIN）特例：INT₁ = 0、PRN₁ = PMT。
     * 利率与 CMPD 相同：名义利率 → 每期实际利率（CN-70）。
     */
    fun amortize(
        pm1: Int,
        pm2: Int,
        iPercent: Double,
        pv: Double,
        pmt: Double,
        py: Int,
        cy: Int,
        payment: Cmpd.Payment
    ): Result {
        if (pm1 < 1 || pm2 < 1 || pm1 > 9999 || pm2 > 9999 || pm1 >= pm2) {
            throw CalcException(CalcException.Kind.ARGUMENT, "PM1、PM2 须为 1 至 9999 的整数且 PM1 < PM2")
        }
        val i = Cmpd.periodRate(iPercent, py, cy)
        val pmtSign = if (pmt >= 0) 1.0 else -1.0
        var bal = pv
        var intPm1 = 0.0
        var prnPm1 = 0.0
        var sumInt = 0.0
        var sumPrn = 0.0
        for (j in 1..pm2) {
            val intJ: Double
            val prnJ: Double
            if (j == 1 && payment == Cmpd.Payment.BEGIN) {
                intJ = 0.0
                prnJ = pmt
            } else {
                intJ = abs(bal * i) * pmtSign
                prnJ = pmt + bal * i
            }
            bal += prnJ
            if (j == pm1) {
                intPm1 = intJ
                prnPm1 = prnJ
            }
            if (j >= pm1) {
                sumInt += intJ
                sumPrn += prnJ
            }
        }
        return Result(bal, intPm1, prnPm1, sumInt, sumPrn)
    }
}
```

- [ ] **Step 2: AmrtTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AmrtTest {

    // ---- 说明书例（L1929-1989）：End，PM1=15，PM2=28，I%=2，PV=100000，PMT=−920，P/Y=C/Y=12 ----
    // 五个答案在说明书中为截图；期望值为按 CN-69/70 公式体系的高精度计算参考值
    // （BAL₀=PV 为工程推断，已经 ΣPRN = BAL₁₄−BAL₂₈ 自洽验证）

    private val r = Amrt.amortize(15, 28, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)

    @Test
    fun `manual example bal`() {
        assertEquals(78425.13934866441, r.bal, 1e-6)
    }

    @Test
    fun `manual example int and prn`() {
        assertEquals(-148.89718761877987, r.int, 1e-9)
        assertEquals(-771.1028123812201, r.prn, 1e-9)
    }

    @Test
    fun `manual example sums`() {
        assertEquals(-1966.8267773985376, r.sumInt, 1e-6)
        assertEquals(-10913.173222601463, r.sumPrn, 1e-6)
    }

    @Test
    fun `sums are consistent with payment count`() {
        // 每期付款 = 利息 + 本金：ΣINT + ΣPRN = PMT × 期数
        assertEquals(-920.0 * 14, r.sumInt + r.sumPrn, 1e-6)
    }

    @Test
    fun `begin payment first period special case`() {
        // CN-70：Begin 时 INT₁ = 0、PRN₁ = PMT（自构造用例）
        val b = Amrt.amortize(1, 2, 12.0, 1000.0, -100.0, 1, 1, Cmpd.Payment.BEGIN)
        assertEquals(0.0, b.int, 0.0)
        assertEquals(-100.0, b.prn, 0.0)
        assertEquals(908.0, b.bal, 1e-12)
        assertEquals(-108.0, b.sumInt, 1e-12)
        assertEquals(-92.0, b.sumPrn, 1e-12)
    }

    @Test
    fun `py differs from cy rate conversion`() {
        // CN-70：P/Y≠C/Y 时先做名义→实际转换（与 CMPD 同一公式），此处只验证通路不报错
        Amrt.amortize(1, 2, 6.0, 1000.0, -100.0, 12, 2, Cmpd.Payment.END)
    }

    @Test
    fun `invalid pm range throws argument error`() {
        assertThrows(CalcException::class.java) {
            Amrt.amortize(28, 15, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)
        }
        assertThrows(CalcException::class.java) {
            Amrt.amortize(15, 15, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)
        }
        assertThrows(CalcException::class.java) {
            Amrt.amortize(0, 15, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)
        }
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.AmrtTest"
```

预期：`BUILD SUCCESSFUL`，7 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): AMRT 年限摊销五量递推"
```

---

### Task 5: 收尾验证（本计划验收点，无新文件、无提交）

- [ ] **Step 1: 全量单元测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest
```

预期：`BUILD SUCCESSFUL`；finance 包 5 个测试类（SmplTest/CostTest/CnvrTest/CashTest/CmpdTest/AmrtTest）与既有 expr/solver/SanityTest 全部 0 失败 0 错误。

- [ ] **Step 2: 验证 core 包为纯 Kotlin（无安卓依赖）**

```bash
grep -rn "import android" app/src/main/java/com/fincalc/app/core/ ; echo "grep exit=$?"
```

预期：无任何匹配行，`grep exit=1`。

- [ ] **Step 3: 仓库卫生检查**

```bash
git log --oneline -6
git status --short
```

预期：4 个 feat(core/finance) 提交依次在案；`git status --short` 为空。

---

## 完成标准（计划 3 验收）

- [ ] `./gradlew testDebugUnitTest` 全绿（新增 finance 6 个测试类 + 计划 2 全部既有测试）
- [ ] core 包无 `import android.*`，纯 JVM 可测
- [ ] 说明书例题验收：SMPL（SI/SFV 原文值）、CMPD 例1 FV、CASH（NPV 原文值 + IRR/PBP/NFV 参考值）、CNVR（EFF 参考值 + 可逆性）、COST（三方向）、AMRT（五量参考值 + 付款合计自洽）全部通过
- [ ] CMPD 五变量任解往返一致（含 Begin、I%=0、P/Y≠C/Y、奇数期 dn CI/SI、负利率）
- [ ] 错误条件覆盖：I%≤−100、n≤0、P/Y-C/Y 越界（ARGUMENT）、PM1≥PM2（ARGUMENT）、现金流同号/超 80 项、IRR≤−50、除零
- [ ] 提交历史清晰（4 个 feat 提交），工作区干净
