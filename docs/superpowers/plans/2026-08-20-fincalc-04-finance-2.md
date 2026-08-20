# 计划 4：core/finance 金融引擎第二批（DAYS/DEPR/BOND/BEVN/STAT）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用纯 Kotlin 实现 FC-200V 后 6 个计算模式（其中 5 个金融模式 + STAT 统计）：DAYS（天数计算）、DEPR（折旧四法）、BOND（债券 PRC/YLD/INT/CST）、BEVN（损益分析 6 子模式）、STAT（单变量统计 + 7 种回归）。全部 TDD，测试以说明书例题及其公式体系为权威验收标准。完成后 12 模式引擎全部就绪。

**Architecture:** 沿用 `com.fincalc.app.core.finance` 包（源码 `app/src/main/java/`，测试 `app/src/test/java/`，JUnit4）。依赖既有 `core/expr`（CalcException）与 `core/solver`（Solver，用于 BOND 求 YLD）。纯函数 API；core 包禁止 `import android.*`。

**Tech Stack:** Kotlin 2.0.20、JUnit 4.13.2（均已就位）。**本计划不引入任何新依赖，全程无需联网。**

**环境前提：** 每次新开 shell 先 `source .dev/env.sh`。

**权威依据（说明书 OCR 文本 `说明书/MinerU_markdown_FC-200V_100V_CN.md`，行号即该文件行号）：**

| 模式 | 公式/规则章节 | 关键规则 |
|---|---|---|
| DAYS | 无独立公式章；日期范围与 31 日规则在 CN-77（L2204-2293） | d1/d2/Dys 知二求一（L2233）；范围 1901-01-01~2099-12-31（L5251）；Date Mode 360/365（L411）；360 的 31 日规则：d1 为 31 日→当月 30 日，d2 为 31 日→次月 1 日（L2225）；Date Input MDY/DMY（L469-486，无 YMD） |
| DEPR | CN-82~84（L2383-2529）：SL/FP/SYD/DB 四组公式（含 YR1≠12 月折算与第 n+1 年规则） | I%：FP 为折旧比、DB 为因子（200=DDB，L2320）；只有 FP/DB 用 I%（L2335）；RDV 为"PV−FV 口径剩余可折旧值"；错误：PV/FV/I% 负 → MATH，n>255 → MATH，j>n+1（YR1≠12）→ MATH，YR1>12 → ARGUMENT（L5259）；j 正自然数、YR1 1~12（L5251） |
| BOND | CN-88~90（L2674-2729）：PRC 两组公式（Date 分长短票息期、Term）+ INT/CST + YLD 牛顿法 | Bond Date：Date/Term（L454-467）；Periods/Y：Annual=1/Semi=2（L439-452）；日计数随 Date Mode（L2576-2578）；Term 前置要求 Date Mode=360 且 Annual（L2638，属设置层）；d1∈1902-01-01~2097-12-30、d2∈1902-01-02~2097-12-31（L5251）；PRC 要求 RDV>0、CPN≥0，YLD 要求 RDV>0、PRC<0（L5261，OCR ≡ 号存疑按惯例解读） |
| BEVN | CN-94~110（L2882-3346）：BEV/MOS/DOL/DFL/DCL/QTY CONV 公式 | BEV 受 PRF/Ratio（L488-498）与 B-Even（L502-520）两设置影响；BEV 五变量任解（L2872-2874）；QTY CONV 两组 QTY 联动（L3284、L3316）；无专属错误条目，分母为 0 → 通用 MATH |
| STAT | CN-137~160（L4171-5166）：1-VAR 公式 + 7 回归模型与系数公式 + x̂/ŷ | 回归清单（L4002）：1-VAR、A+BX、_+CX²、ln X、e^X、A•B^X、A•X^B、1/X；FREQ 频率加权（L4034-4038）；行数上限 80/40/26（L4073，UI 层职责）；二次回归无 r（L4471） |

**设计决策（说明书未覆盖处，本计划规定并声明）：**

1. **日期表示**：引擎内部用 `Days.Date(year, month, day)` 数据类；紧凑输入数字（MDY `11052022` / DMY `05112022`）与 Date 的互转由引擎提供（`parse`/`format`），键盘层（计划 5）只传数字。
2. **DAYS 的 360 模式**：`daysBetween` 按 30/360（含 31 日规则）；**日期±天数一律按实际历法（JDN）加减**——说明书未给 360 模式的日期加减公式，例题（例 2/3）明确要求先切 365（L2271-2273），本计划按此裁定。
3. **BOND 票息日程**：票息日与赎回日 d2 同月日、按 Periods/Y 回推；月日钳制到当月长度（如 2 月 30→28，modified-following 惯例，说明书未明）；d1<d2 为输入前提（违反 → ARGUMENT，说明书未明，本计划裁定）。D 按"结算日所在票息期"的天数（365 模式为实际天数；360 模式为 30/360 天数）。
4. **DEPR 的 YR1=12 时 j 上限为 n**（说明书只给 YR1≠12 时 j≤n+1；对称推论）；SL 的 RDV 说明书未给公式，按 RDVⱼ = PV−FV−ΣSL（与其他三法同构，例题屏幕确有 RDV 显示）。
5. **STAT 全部加权和**（FREQ 适用 2-VAR 同理）；统计量计算：标准差按说明书定义式（两遍法，先均值后偏差平方和）；回归系数按说明书计算式（单遍和形式）。二次回归 `RegResult.c` 承载 C，其余回归 c=null；二次回归无 r（r=null）。
6. **x̂/ŷ 估计**：`estimateY` 全模式支持；`estimateX` 支持非二次模式；二次回归单独 `estimateXQuadratic` 返回 (x̂1, x̂2)。定义域违规（ln 非正、B=0、判别式<0、x=0 取倒数等）→ MATH。
7. **BEVN 反解**：由说明书正向公式代数反解（全部线性/分式，无迭代）；分母为 0 → MATH。SBE 侧反解先换算 QBE=SBE/PRC 再套用（文档化）。
8. **已知留白**：VARS 共享存储、设置持久化（Date Mode/Date Input/Bond Date/Periods/Y/PRF-Ratio/B-Even/STAT FREQ）、数据编辑器行数上限、模式菜单——均属计划 5（state/UI）。

**修订记录（编写期自查）：**

- 2026-08-20：①STAT 初稿用 `Entry1`/`Entry2` 两个数据类导致 `count(List<Entry1>)` 与 `count(List<Entry2>)` 等 10 个函数 JVM 泛型签名冲突（编译错误）——已改为单一 `Entry(x, y?=null, freq=1)` 类型（2-VAR 专属函数经 `check2` 要求 y 非空），测试构造器相应更新。②DaysTest 的 JDN 往返测试初稿首迭代越界（1901-01-01 减 100 天）——已改为保持在范围内。③DaysTest 的 31 日规则跨年断言初稿数值错误（61→421）——已修正。④BOND 半年付测试初稿 N=5 系笔误（实际 6），期望值已按公式体系算出（PRC=−97.60774696391445）。
- 编写期自查方法：全部参考值用 Python 按说明书公式独立计算；SMPL/DAYS 365/CASH NPV/BOND Date PRC 与说明书原文精确吻合（交叉验证公式实现正确性）。

**参考期望值表（测试断言用；说明书原文值标"原文"，截图缺失的标"参考值"=按说明书公式高精度计算，全部经独立复算验证）：**

| 用例 | 输入 | 期望值 |
|---|---|---|
| DAYS 例1（365） | d1=2022-11-05，d2=2023-04-27 | Dys=173（原文） |
| DAYS 同例 360 | 同上 | Dys=172（参考值，30/360） |
| DAYS 31 日规则（360） | d1=2022-01-31，d2=2022-03-31 | Dys=61（d1→30，d2→4/1：360×0+30×(4−1)+(1−30)） |
| DAYS 例2/例3（365） | d1=2022-11-05，Dys=173 → d2；d2=2023-04-27，Dys=173 → d1 | d2=2023-04-27；d1=2022-11-05（原文流程，逆运算） |
| DEPR 例1 SL | n=6，PV=150000，FV=0，j=3，YR1=2 | SL₃=25000（原文）；RDV₃=95833.33333（参考值；OCR 误作 95855 已证伪） |
| DEPR 例2 FP | 同上 + I%=25 | FP₃=26953.125；RDV₃=80859.375（参考值） |
| DEPR 例3 SYD | 同上（I% 不用） | SYD₁=7142.857143；SYD₃=34523.809524；RDV₃=66666.666667（参考值） |
| DEPR 例4 DB(DDB) | 同上 + I%=200 | DB₁=8333.333333；DB₃=31481.481481；RDV₃=62962.962963（参考值） |
| BOND 例1（Date） | Date，365，Annual，d1=2022-06-01，d2=2024-12-15，RDV=100，CPN=3，YLD=4 | A=168、D=365、B=197、N=3；PRC=−97.6151555（原文设定值表）−精确 97.61515550118818；INT=−1.3808219178082193；CST=−98.9959774189964（参考值） |
| BOND 例2（Date YLD） | 同上输入 PRC=−97.6151555 | YLD=4.0（往返，参考值 4.0000000005） |
| BOND 例3（Term） | Term，n=3，RDV=100，CPN=3，YLD=4 | PRC=−97.22490896677286（参考值） |
| BOND 例4（Term YLD） | 同 n=3，PRC=−97.6151555 | YLD=3.857044586（参考值，Term 口径） |
| BEVN 例 | PRC=100、VCU=50、FC=100000 | PRF=0：QBE=2000、SBE=200000；PRF=400000：QBE=10000、SBE=1000000；r%=40：QBE=10000、SBE=1000000（均原文设定值表） |
| BEVN MOS/DOL/DFL/DCL 例 | SAL=1200000、SBE=1000000 / VC=600000、FC=200000 / EIT=400000、ITR=80000 / +ITR=100000 | MOS=1/6≈0.16667；DOL=1.5；DFL=1.25；DCL=2（均原文设定值表） |
| BEVN QTY CONV 例 | SAL=100000、PRC=200 / VC=15000、VCU=30 | QTY=500（两向，原文） |
| STAT 例4-6（1-VAR FREQ） | (0,1),(1,2),(2,1),(3,2),(4,2),(5,2),(6,3),(7,4),(9,2),(10,1) | n=20、Σx=102、Σx²=672、x̄=5.1、xσn=2.754995463、minX=0、maxX=10（原文）；xσn−1=2.826565705（参考值） |
| STAT 例7-10（线性） | x=[1.0..3.0]、y=[1.0..2.0]（10 对，见测试代码） | A=0.5043587805492551、B=0.4802217183（原文）、r=0.9952824846（原文）、x̂(−3)=−7.297376705（原文）、ŷ(2)=1.464802217（原文） |
| STAT 例11-13（二次） | 同上数据 | A=0.7028598638（原文）、B=0.25763843788924545、C=0.05610274152791289（参考值）、x̂1(3)=4.502211457（原文）、x̂2(3)=−9.094472563（原文）、ŷ(2)=1.442547706（原文） |
| STAT 例15（对数） | (29,1.6),(50,23.5),(74,38.0),(103,46.4),(118,48.9) | A=−111.1283976473655、B=34.02014750160489、r=0.994013946616563、ŷ(80)=37.94879482020123、x̂(73)=224.15413126072139（全参考值） |
| STAT 例16（e 指数） | (6.9,21.4),(12.9,15.7),(19.8,12.1),(26.7,8.5),(35.1,5.2) | A=30.49758743（原文）、B=−0.04920370830766393、r=−0.997247352（原文）、ŷ(16)=13.87915739（原文）、x̂(20)=8.574868047（原文） |
| STAT 例17（ab 指数） | (−1,0.24),(3,4),(5,16.2),(10,513) | A=0.48886664（原文）、B=2.0074993437791706、r=0.9999873551795408、x̂(1.02)=1.055357865（原文）、ŷ(15)=16944.2200173692（参考值） |
| STAT 例18（幂） | (28,2410),(30,3033),(33,3895),(35,4491),(38,5717) | A=0.23880106853373598、B=2.771866158（原文）、r=0.9989062551（原文）、ŷ(40)=6587.674589（原文）、x̂(1000)=20.262256810920633（参考值） |
| STAT 例19（倒数） | (1.1,18.3),(2.1,9.7),(2.9,6.8),(4.0,4.9),(4.9,4.1) | A=−0.09344061817312763、B=20.267097114570788、r=0.9998526952656159（参考值）、ŷ(3.5)=5.697158557（原文）、x̂(15)=1.342775158（原文） |

---

### Task 1: DAYS 天数计算

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Days.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/DaysTest.kt`

- [ ] **Step 1: Days.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/**
 * DAYS 天数计算（说明书 CN-76~79）：d1、d2、Dys 知二求一。
 * Date Mode（CN-21）：365 按实际天数；360 按 30/360（含 31 日规则，CN-77 L2225）。
 * Date Input（CN-22）：MDY（月日年）或 DMY（日月年）。
 * 日期范围 1901-01-01 ~ 2099-12-31（CN-165）。
 */
object Days {

    enum class DateFormat { MDY, DMY }

    data class Date(val year: Int, val month: Int, val day: Int) : Comparable<Date> {
        override fun compareTo(other: Date): Int =
            compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
    }

    /** 紧凑数字 → Date（MDY 11052022 / DMY 05112022）。非法日期或超范围 → Argument ERROR。 */
    fun parse(input: Double, format: DateFormat): Date {
        if (input < 0 || input != kotlin.math.floor(input)) argErr("日期输入须为非负整数")
        val v = input.toLong()
        val year = (v % 10000).toInt()
        val md = (v / 10000).toInt()
        val month: Int
        val day: Int
        when (format) {
            DateFormat.MDY -> { month = md / 100; day = md % 100 }
            DateFormat.DMY -> { day = md / 100; month = md % 100 }
        }
        return validate(Date(year, month, day))
    }

    /** Date → 紧凑数字（MDY 11052022 / DMY 05112022）。 */
    fun format(date: Date, format: DateFormat): Double {
        val v = when (format) {
            DateFormat.MDY -> date.month * 1000000L + date.day * 10000L + date.year
            DateFormat.DMY -> date.day * 1000000L + date.month * 10000L + date.year
        }
        return v.toDouble()
    }

    /** 日期合法性 + 范围（1901-01-01 ~ 2099-12-31）校验。 */
    fun validate(date: Date): Date {
        if (date.year !in 1901..2099) argErr("年份超出 1901~2099")
        val dim = daysInMonth(date.month, date.year)
        if (dim == 0 || date.day !in 1..dim) argErr("非法日期")
        return date
    }

    fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    fun daysInMonth(month: Int, year: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeap(year)) 29 else 28
        else -> 0
    }

    /** Dys：365 按实际天数（JDN 差）；360 按 30/360（含 31 日规则）。 */
    fun daysBetween(d1: Date, d2: Date, days360: Boolean): Int {
        validate(d1)
        validate(d2)
        return if (days360) {
            val dd1 = if (d1.day == 31) 30 else d1.day
            var y2 = d2.year
            var m2 = d2.month
            var dd2 = d2.day
            if (dd2 == 31) {
                dd2 = 1
                m2++
                if (m2 == 13) { m2 = 1; y2++ }
            }
            360 * (y2 - d1.year) + 30 * (m2 - d1.month) + (dd2 - dd1)
        } else {
            toJdn(d2) - toJdn(d1)
        }
    }

    /** d2 = d1 + Dys（实际历法；说明书未给 360 模式的日期加减公式，例题均用 365）。 */
    fun plusDays(d1: Date, dys: Int): Date {
        validate(d1)
        return validate(fromJdn(toJdn(d1) + dys))
    }

    /** d1 = d2 − Dys（实际历法）。 */
    fun minusDays(d2: Date, dys: Int): Date {
        validate(d2)
        return validate(fromJdn(toJdn(d2) - dys))
    }

    /** 格里历 → JDN（Fliegel–Van Flandern，整数精确）。 */
    internal fun toJdn(date: Date): Int {
        val a = (14 - date.month) / 12
        val y = date.year + 4800 - a
        val m = date.month + 12 * a - 3
        return date.day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    /** JDN → 格里历（Fliegel–Van Flandern 逆变换）。 */
    internal fun fromJdn(jdn: Int): Date {
        var a = jdn + 32044
        val b = (4 * a + 3) / 146097
        a -= 146097 * b / 4
        val c = (4 * a + 3) / 1461
        a -= 1461 * c / 4
        val m = (5 * a + 2) / 153
        val day = a - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = 100 * b + c - 4800 + m / 10
        return Date(year, month, day)
    }

    private fun argErr(msg: String): Nothing = throw CalcException(CalcException.Kind.ARGUMENT, msg)
}
```

- [ ] **Step 2: DaysTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DaysTest {
    private val d1 = Days.Date(2022, 11, 5)
    private val d2 = Days.Date(2023, 4, 27)

    @Test
    fun `manual example 1 days between 365 basis`() {
        // 说明书例1（L2237-2262）：365 基准
        assertEquals(173, Days.daysBetween(d1, d2, days360 = false))
    }

    @Test
    fun `same example 360 basis`() {
        // 30/360：360×0 + 30×(4−11) + (27−5) → 跨年：360×1 + 30×(4−11) + 22 = 172
        assertEquals(172, Days.daysBetween(d1, d2, days360 = true))
    }

    @Test
    fun `31st day rule on 360 basis`() {
        // CN-77 L2225：d1 为 31 日→当月 30 日；d2 为 31 日→次月 1 日
        // 2022-01-31 → 2022-03-31：d1→30，d2→4/1：360×0 + 30×(4−1) + (1−30) = 61
        assertEquals(61, Days.daysBetween(Days.Date(2022, 1, 31), Days.Date(2022, 3, 31), true))
        // 跨年变体：2022-01-31 → 2023-03-31：d2→2023-04-01：360×1 + 30×(4−1) + (1−30) = 421
        assertEquals(421, Days.daysBetween(Days.Date(2022, 1, 31), Days.Date(2023, 3, 31), true))
    }

    @Test
    fun `leap year handling`() {
        // 2024 为闰年：2/1 → 3/1 实际 29 天
        assertEquals(29, Days.daysBetween(Days.Date(2024, 2, 1), Days.Date(2024, 3, 1), false))
        // 2023 非闰年：28 天
        assertEquals(28, Days.daysBetween(Days.Date(2023, 2, 1), Days.Date(2023, 3, 1), false))
    }

    @Test
    fun `manual example 2 and 3 date plus minus days`() {
        // 说明书例2/例3（L2275-2288，365 模式）：d1+173=d2；d2−173=d1
        assertEquals(d2, Days.plusDays(d1, 173))
        assertEquals(d1, Days.minusDays(d2, 173))
    }

    @Test
    fun `plus days zero and negative`() {
        assertEquals(d1, Days.plusDays(d1, 0))
        assertEquals(Days.Date(2022, 11, 4), Days.plusDays(d1, -1))
    }

    @Test
    fun `parse and format mdy`() {
        // CN-22：MDY 月日年
        assertEquals(Days.Date(2022, 11, 5), Days.parse(11052022.0, Days.DateFormat.MDY))
        assertEquals(11052022.0, Days.format(d1, Days.DateFormat.MDY), 0.0)
        // 前导零月份
        assertEquals(Days.Date(2022, 6, 1), Days.parse(6012022.0, Days.DateFormat.MDY))
    }

    @Test
    fun `parse and format dmy`() {
        // CN-22：DMY 日月年
        assertEquals(Days.Date(2022, 11, 5), Days.parse(5112022.0, Days.DateFormat.DMY))
        assertEquals(5112022.0, Days.format(d1, Days.DateFormat.DMY), 0.0)
    }

    @Test
    fun `invalid dates throw argument error`() {
        assertThrows(CalcException::class.java) { Days.parse(1332022.0, Days.DateFormat.MDY) }   // 13 月
        assertThrows(CalcException::class.java) { Days.parse(2292023.0, Days.DateFormat.MDY) }   // 2023-02-29
        assertThrows(CalcException::class.java) { Days.parse(1011900.0, Days.DateFormat.MDY) }   // 1900 超范围
        assertThrows(CalcException::class.java) { Days.parse(1012100.0, Days.DateFormat.MDY) }   // 2100 超范围
        assertThrows(CalcException::class.java) { Days.parse(11052022.5, Days.DateFormat.MDY) }  // 非整数
        val e = assertThrows(CalcException::class.java) { Days.parse(1312022.0, Days.DateFormat.MDY) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
    }

    @Test
    fun `jdn round trip over range`() {
        // JDN 往返一致性（覆盖闰年/世纪年；步长取质数 97 天增加覆盖；保持在合法范围内）
        var d = Days.Date(1901, 1, 1)
        val end = Days.Date(2099, 12, 31)
        while (Days.plusDays(d, 100) <= end) {
            assertEquals(d, Days.minusDays(Days.plusDays(d, 100), 100))
            d = Days.plusDays(d, 97)
        }
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.DaysTest"
```

预期：`BUILD SUCCESSFUL`，10 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): DAYS 天数计算（360/365 基准 + MDY/DMY + JDN 历法）"
```

---

### Task 2: DEPR 折旧（四方法）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Depr.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/DeprTest.kt`

- [ ] **Step 1: Depr.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.floor

/**
 * DEPR 折旧（说明书 CN-80~84）：直线法 SL、定率法 FP、年数总和法 SYD、余额递减法 DB。
 * I%：FP 为折旧比、DB 为折旧因子（200=加倍余额递减 DDB，L2320）；SL/SYD 不用 I%（L2335）。
 * RDVⱼ：第 j 年年终剩余可折旧值（PV−FV 口径）；YR1：折旧第一年的月数（1~12）。
 */
object Depr {

    enum class Method { SL, FP, SYD, DB }

    /** 第 j 年折旧费与第 j 年末剩余可折旧值。 */
    data class Result(val depreciation: Double, val rdv: Double)

    /**
     * 错误（CN-168）：PV/FV/I% 为负 → MATH；n > 255 或 n < 1 → MATH；
     * j > n+1（YR1≠12）或 j > n（YR1=12）→ MATH；YR1 不在 1~12 → ARGUMENT。
     */
    fun depreciate(
        method: Method,
        n: Int,
        iPercent: Double,
        pv: Double,
        fv: Double,
        j: Int,
        yr1: Int
    ): Result {
        if (pv < 0 || fv < 0 || iPercent < 0) mathErr("PV/FV/I% 不得为负")
        if (n < 1 || n > 255) mathErr("n 须为 1 至 255")
        if (yr1 < 1 || yr1 > 12) throw CalcException(CalcException.Kind.ARGUMENT, "YR1 须为 1 至 12")
        val maxJ = if (yr1 == 12) n else n + 1
        if (j < 1 || j > maxJ) mathErr("j 超出范围")
        return when (method) {
            Method.SL -> sl(n, pv, fv, j, yr1)
            Method.FP -> fp(n, iPercent, pv, fv, j, yr1)
            Method.SYD -> syd(n, pv, fv, j, yr1)
            Method.DB -> db(n, iPercent, pv, fv, j, yr1)
        }
    }

    /** 直线法（CN-82）：SL₁ = (PV−FV)/n × YR1/12；SLⱼ = (PV−FV)/n；SLₙ₊₁ = (PV−FV)/n × (12−YR1)/12。 */
    private fun sl(n: Int, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        val base = (pv - fv) / n
        var rdv = pv - fv
        var dep = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> base * yr1 / 12
                n + 1 -> base * (12 - yr1) / 12
                else -> base
            }
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    /** 定率法（CN-82/83）：FP₁ = PV×I%/100×YR1/12；FPⱼ = (RDVⱼ₋₁+FV)×I%/100；FPₙ₊₁ = RDVₙ。 */
    private fun fp(n: Int, iPercent: Double, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        var rdv = pv - fv
        var dep = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> pv * iPercent / 100 * yr1 / 12
                n + 1 -> rdv
                else -> (rdv + fv) * iPercent / 100
            }
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    /**
     * 年数总和法（CN-83/84）：Z = n(n+1)/2；n′ = n − YR1/12；Z′ = (Intg(n′)+1)(Intg(n′)+2×Frac(n′))/2；
     * SYD₁ = n/Z × YR1/12 × (PV−FV)；SYDⱼ = ((n′−j+2)/Z′)(PV−FV−SYD₁)（j≠1）；
     * SYDₙ₊₁ = ((n′−(n+1)+2)/Z′)(PV−FV−SYD₁)×(12−YR1)/12。
     */
    private fun syd(n: Int, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        val z = n * (n + 1) / 2.0
        val nPrime = n - yr1 / 12.0
        val intg = floor(nPrime).toInt()
        val frac = nPrime - intg
        val zPrime = (intg + 1) * (intg + 2 * frac) / 2
        var rdv = pv - fv
        var dep = 0.0
        var syd1 = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> n / z * yr1 / 12.0 * (pv - fv)
                n + 1 -> ((nPrime - (n + 1) + 2) / zPrime) * (pv - fv - syd1) * (12 - yr1) / 12.0
                else -> ((nPrime - year + 2) / zPrime) * (pv - fv - syd1)
            }
            if (year == 1) syd1 = dep
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    /** 余额递减法（CN-84）：DB₁ = PV×I%/(100n)×YR1/12；DBⱼ = (RDVⱼ₋₁+FV)×I%/(100n)；DBₙ₊₁ = RDVₙ。 */
    private fun db(n: Int, iPercent: Double, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        var rdv = pv - fv
        var dep = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> pv * iPercent / (100.0 * n) * yr1 / 12
                n + 1 -> rdv
                else -> (rdv + fv) * iPercent / (100.0 * n)
            }
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
```

- [ ] **Step 2: DeprTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DeprTest {
    // 说明书例（L2324-2373）：n=6、PV=150000、FV=0、j=3、YR1=2；FP 用 I%=25、DB 用 I%=200
    private val n = 6
    private val pv = 150000.0
    private val fv = 0.0
    private val j = 3
    private val yr1 = 2

    @Test
    fun `manual example 1 straight line`() {
        val r = Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, j, yr1)
        assertEquals(25000.0, r.depreciation, 1e-9)          // 原文（L2352）
        assertEquals(95833.33333333334, r.rdv, 1e-6)         // 参考值（OCR 误作 95855 已证伪）
    }

    @Test
    fun `sl year 1 and n+1 proration`() {
        // SL₁ = 25000×2/12；SL₇（n+1 年）= 25000×10/12
        assertEquals(4166.666666666667, Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, 1, yr1).depreciation, 1e-9)
        val last = Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, n + 1, yr1)
        assertEquals(20833.333333333332, last.depreciation, 1e-9)
        assertEquals(0.0, last.rdv, 1e-6)                    // 折完归零
    }

    @Test
    fun `manual example 2 fixed percent`() {
        val r = Depr.depreciate(Depr.Method.FP, n, 25.0, pv, fv, j, yr1)
        assertEquals(26953.125, r.depreciation, 1e-9)
        assertEquals(80859.375, r.rdv, 1e-9)
    }

    @Test
    fun `manual example 3 sum of years digits`() {
        val r = Depr.depreciate(Depr.Method.SYD, n, 0.0, pv, fv, j, yr1)
        assertEquals(34523.80952380953, r.depreciation, 1e-6)
        assertEquals(66666.66666666669, r.rdv, 1e-6)
        // SYD₁ = 6/21×2/12×150000
        assertEquals(7142.857142857142, Depr.depreciate(Depr.Method.SYD, n, 0.0, pv, fv, 1, yr1).depreciation, 1e-9)
    }

    @Test
    fun `manual example 4 declining balance ddb`() {
        val r = Depr.depreciate(Depr.Method.DB, n, 200.0, pv, fv, j, yr1)
        assertEquals(31481.48148148148, r.depreciation, 1e-9)
        assertEquals(62962.962962962956, r.rdv, 1e-9)
        assertEquals(8333.333333333334, Depr.depreciate(Depr.Method.DB, n, 200.0, pv, fv, 1, yr1).depreciation, 1e-9)
    }

    @Test
    fun `yr1 12 means no proration and no extra year`() {
        val r = Depr.depreciate(Depr.Method.SL, 5, 0.0, 10000.0, 0.0, 2, 12)
        assertEquals(2000.0, r.depreciation, 1e-12)
        assertThrows(CalcException::class.java) {
            Depr.depreciate(Depr.Method.SL, 5, 0.0, 10000.0, 0.0, 6, 12)
        }
    }

    @Test
    fun `error conditions`() {
        // CN-168：负 PV/FV/I% → MATH；n>255 → MATH；j 超范围 → MATH；YR1>12 → ARGUMENT
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, -1.0, fv, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, pv, -1.0, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.FP, n, -25.0, pv, fv, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, 256, 0.0, pv, fv, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, n + 2, yr1) }
        val e = assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, j, 13) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.DeprTest"
```

预期：`BUILD SUCCESSFUL`，7 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): DEPR 折旧四方法（SL/FP/SYD/DB，含月折算）"
```

---

### Task 3: BOND 债券

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Bond.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/BondTest.kt`

- [ ] **Step 1: Bond.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.solver.Solver
import kotlin.math.pow

/**
 * BOND 债券（说明书 CN-85~92）。
 * 计算期两形态（CN-22 Bond Date）：Date（按购买日 d1/赎回日 d2）与 Term（按票息支付数 n）。
 * 日计数基准随 Date Mode（360/365）；Periods/Y：Annual=1 / Semi=2。
 * YLD 用牛顿法（CN-90，近似值精度受条件影响——说明书警告 L2729）。
 */
object Bond {

    /** Date 模式结果：净价 PRC、应计利息 INT、含息价 CST（每 $100 票面，CN-89）。 */
    data class DateResult(val prc: Double, val int: Double, val cst: Double)

    /**
     * PRC —— Bond Date = Date（CN-88/89）。
     * RDV ≤ 0 或 CPN < 0 → Math ERROR（CN-168）。d1 ≥ d2 → Argument ERROR（本计划裁定）。
     */
    fun prcDate(
        d1: Days.Date, d2: Days.Date,
        rdv: Double, cpn: Double, yld: Double,
        paymentsPerYear: Int, days360: Boolean
    ): DateResult {
        if (rdv <= 0 || cpn < 0) mathErr("须满足 RDV > 0、CPN ≥ 0")
        val m = checkM(paymentsPerYear)
        val sched = couponSchedule(d1, d2, m)
        val prev = sched.last { it <= d1 }
        val next = sched.first { it > d1 }
        val a = Days.daysBetween(prev, d1, days360)   // A：上一票息日 → 结算日
        val d = Days.daysBetween(prev, next, days360) // D：结算日所在票息期天数
        val n = sched.count { it > d1 && it <= d2 }   // N：到期前剩余票息次数
        val b = d - a                                 // B = D − A
        val y = yld / 100 / m
        val coupon = cpn / m
        val prc = if (n <= 1) {
            // 不超过一个票息期（CN-88）
            -(rdv + coupon) / (1 + (b.toDouble() / d) * y) + (a.toDouble() / d) * coupon
        } else {
            // 一个以上票息期（CN-89）
            var s = 0.0
            for (k in 1..n) s += coupon / (1 + y).pow(k - 1 + b.toDouble() / d)
            -rdv / (1 + y).pow(n - 1 + b.toDouble() / d) - s + (a.toDouble() / d) * coupon
        }
        val int = -(a.toDouble() / d) * coupon
        return DateResult(prc, int, prc + int)
    }

    /**
     * YLD —— Bond Date = Date（牛顿法，CN-90）。
     * RDV ≤ 0 或 PRC ≥ 0 → Math ERROR（CN-168）。
     */
    fun yldDate(
        d1: Days.Date, d2: Days.Date,
        rdv: Double, cpn: Double, prc: Double,
        paymentsPerYear: Int, days360: Boolean
    ): Double {
        if (rdv <= 0 || prc >= 0) mathErr("须满足 RDV > 0、PRC < 0")
        return Solver.solve(
            f = { y -> prcDate(d1, d2, rdv, cpn, y, paymentsPerYear, days360).prc - prc },
            x0 = 5.0,
            lower = -99.99,
            upper = 10000.0
        )
    }

    /**
     * PRC —— Bond Date = Term（CN-89）。INT=0、CST=PRC。
     * 前置要求 Date Mode=360 且 Annual（CN-88 L2638）属设置层职责，引擎不强制。
     */
    fun prcTerm(n: Int, rdv: Double, cpn: Double, yld: Double, paymentsPerYear: Int): Double {
        if (rdv <= 0 || cpn < 0) mathErr("须满足 RDV > 0、CPN ≥ 0")
        if (n < 1) mathErr("n 须为正整数")
        val m = checkM(paymentsPerYear)
        val y = yld / 100 / m
        val coupon = cpn / m
        var s = 0.0
        for (k in 1..n) s += coupon / (1 + y).pow(k)
        return -rdv / (1 + y).pow(n) - s
    }

    /** YLD —— Bond Date = Term（牛顿法）。 */
    fun yldTerm(n: Int, rdv: Double, cpn: Double, prc: Double, paymentsPerYear: Int): Double {
        if (rdv <= 0 || prc >= 0) mathErr("须满足 RDV > 0、PRC < 0")
        if (n < 1) mathErr("n 须为正整数")
        val m = checkM(paymentsPerYear)
        return Solver.solve(
            f = { y -> prcTerm(n, rdv, cpn, y, m) - prc },
            x0 = 5.0,
            lower = -99.99,
            upper = 10000.0
        )
    }

    /**
     * 票息日序列：与赎回日 d2 同月日、每年 m 次（月日钳制到当月长度，modified-following 惯例）。
     * 生成范围覆盖 d1 前 2 年至 d2 当年（保证 d1 所在票息期与全部剩余票息在列）。
     */
    private fun couponSchedule(d1: Days.Date, d2: Days.Date, m: Int): List<Days.Date> {
        if (d1 >= d2) throw CalcException(CalcException.Kind.ARGUMENT, "须满足 d1 < d2")
        val step = 12 / m
        val result = mutableListOf<Days.Date>()
        for (yy in (d1.year - 2)..d2.year) {
            for (k in 0 until m) {
                val t = yy * 12 + (d2.month - 1) - k * step
                val year = t / 12
                val month = t % 12 + 1
                result += Days.Date(year, month, minOf(d2.day, Days.daysInMonth(month, year)))
            }
        }
        return result.distinct().sorted()
    }

    private fun checkM(paymentsPerYear: Int): Int {
        if (paymentsPerYear != 1 && paymentsPerYear != 2) {
            throw CalcException(CalcException.Kind.ARGUMENT, "Periods/Y 仅支持 Annual(1) 或 Semi(2)")
        }
        return paymentsPerYear
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
```

- [ ] **Step 2: BondTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BondTest {
    // 说明书例（CN-86~88）：Date、365、Annual，d1=2022-06-01，d2=2024-12-15，RDV=100，CPN=3，YLD=4
    private val d1 = Days.Date(2022, 6, 1)
    private val d2 = Days.Date(2024, 12, 15)

    @Test
    fun `manual example 1 prc date mode`() {
        // A=168、D=365、B=197、N=3（日程推算已验证）；PRC 原文 −97.6151555（设定值表）
        val r = Bond.prcDate(d1, d2, 100.0, 3.0, 4.0, 1, days360 = false)
        assertEquals(-97.6151555, r.prc, 1e-9)
        assertEquals(-1.3808219178082193, r.int, 1e-12)   // 参考值（截图缺失）
        assertEquals(-98.9959774189964, r.cst, 1e-9)      // 参考值
    }

    @Test
    fun `manual example 2 yld date round trip`() {
        // 例2：以 PRC=−97.6151555 反解 YLD → 4
        val y = Bond.yldDate(d1, d2, 100.0, 3.0, -97.6151555, 1, false)
        assertEquals(4.0, y, 1e-6)
    }

    @Test
    fun `manual example 3 prc term mode`() {
        // 例3：Term、n=3（前置 Date Mode=360、Annual 属设置层）
        assertEquals(-97.22490896677286, Bond.prcTerm(3, 100.0, 3.0, 4.0, 1), 1e-9)
    }

    @Test
    fun `manual example 4 yld term`() {
        // 例4：Term、n=3、PRC=−97.6151555 → YLD（Term 口径，参考值）
        assertEquals(3.857044586, Bond.yldTerm(3, 100.0, 3.0, -97.6151555, 1), 1e-6)
    }

    @Test
    fun `zero coupon bond`() {
        // 零息票（CN-86 L2562）：CPN=0 → 纯贴现
        val p = Bond.prcTerm(3, 100.0, 0.0, 4.0, 1)
        assertEquals(-100.0 / 1.04 / 1.04 / 1.04, p, 1e-9)
    }

    @Test
    fun `semi annual schedule`() {
        // Semi（Periods/Y=2）：票息日为每年 6/15 与 12/15；2022-06-01 在 2021-12-15 与 2022-06-15 之间
        // A=168（2021-12-15→2022-06-01），D=182（2021-12-15→2022-06-15），B=14，N=6
        val r = Bond.prcDate(d1, d2, 100.0, 3.0, 4.0, 2, days360 = false)
        // 参考值（按 CN-88/89 公式高精度计算）
        assertEquals(-97.60774696391445, r.prc, 1e-9)
        assertEquals(-1.3846153846153846, r.int, 1e-12)
        assertEquals(-98.99236234852984, r.cst, 1e-9)
    }

    @Test
    fun `par bond prices at minus 100`() {
        // 票面利率 = 收益率时平价：PRC = −100（Term、Annual、n=5）
        assertEquals(-100.0, Bond.prcTerm(5, 100.0, 5.0, 5.0, 1), 1e-9)
    }

    @Test
    fun `error conditions`() {
        // CN-168：PRC 计算要求 RDV>0、CPN≥0；YLD 要求 RDV>0、PRC<0
        assertThrows(CalcException::class.java) { Bond.prcTerm(3, 0.0, 3.0, 4.0, 1) }
        assertThrows(CalcException::class.java) { Bond.prcTerm(3, 100.0, -1.0, 4.0, 1) }
        assertThrows(CalcException::class.java) { Bond.yldTerm(3, 100.0, 3.0, 97.0, 1) }   // PRC 非负
        assertThrows(CalcException::class.java) { Bond.prcTerm(0, 100.0, 3.0, 4.0, 1) }    // n<1
        val e = assertThrows(CalcException::class.java) { Bond.prcTerm(3, 100.0, 3.0, 4.0, 3) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
        assertThrows(CalcException::class.java) {
            Bond.prcDate(d2, d1, 100.0, 3.0, 4.0, 1, false)   // d1 ≥ d2
        }
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.BondTest"
```

预期：`BUILD SUCCESSFUL`，8 个测试全过。若 YLD 牛顿法未直接收敛而落入二分兜底，属设计内行为；最终失败才需回报，不得改实现逻辑凑测试。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): BOND 债券（Date/Term 双形态 PRC/INT/CST + 牛顿法 YLD）"
```

---

### Task 4: BEVN 损益分析（6 子模式）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Bevn.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/BevnTest.kt`

- [ ] **Step 1: Bevn.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/**
 * BEVN 损益分析（说明书 CN-93~110）：BEV、MOS、DOL、DFL、DCL、QTY CONV 六子模式。
 * 全部变量正解/反解（BEV 五变量任解，CN-95 L2872-2874）；分母为 0 → Math ERROR（CN-169 通用规则）。
 * BEV 受两个设置影响（CN-23）：PRF/Ratio（目标利润额/利润率）、B-Even（销售量/销售额）——
 * 引擎按函数区分，设置切换属 state 层。
 */
object Bevn {

    // ---------- BEV（CN-94~99） ----------

    /** 利润模式（PRF/Ratio=PRF）：QBE = (FC+PRF)/(PRC−VCU)。 */
    fun qbePrf(prc: Double, vcu: Double, fc: Double, prf: Double): Double {
        val d = prc - vcu
        if (d == 0.0) mathErr("除以 0")
        return (fc + prf) / d
    }

    /** SBE = QBE × PRC。 */
    fun sbePrf(prc: Double, vcu: Double, fc: Double, prf: Double): Double =
        qbePrf(prc, vcu, fc, prf) * prc

    /** 利润率模式（PRF/Ratio=r%）：QBE = FC/(PRC×(1−r%/100)−VCU)。 */
    fun qbeRatio(prc: Double, vcu: Double, fc: Double, rPercent: Double): Double {
        val d = prc * (1 - rPercent / 100) - vcu
        if (d == 0.0) mathErr("除以 0")
        return fc / d
    }

    /** SBE = QBE × PRC。 */
    fun sbeRatio(prc: Double, vcu: Double, fc: Double, rPercent: Double): Double =
        qbeRatio(prc, vcu, fc, rPercent) * prc

    // BEV 反解（PRF 模式）
    /** PRC = (FC+PRF)/QBE + VCU。 */
    fun prcFromQbePrf(qbe: Double, vcu: Double, fc: Double, prf: Double): Double {
        if (qbe == 0.0) mathErr("除以 0")
        return (fc + prf) / qbe + vcu
    }

    /** VCU = PRC − (FC+PRF)/QBE。 */
    fun vcuFromQbePrf(qbe: Double, prc: Double, fc: Double, prf: Double): Double {
        if (qbe == 0.0) mathErr("除以 0")
        return prc - (fc + prf) / qbe
    }

    /** FC = QBE×(PRC−VCU) − PRF。 */
    fun fcFromQbePrf(qbe: Double, prc: Double, vcu: Double, prf: Double): Double =
        qbe * (prc - vcu) - prf

    /** PRF = QBE×(PRC−VCU) − FC。 */
    fun prfFromQbe(qbe: Double, prc: Double, vcu: Double, fc: Double): Double =
        qbe * (prc - vcu) - fc

    // BEV 反解（r% 模式）
    /** PRC = (FC/QBE + VCU)/(1 − r%/100)。 */
    fun prcFromQbeRatio(qbe: Double, vcu: Double, fc: Double, rPercent: Double): Double {
        val d = 1 - rPercent / 100
        if (qbe == 0.0 || d == 0.0) mathErr("除以 0")
        return (fc / qbe + vcu) / d
    }

    /** VCU = PRC×(1−r%/100) − FC/QBE。 */
    fun vcuFromQbeRatio(qbe: Double, prc: Double, fc: Double, rPercent: Double): Double {
        if (qbe == 0.0) mathErr("除以 0")
        return prc * (1 - rPercent / 100) - fc / qbe
    }

    /** FC = QBE×(PRC×(1−r%/100) − VCU)。 */
    fun fcFromQbeRatio(qbe: Double, prc: Double, vcu: Double, rPercent: Double): Double =
        qbe * (prc * (1 - rPercent / 100) - vcu)

    /** r% = (1 − (FC/QBE + VCU)/PRC) × 100。 */
    fun rPercentFromQbe(qbe: Double, prc: Double, vcu: Double, fc: Double): Double {
        if (qbe == 0.0 || prc == 0.0) mathErr("除以 0")
        return (1 - (fc / qbe + vcu) / prc) * 100
    }

    // ---------- MOS 安全边际（CN-100 前后）：MOS = (SAL−SBE)/SAL ----------

    fun mos(sal: Double, sbe: Double): Double {
        if (sal == 0.0) mathErr("除以 0")
        return (sal - sbe) / sal
    }

    /** SAL = SBE/(1−MOS)。 */
    fun salFromMos(mos: Double, sbe: Double): Double {
        if (1 - mos == 0.0) mathErr("除以 0")
        return sbe / (1 - mos)
    }

    /** SBE = SAL×(1−MOS)。 */
    fun sbeFromMos(mos: Double, sal: Double): Double = sal * (1 - mos)

    // ---------- DOL 经营杠杆（CN-102 前后）：DOL = (SAL−VC)/(SAL−VC−FC) ----------

    fun dol(sal: Double, vc: Double, fc: Double): Double {
        val d = sal - vc - fc
        if (d == 0.0) mathErr("除以 0")
        return (sal - vc) / d
    }

    /** SAL = DOL×FC/(DOL−1) + VC。 */
    fun salFromDol(dol: Double, vc: Double, fc: Double): Double {
        if (dol - 1 == 0.0) mathErr("除以 0")
        return dol * fc / (dol - 1) + vc
    }

    /** VC = SAL − DOL×FC/(DOL−1)。 */
    fun vcFromDol(dol: Double, sal: Double, fc: Double): Double {
        if (dol - 1 == 0.0) mathErr("除以 0")
        return sal - dol * fc / (dol - 1)
    }

    /** FC = (SAL−VC)×(DOL−1)/DOL。 */
    fun fcFromDol(dol: Double, sal: Double, vc: Double): Double {
        if (dol == 0.0) mathErr("除以 0")
        return (sal - vc) * (dol - 1) / dol
    }

    // ---------- DFL 财务杠杆（CN-104~106）：DFL = EIT/(EIT−ITR) ----------

    fun dfl(eit: Double, itr: Double): Double {
        val d = eit - itr
        if (d == 0.0) mathErr("除以 0")
        return eit / d
    }

    /** EIT = DFL×ITR/(DFL−1)。 */
    fun eitFromDfl(dfl: Double, itr: Double): Double {
        if (dfl - 1 == 0.0) mathErr("除以 0")
        return dfl * itr / (dfl - 1)
    }

    /** ITR = EIT×(DFL−1)/DFL。 */
    fun itrFromDfl(dfl: Double, eit: Double): Double {
        if (dfl == 0.0) mathErr("除以 0")
        return eit * (dfl - 1) / dfl
    }

    // ---------- DCL 复合杠杆（CN-106~108）：DCL = (SAL−VC)/(SAL−VC−FC−ITR) ----------

    fun dcl(sal: Double, vc: Double, fc: Double, itr: Double): Double {
        val d = sal - vc - fc - itr
        if (d == 0.0) mathErr("除以 0")
        return (sal - vc) / d
    }

    /** SAL = DCL×(FC+ITR)/(DCL−1) + VC。 */
    fun salFromDcl(dcl: Double, vc: Double, fc: Double, itr: Double): Double {
        if (dcl - 1 == 0.0) mathErr("除以 0")
        return dcl * (fc + itr) / (dcl - 1) + vc
    }

    /** VC = SAL − DCL×(FC+ITR)/(DCL−1)。 */
    fun vcFromDcl(dcl: Double, sal: Double, fc: Double, itr: Double): Double {
        if (dcl - 1 == 0.0) mathErr("除以 0")
        return sal - dcl * (fc + itr) / (dcl - 1)
    }

    /** FC = (SAL−VC)×(DCL−1)/DCL − ITR。 */
    fun fcFromDcl(dcl: Double, sal: Double, vc: Double, itr: Double): Double {
        if (dcl == 0.0) mathErr("除以 0")
        return (sal - vc) * (dcl - 1) / dcl - itr
    }

    /** ITR = (SAL−VC)×(DCL−1)/DCL − FC。 */
    fun itrFromDcl(dcl: Double, sal: Double, vc: Double, fc: Double): Double {
        if (dcl == 0.0) mathErr("除以 0")
        return (sal - vc) * (dcl - 1) / dcl - fc
    }

    // ---------- QTY CONV（CN-109/110）：SAL = PRC×QTY；VC = VCU×QTY（两组 QTY 联动） ----------

    fun sal(prc: Double, qty: Double): Double = prc * qty

    fun prcFromSal(sal: Double, qty: Double): Double {
        if (qty == 0.0) mathErr("除以 0")
        return sal / qty
    }

    fun qtyFromSal(sal: Double, prc: Double): Double {
        if (prc == 0.0) mathErr("除以 0")
        return sal / prc
    }

    fun vc(vcu: Double, qty: Double): Double = vcu * qty

    fun vcuFromVc(vc: Double, qty: Double): Double {
        if (qty == 0.0) mathErr("除以 0")
        return vc / qty
    }

    fun qtyFromVc(vc: Double, vcu: Double): Double {
        if (vcu == 0.0) mathErr("除以 0")
        return vc / vcu
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
```

- [ ] **Step 2: BevnTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BevnTest {

    // ---- BEV（说明书例1-6，L2794-2874）：PRC=100、VCU=50、FC=100000 ----

    @Test
    fun `manual bev profit mode`() {
        // 例1/例2：PRF=0 → 损益平衡点
        assertEquals(2000.0, Bevn.qbePrf(100.0, 50.0, 100000.0, 0.0), 1e-9)
        assertEquals(200000.0, Bevn.sbePrf(100.0, 50.0, 100000.0, 0.0), 1e-6)
        // 例3/例4：PRF=400000
        assertEquals(10000.0, Bevn.qbePrf(100.0, 50.0, 100000.0, 400000.0), 1e-9)
        assertEquals(1000000.0, Bevn.sbePrf(100.0, 50.0, 100000.0, 400000.0), 1e-6)
    }

    @Test
    fun `manual bev ratio mode`() {
        // 例5/例6：r%=40
        assertEquals(10000.0, Bevn.qbeRatio(100.0, 50.0, 100000.0, 40.0), 1e-9)
        assertEquals(1000000.0, Bevn.sbeRatio(100.0, 50.0, 100000.0, 40.0), 1e-6)
    }

    @Test
    fun `bev inverse solutions round trip`() {
        // 五变量任解（L2872-2874）：由 QBE=10000 反解其余四变量
        assertEquals(100.0, Bevn.prcFromQbePrf(10000.0, 50.0, 100000.0, 400000.0), 1e-9)
        assertEquals(50.0, Bevn.vcuFromQbePrf(10000.0, 100.0, 100000.0, 400000.0), 1e-9)
        assertEquals(100000.0, Bevn.fcFromQbePrf(10000.0, 100.0, 50.0, 400000.0), 1e-6)
        assertEquals(400000.0, Bevn.prfFromQbe(10000.0, 100.0, 50.0, 100000.0), 1e-6)
        assertEquals(40.0, Bevn.rPercentFromQbe(10000.0, 100.0, 50.0, 100000.0), 1e-9)
    }

    // ---- MOS（说明书例，L2940）：SAL=1200000、SBE=1000000 ----

    @Test
    fun `manual mos`() {
        assertEquals(1.0 / 6, Bevn.mos(1200000.0, 1000000.0), 1e-12)
        assertEquals(1200000.0, Bevn.salFromMos(1.0 / 6, 1000000.0), 1e-6)
        assertEquals(1000000.0, Bevn.sbeFromMos(1.0 / 6, 1200000.0), 1e-6)
    }

    // ---- DOL（L3015）：SAL=1200000、VC=600000、FC=200000 → 1.5 ----

    @Test
    fun `manual dol`() {
        assertEquals(1.5, Bevn.dol(1200000.0, 600000.0, 200000.0), 1e-12)
        assertEquals(1200000.0, Bevn.salFromDol(1.5, 600000.0, 200000.0), 1e-6)
        assertEquals(600000.0, Bevn.vcFromDol(1.5, 1200000.0, 200000.0), 1e-6)
        assertEquals(200000.0, Bevn.fcFromDol(1.5, 1200000.0, 600000.0), 1e-6)
    }

    // ---- DFL（L3092）：EIT=400000、ITR=80000 → 1.25 ----

    @Test
    fun `manual dfl`() {
        assertEquals(1.25, Bevn.dfl(400000.0, 80000.0), 1e-12)
        assertEquals(400000.0, Bevn.eitFromDfl(1.25, 80000.0), 1e-6)
        assertEquals(80000.0, Bevn.itrFromDfl(1.25, 400000.0), 1e-6)
    }

    // ---- DCL（L3171）：SAL=1200000、VC=600000、FC=200000、ITR=100000 → 2 ----

    @Test
    fun `manual dcl`() {
        assertEquals(2.0, Bevn.dcl(1200000.0, 600000.0, 200000.0, 100000.0), 1e-12)
        assertEquals(1200000.0, Bevn.salFromDcl(2.0, 600000.0, 200000.0, 100000.0), 1e-6)
        assertEquals(600000.0, Bevn.vcFromDcl(2.0, 1200000.0, 200000.0, 100000.0), 1e-6)
        assertEquals(200000.0, Bevn.fcFromDcl(2.0, 1200000.0, 600000.0, 100000.0), 1e-6)
        assertEquals(100000.0, Bevn.itrFromDcl(2.0, 1200000.0, 600000.0, 200000.0), 1e-6)
    }

    // ---- QTY CONV（L3260）：SAL=100000、PRC=200、QTY=500；VC=15000、VCU=30、QTY=500 ----

    @Test
    fun `manual qty conv`() {
        assertEquals(500.0, Bevn.qtyFromSal(100000.0, 200.0), 1e-12)
        assertEquals(500.0, Bevn.qtyFromVc(15000.0, 30.0), 1e-12)
        assertEquals(100000.0, Bevn.sal(200.0, 500.0), 1e-9)
        assertEquals(200.0, Bevn.prcFromSal(100000.0, 500.0), 1e-12)
        assertEquals(15000.0, Bevn.vc(30.0, 500.0), 1e-9)
        assertEquals(30.0, Bevn.vcuFromVc(15000.0, 500.0), 1e-12)
    }

    @Test
    fun `division by zero throws math error`() {
        assertThrows(CalcException::class.java) { Bevn.qbePrf(50.0, 50.0, 100000.0, 0.0) }
        assertThrows(CalcException::class.java) { Bevn.qbeRatio(50.0, 50.0, 100000.0, 0.0) }
        assertThrows(CalcException::class.java) { Bevn.mos(0.0, 100.0) }
        assertThrows(CalcException::class.java) { Bevn.dol(100.0, 50.0, 50.0) }
        assertThrows(CalcException::class.java) { Bevn.dfl(100.0, 100.0) }
        assertThrows(CalcException::class.java) { Bevn.dcl(100.0, 50.0, 30.0, 20.0) }
        assertThrows(CalcException::class.java) { Bevn.qtyFromSal(100.0, 0.0) }
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.BevnTest"
```

预期：`BUILD SUCCESSFUL`，8 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): BEVN 损益分析六子模式（BEV/MOS/DOL/DFL/DCL/QTY CONV）"
```

---

### Task 5: STAT 统计（1-VAR + 7 种回归）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/core/finance/Stat.kt`
- Create: `app/src/test/java/com/fincalc/app/core/finance/StatTest.kt`

- [ ] **Step 1: Stat.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * STAT 统计（说明书 CN-130~160）：1-VAR 单变量统计 + 2-VAR 七种回归。
 * FREQ 为频率（重复次数，CN-131 L4034-4038）；数据行数上限（80/40/26）由 UI 编辑器层约束。
 * 回归模型（CN-130）：A+BX、_+CX²、ln X、e^X、A•B^X、A•X^B、1/X。
 */
object Stat {

    /** 数据行：1-VAR 时 y 为 null；freq 为频率（默认 1）。单类型设计避免 JVM 泛型签名冲突。 */
    data class Entry(val x: Double, val y: Double? = null, val freq: Double = 1.0)

    enum class RegType { LINEAR, QUADRATIC, LOG, EXP, AB_EXP, POWER, RECIPROCAL }

    /** 回归结果：c 仅二次回归（CN-147）；r 除二次回归外（二次回归 Reg 菜单无 r，CN-149）。 */
    data class RegResult(val a: Double, val b: Double, val c: Double? = null, val r: Double? = null)

    // ---------- 1-VAR（CN-137，忽略 y） ----------

    /** n = Σf。 */
    fun count(data: List<Entry>): Double {
        check1(data)
        return data.sumOf { it.freq }
    }

    /** Σx（频率加权）。 */
    fun sumX(data: List<Entry>): Double = sumW(data) { x }

    /** Σx²（频率加权）。 */
    fun sumX2(data: List<Entry>): Double = sumW(data) { x * x }

    /** x̄ = Σx/n。 */
    fun meanX(data: List<Entry>): Double {
        val n = count(data)
        if (n == 0.0) mathErr("无有效数据")
        return sumX(data) / n
    }

    /** 总体标准差 xσn = √(Σ(x−x̄)²/n)（定义式两遍法）。 */
    fun stdXn(data: List<Entry>): Double {
        val n = count(data)
        if (n == 0.0) mathErr("无有效数据")
        val m = meanX(data)
        return sqrt(data.sumOf { it.freq * (it.x - m) * (it.x - m) } / n)
    }

    /** 样本标准差 xσn−1 = √(Σ(x−x̄)²/(n−1))。n ≤ 1 → Math ERROR。 */
    fun stdXn1(data: List<Entry>): Double {
        val n = count(data)
        if (n <= 1) mathErr("样本数不足")
        val m = meanX(data)
        return sqrt(data.sumOf { it.freq * (it.x - m) * (it.x - m) } / (n - 1))
    }

    fun minX(data: List<Entry>): Double {
        check1(data)
        return data.minOf { it.x }
    }

    fun maxX(data: List<Entry>): Double {
        check1(data)
        return data.maxOf { it.x }
    }

    // ---------- 2-VAR（要求 y 非空，CN-141~146） ----------

    fun sumY(data: List<Entry>): Double = sumW2(data) { y!! }
    fun sumY2(data: List<Entry>): Double = sumW2(data) { y!! * y!! }
    fun sumXY(data: List<Entry>): Double = sumW2(data) { x * y!! }
    fun sumX3(data: List<Entry>): Double = sumW(data) { x * x * x }
    fun sumX2Y(data: List<Entry>): Double = sumW2(data) { x * x * y!! }
    fun sumX4(data: List<Entry>): Double = sumW(data) { x * x * x * x }

    fun meanY(data: List<Entry>): Double {
        val n = count(data)
        if (n == 0.0) mathErr("无有效数据")
        return sumY(data) / n
    }

    fun stdYn(data: List<Entry>): Double {
        check2(data)
        val m = meanY(data)
        return sqrt(data.sumOf { it.freq * (it.y!! - m) * (it.y!! - m) } / count(data))
    }

    fun stdYn1(data: List<Entry>): Double {
        val n = count(data)
        if (n <= 1) mathErr("样本数不足")
        check2(data)
        val m = meanY(data)
        return sqrt(data.sumOf { it.freq * (it.y!! - m) * (it.y!! - m) } / (n - 1))
    }

    fun minY(data: List<Entry>): Double {
        check2(data)
        return data.minOf { it.y!! }
    }

    fun maxY(data: List<Entry>): Double {
        check2(data)
        return data.maxOf { it.y!! }
    }

    // ---------- 回归（CN-141~160） ----------

    fun regress(type: RegType, data: List<Entry>): RegResult {
        check2(data)
        return when (type) {
            RegType.LINEAR -> linear(data)
            RegType.QUADRATIC -> quadratic(data)
            RegType.LOG -> logReg(data)
            RegType.EXP -> expReg(data)
            RegType.AB_EXP -> abExpReg(data)
            RegType.POWER -> powerReg(data)
            RegType.RECIPROCAL -> recipReg(data)
        }
    }

    /** 线性回归 y = A + BX（CN-141）。 */
    private fun linear(data: List<Entry>): RegResult {
        val n = count(data)
        val sx = sumX(data); val sxx = sumX2(data)
        val sy = sumY(data); val syy = sumY2(data)
        val sxy = sumXY(data)
        val bn = n * sxy - sx * sy
        val bd = n * sxx - sx * sx
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = (sy - b * sx) / n
        val rd = (n * sxx - sx * sx) * (n * syy - sy * sy)
        if (rd <= 0.0) mathErr("除以 0")
        val r = bn / sqrt(rd)
        return RegResult(a, b, null, r)
    }

    /** 二次回归 y = A + BX + CX²（CN-147~149，无 r）。 */
    private fun quadratic(data: List<Entry>): RegResult {
        val n = count(data)
        val sx = sumX(data); val sx2 = sumX2(data); val sx3 = sumX3(data); val sx4 = sumX4(data)
        val sy = sumY(data); val sxy = sumXY(data); val sx2y = sumX2Y(data)
        val sxx = sx2 - sx * sx / n
        val sxy_ = sxy - sx * sy / n
        val sxx2 = sx3 - sx * sx2 / n
        val sx2x2 = sx4 - sx2 * sx2 / n
        val sx2y_ = sx2y - sx2 * sy / n
        val d = sxx * sx2x2 - sxx2 * sxx2
        if (d == 0.0) mathErr("除以 0")
        val b = (sxy_ * sx2x2 - sx2y_ * sxx2) / d
        val c = (sx2y_ * sxx - sxy_ * sxx2) / d
        val a = sy / n - b * sx / n - c * sx2 / n
        return RegResult(a, b, c, null)
    }

    /** 对数回归 y = A + B·ln X（CN-151）。x ≤ 0 → Math ERROR。 */
    private fun logReg(data: List<Entry>): RegResult {
        val tx = data.map { Entry(lnPos(it.x), it.y, it.freq) }
        val n = count(tx)
        val su = sumX(tx); val suu = sumX2(tx)
        val sy = sumY(tx); val syy = sumY2(tx)
        val suy = sumXY(tx)
        val bn = n * suy - su * sy
        val bd = n * suu - su * su
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = (sy - b * su) / n
        val rd = (n * suu - su * su) * (n * syy - sy * sy)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, bn / sqrt(rd))
    }

    /** e 指数回归 y = A·e^(BX)（CN-151）。y ≤ 0 → Math ERROR。 */
    private fun expReg(data: List<Entry>): RegResult {
        val ty = data.map { Entry(it.x, lnPos(it.y!!), it.freq) }
        val n = count(ty)
        val sx = sumX(ty); val sxx = sumX2(ty)
        val sv = sumY(ty); val svv = sumY2(ty)
        val sxv = sumXY(ty)
        val bn = n * sxv - sx * sv
        val bd = n * sxx - sx * sx
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = exp((sv - b * sx) / n)
        val rd = (n * sxx - sx * sx) * (n * svv - sv * sv)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, bn / sqrt(rd))
    }

    /** ab 指数回归 y = A·B^X（CN-151/152）：对数域同 e 指数，B = e^(对数斜率)。 */
    private fun abExpReg(data: List<Entry>): RegResult {
        val ty = data.map { Entry(it.x, lnPos(it.y!!), it.freq) }
        val n = count(ty)
        val sx = sumX(ty); val sxx = sumX2(ty)
        val sv = sumY(ty); val svv = sumY2(ty)
        val sxv = sumXY(ty)
        val bn = n * sxv - sx * sv
        val bd = n * sxx - sx * sx
        if (bd == 0.0) mathErr("除以 0")
        val bSlope = bn / bd
        val a = exp((sv - bSlope * sx) / n)
        val rd = (n * sxx - sx * sx) * (n * svv - sv * sv)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, exp(bSlope), null, bn / sqrt(rd))
    }

    /** 幂回归 y = A·X^B（CN-152/153）。x ≤ 0 或 y ≤ 0 → Math ERROR。 */
    private fun powerReg(data: List<Entry>): RegResult {
        val t = data.map { Entry(lnPos(it.x), lnPos(it.y!!), it.freq) }
        val n = count(t)
        val su = sumX(t); val suu = sumX2(t)
        val sv = sumY(t); val svv = sumY2(t)
        val suv = sumXY(t)
        val bn = n * suv - su * sv
        val bd = n * suu - su * su
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = exp((sv - b * su) / n)
        val rd = (n * suu - su * su) * (n * svv - sv * sv)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, bn / sqrt(rd))
    }

    /** 倒数回归 y = A + B/X（CN-153）。x = 0 → Math ERROR。 */
    private fun recipReg(data: List<Entry>): RegResult {
        val tx = data.map { Entry(recipNz(it.x), it.y, it.freq) }
        val n = count(tx)
        val su = sumX(tx); val suu = sumX2(tx)
        val sy = sumY(tx); val syy = sumY2(tx)
        val suy = sumXY(tx)
        val sxx = suu - su * su / n
        if (sxx == 0.0) mathErr("除以 0")
        val syy_ = syy - sy * sy / n
        val sxy_ = suy - su * sy / n
        val b = sxy_ / sxx
        val a = sy / n - b * su / n
        if (syy_ <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, sxy_ / sqrt(sxx * syy_))
    }

    // ---------- 估计值 x̂ / ŷ（CN-146~160） ----------

    /** ŷ = 回归估计 y。定义域违规 → Math ERROR。 */
    fun estimateY(type: RegType, reg: RegResult, x: Double): Double = when (type) {
        RegType.LINEAR -> reg.a + reg.b * x
        RegType.QUADRATIC -> reg.a + reg.b * x + reg.c!! * x * x
        RegType.LOG -> {
            if (x <= 0) mathErr("ln 定义域")
            reg.a + reg.b * ln(x)
        }
        RegType.EXP -> reg.a * exp(reg.b * x)
        RegType.AB_EXP -> reg.a * reg.b.pow(x)
        RegType.POWER -> {
            if (x <= 0) mathErr("ln 定义域")
            reg.a * x.pow(reg.b)
        }
        RegType.RECIPROCAL -> {
            if (x == 0.0) mathErr("除以 0")
            reg.a + reg.b / x
        }
    }

    /** x̂ = 回归估计 x（二次回归有两个根，用 estimateXQuadratic）。 */
    fun estimateX(type: RegType, reg: RegResult, y: Double): Double {
        if (type == RegType.QUADRATIC) mathErr("二次回归有两个 x̂，请用 estimateXQuadratic")
        if (reg.b == 0.0) mathErr("除以 0")
        return when (type) {
            RegType.LINEAR -> (y - reg.a) / reg.b
            RegType.LOG -> exp((y - reg.a) / reg.b)
            RegType.EXP -> {
                if (y <= 0 || reg.a <= 0) mathErr("ln 定义域")
                (ln(y) - ln(reg.a)) / reg.b
            }
            RegType.AB_EXP -> {
                if (y <= 0 || reg.a <= 0 || reg.b <= 0 || reg.b == 1.0) mathErr("ln 定义域")
                (ln(y) - ln(reg.a)) / ln(reg.b)
            }
            RegType.POWER -> {
                if (y <= 0 || reg.a <= 0) mathErr("ln 定义域")
                exp((ln(y) - ln(reg.a)) / reg.b)
            }
            RegType.RECIPROCAL -> {
                val d = y - reg.a
                if (d == 0.0) mathErr("除以 0")
                reg.b / d
            }
            RegType.QUADRATIC -> throw AssertionError("unreachable")
        }
    }

    /** 二次回归的两个 x̂（CN-149）：x̂1,2 = (−B±√(B²−4C(A−y)))/(2C)。C=0 或判别式<0 → MATH。 */
    fun estimateXQuadratic(reg: RegResult, y: Double): Pair<Double, Double> {
        val c = reg.c!!
        if (c == 0.0) mathErr("除以 0")
        val disc = reg.b * reg.b - 4 * c * (reg.a - y)
        if (disc < 0) mathErr("判别式为负")
        val sq = sqrt(disc)
        return ((-reg.b + sq) / (2 * c)) to ((-reg.b - sq) / (2 * c))
    }

    // ---------- 内部 ----------

    private fun sumW(data: List<Entry>, selector: Entry.() -> Double): Double {
        check1(data)
        return data.sumOf { it.selector() * it.freq }
    }

    private fun sumW2(data: List<Entry>, selector: Entry.() -> Double): Double {
        check2(data)
        return data.sumOf { it.selector() * it.freq }
    }

    private fun lnPos(v: Double): Double {
        if (v <= 0) mathErr("ln 定义域")
        return ln(v)
    }

    private fun recipNz(v: Double): Double {
        if (v == 0.0) mathErr("除以 0")
        return 1 / v
    }

    private fun check1(data: List<Entry>) {
        if (data.isEmpty()) mathErr("无数据")
        if (data.any { it.freq < 0 }) mathErr("频率不得为负")
    }

    private fun check2(data: List<Entry>) {
        check1(data)
        if (data.any { it.y == null }) mathErr("需要成对数据")
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
```

- [ ] **Step 2: StatTest.kt**

```kotlin
package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class StatTest {

    // ---- 例4-6（L4231-4298）：1-VAR + FREQ ----
    private val oneVarData = listOf(
        Stat.Entry(0.0, freq = 1.0), Stat.Entry(1.0, freq = 2.0), Stat.Entry(2.0, freq = 1.0),
        Stat.Entry(3.0, freq = 2.0), Stat.Entry(4.0, freq = 2.0), Stat.Entry(5.0, freq = 2.0),
        Stat.Entry(6.0, freq = 3.0), Stat.Entry(7.0, freq = 4.0), Stat.Entry(9.0, freq = 2.0),
        Stat.Entry(10.0, freq = 1.0)
    )

    @Test
    fun `manual one var with freq`() {
        assertEquals(20.0, Stat.count(oneVarData), 0.0)          // 原文 L4276
        assertEquals(102.0, Stat.sumX(oneVarData), 1e-12)        // 原文 L4254
        assertEquals(672.0, Stat.sumX2(oneVarData), 1e-12)       // 原文 L4246
        assertEquals(5.1, Stat.meanX(oneVarData), 1e-12)         // 原文 L4284
        assertEquals(2.754995463, Stat.stdXn(oneVarData), 1e-9)  // 原文 L4292
        assertEquals(2.8265657049165736, Stat.stdXn1(oneVarData), 1e-12)
        assertEquals(0.0, Stat.minX(oneVarData), 0.0)
        assertEquals(10.0, Stat.maxX(oneVarData), 0.0)
    }

    // ---- 例7-10（L4347-4446）：线性回归数据 ----
    private val linearData = listOf(
        Stat.Entry(1.0, 1.0), Stat.Entry(1.2, 1.1), Stat.Entry(1.5, 1.2),
        Stat.Entry(1.6, 1.3), Stat.Entry(1.9, 1.4), Stat.Entry(2.1, 1.5),
        Stat.Entry(2.4, 1.6), Stat.Entry(2.5, 1.7), Stat.Entry(2.7, 1.8),
        Stat.Entry(3.0, 2.0)
    )

    @Test
    fun `two var sums and stats`() {
        assertEquals(10.0, Stat.count(linearData), 0.0)
        assertEquals(19.9, Stat.sumX(linearData), 1e-12)
        assertEquals(14.6, Stat.sumY(linearData), 1e-12)
        assertEquals(0.63, Stat.stdXn(linearData), 1e-12)        // 例8 原文 ≈0.63
        assertEquals(0.6640783086353597, Stat.stdXn1(linearData), 1e-12)
        assertEquals(1.99, Stat.meanX(linearData), 1e-12)
        assertEquals(1.46, Stat.meanY(linearData), 1e-12)
        assertEquals(102.45100000000001, Stat.sumX3(linearData), 1e-9)
        assertEquals(71.244, Stat.sumX2Y(linearData), 1e-9)
        assertEquals(253.5541, Stat.sumX4(linearData), 1e-9)
        assertEquals(1.0, Stat.minX(linearData), 0.0)
        assertEquals(3.0, Stat.maxX(linearData), 0.0)
        assertEquals(2.0, Stat.maxY(linearData), 0.0)            // 原文 L4408
    }

    @Test
    fun `manual linear regression`() {
        val r = Stat.regress(Stat.RegType.LINEAR, linearData)
        assertEquals(0.5043587805492551, r.a, 1e-12)
        assertEquals(0.4802217183, r.b, 1e-12)                   // 原文 L4428
        assertEquals(0.9952824846, r.r!!, 1e-12)                 // 原文 L4432
        assertNull(r.c)
        assertEquals(-7.297376705, Stat.estimateX(Stat.RegType.LINEAR, r, -3.0), 1e-9)  // 原文 L4440
        assertEquals(1.464802217, Stat.estimateY(Stat.RegType.LINEAR, r, 2.0), 1e-9)    // 原文 L4446
    }

    // ---- 例11-13（L4529-4580）：二次回归 ----

    @Test
    fun `manual quadratic regression`() {
        val r = Stat.regress(Stat.RegType.QUADRATIC, linearData)
        assertEquals(0.7028598638, r.a, 1e-9)                    // 原文 L4529
        assertEquals(0.25763843788924545, r.b, 1e-12)
        assertEquals(0.05610274152791289, r.c!!, 1e-12)
        assertNull(r.r)                                          // 二次回归无 r（CN-149）
        val (x1, x2) = Stat.estimateXQuadratic(r, 3.0)
        assertEquals(4.502211457, x1, 1e-9)                      // 原文 L4552
        assertEquals(-9.094472563, x2, 1e-9)                     // 原文 L4580
        assertEquals(1.442547706, Stat.estimateY(Stat.RegType.QUADRATIC, r, 2.0), 1e-9)
    }

    // ---- 例15（L4741-4839）：对数回归 y = A + B·ln x ----
    private val logData = listOf(
        Stat.Entry(29.0, 1.6), Stat.Entry(50.0, 23.5), Stat.Entry(74.0, 38.0),
        Stat.Entry(103.0, 46.4), Stat.Entry(118.0, 48.9)
    )

    @Test
    fun `manual log regression`() {
        // 答案为截图；期望值为公式体系参考值
        val r = Stat.regress(Stat.RegType.LOG, logData)
        assertEquals(-111.1283976473655, r.a, 1e-9)
        assertEquals(34.02014750160489, r.b, 1e-9)
        assertEquals(0.994013946616563, r.r!!, 1e-12)
        assertEquals(37.94879482020123, Stat.estimateY(Stat.RegType.LOG, r, 80.0), 1e-9)
        assertEquals(224.15413126072139, Stat.estimateX(Stat.RegType.LOG, r, 73.0), 1e-6)
    }

    // ---- 例16（L4842-4926）：e 指数回归 y = A·e^(Bx) ----
    private val expData = listOf(
        Stat.Entry(6.9, 21.4), Stat.Entry(12.9, 15.7), Stat.Entry(19.8, 12.1),
        Stat.Entry(26.7, 8.5), Stat.Entry(35.1, 5.2)
    )

    @Test
    fun `manual exp regression`() {
        val r = Stat.regress(Stat.RegType.EXP, expData)
        assertEquals(30.49758743, r.a, 1e-9)                     // 原文 L4888
        assertEquals(-0.04920370830766393, r.b, 1e-12)
        assertEquals(-0.997247352, r.r!!, 1e-9)                  // 原文 L4903
        assertEquals(13.87915739, Stat.estimateY(Stat.RegType.EXP, r, 16.0), 1e-9)   // 原文 L4915
        assertEquals(8.574868047, Stat.estimateX(Stat.RegType.EXP, r, 20.0), 1e-9)   // 原文 L4926
    }

    // ---- 例17（L4928-5013）：ab 指数回归 y = A·B^x ----
    private val abData = listOf(
        Stat.Entry(-1.0, 0.24), Stat.Entry(3.0, 4.0),
        Stat.Entry(5.0, 16.2), Stat.Entry(10.0, 513.0)
    )

    @Test
    fun `manual ab exp regression`() {
        val r = Stat.regress(Stat.RegType.AB_EXP, abData)
        assertEquals(0.48886664, r.a, 1e-9)                      // 原文 L4976
        assertEquals(2.0074993437791706, r.b, 1e-12)
        assertEquals(0.9999873551795408, r.r!!, 1e-12)
        assertEquals(1.055357865, Stat.estimateX(Stat.RegType.AB_EXP, r, 1.02), 1e-9)  // 原文 L5013
        assertEquals(16944.2200173692, Stat.estimateY(Stat.RegType.AB_EXP, r, 15.0), 1e-6)
    }

    // ---- 例18（L5016-5068）：幂回归 y = A·x^B ----
    private val powerData = listOf(
        Stat.Entry(28.0, 2410.0), Stat.Entry(30.0, 3033.0), Stat.Entry(33.0, 3895.0),
        Stat.Entry(35.0, 4491.0), Stat.Entry(38.0, 5717.0)
    )

    @Test
    fun `manual power regression`() {
        val r = Stat.regress(Stat.RegType.POWER, powerData)
        assertEquals(0.23880106853373598, r.a, 1e-12)
        assertEquals(2.771866158, r.b, 1e-9)                     // 原文 L5057
        assertEquals(0.9989062551, r.r!!, 1e-9)                  // 原文 L5061
        assertEquals(6587.674589, Stat.estimateY(Stat.RegType.POWER, r, 40.0), 1e-6)   // 原文 L5065
        assertEquals(20.262256810920633, Stat.estimateX(Stat.RegType.POWER, r, 1000.0), 1e-9)
    }

    // ---- 例19（L5071-5162）：倒数回归 y = A + B/x ----
    private val recipData = listOf(
        Stat.Entry(1.1, 18.3), Stat.Entry(2.1, 9.7), Stat.Entry(2.9, 6.8),
        Stat.Entry(4.0, 4.9), Stat.Entry(4.9, 4.1)
    )

    @Test
    fun `manual reciprocal regression`() {
        val r = Stat.regress(Stat.RegType.RECIPROCAL, recipData)
        assertEquals(-0.09344061817312763, r.a, 1e-12)
        assertEquals(20.267097114570788, r.b, 1e-12)
        assertEquals(0.9998526952656159, r.r!!, 1e-12)
        assertEquals(5.697158557, Stat.estimateY(Stat.RegType.RECIPROCAL, r, 3.5), 1e-9)  // 原文 L5152
        assertEquals(1.342775158, Stat.estimateX(Stat.RegType.RECIPROCAL, r, 15.0), 1e-9)  // 原文 L5154
    }

    // ---- 错误条件 ----

    @Test
    fun `error conditions`() {
        // 空数据
        assertThrows(CalcException::class.java) { Stat.count(emptyList()) }
        assertThrows(CalcException::class.java) { Stat.regress(Stat.RegType.LINEAR, emptyList()) }
        // 样本数不足（xσn−1 需 n>1）
        assertThrows(CalcException::class.java) { Stat.stdXn1(listOf(Stat.Entry(5.0))) }
        // 负频率
        assertThrows(CalcException::class.java) { Stat.count(listOf(Stat.Entry(5.0, freq = -1.0))) }
        // 1-VAR 数据用于回归（y 缺失）
        assertThrows(CalcException::class.java) { Stat.sumY(oneVarData) }
        // 对数/幂回归定义域：x ≤ 0 或 y ≤ 0
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.LOG, listOf(Stat.Entry(-1.0, 1.0), Stat.Entry(2.0, 2.0)))
        }
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.EXP, listOf(Stat.Entry(1.0, -1.0), Stat.Entry(2.0, 2.0)))
        }
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.POWER, listOf(Stat.Entry(1.0, 1.0), Stat.Entry(0.0, 2.0)))
        }
        // 倒数回归 x = 0
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.RECIPROCAL, listOf(Stat.Entry(0.0, 1.0), Stat.Entry(2.0, 2.0)))
        }
        // 线性回归 x 全部相同 → 分母为 0
        assertThrows(CalcException::class.java) {
            Stat.regress(Stat.RegType.LINEAR, listOf(Stat.Entry(1.0, 1.0), Stat.Entry(1.0, 2.0)))
        }
        // 二次回归判别式为负
        val qr = Stat.regress(Stat.RegType.QUADRATIC, linearData)
        assertThrows(CalcException::class.java) { Stat.estimateXQuadratic(qr, -100.0) }
        // estimateX 对二次回归报错
        assertThrows(CalcException::class.java) { Stat.estimateX(Stat.RegType.QUADRATIC, qr, 3.0) }
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.core.finance.StatTest"
```

预期：`BUILD SUCCESSFUL`，11 个测试全过。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/core/finance/ app/src/test/java/com/fincalc/app/core/finance/
git commit -m "feat(core/finance): STAT 统计（1-VAR + 7 种回归 + x̂/ŷ 估计）"
```

---

### Task 6: 收尾验证（本计划验收点，无新文件、无提交）

- [ ] **Step 1: 全量单元测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest
```

预期：`BUILD SUCCESSFUL`；全部测试类 0 失败 0 错误（预期总测试数 134 + 10 + 7 + 8 + 8 + 11 = 178）。

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

预期：5 个 feat(core/finance) 提交依次在案；`git status --short` 为空。

---

## 完成标准（计划 4 验收）

- [ ] `./gradlew testDebugUnitTest` 全绿（预期 178 测：计划 3 末 134 + 新增 44）
- [ ] core 包无 `import android.*`，纯 JVM 可测
- [ ] 说明书例题验收：DAYS 例1（Dys=173 原文）、DEPR 四法（SL₃=25000 原文 + 三个参考值锚）、BOND 例1（PRC=−97.6151555 原文）、BEVN 全部（设定值表原文值）、STAT（1-VAR 例4-6 原文值 + 线性回归 B/r/x̂/ŷ 原文值 + 二次回归 A/x̂1/x̂2/ŷ 原文值 + e 指数 A/r/ŷ/x̂ 原文值 + ab 指数 A/x̂ 原文值 + 幂 B/r/ŷ 原文值 + 倒数 ŷ/x̂ 原文值）
- [ ] 错误条件覆盖：非法日期/超范围、DEPR 全错误表（CN-168）、BOND 全错误表、BEVN 除零、STAT 定义域/空数据/判别式
- [ ] 提交历史清晰（5 个 feat 提交），工作区干净
