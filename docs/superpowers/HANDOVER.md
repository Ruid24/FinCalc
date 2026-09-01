# 交接文档（HANDOVER）—— FinCalc 项目跨电脑迁移

日期：2026-08-19
原因：笔记本电量告急，迁移到台式机继续开发。

## 这是什么项目

对标卡西欧 FC-200V 的开源安卓金融计算器（Kotlin + Jetpack Compose，12 模式全量，LaTeX 自然显示）。完整背景见：

- 设计文档：`docs/superpowers/specs/2026-08-19-fc200v-financial-calculator-design.md`
- 计划 1（当前执行中）：`docs/superpowers/plans/2026-08-19-fincalc-01-scaffold-ci.md`
- 说明书资料：`说明书/`（FC-200V PDF 原件 + MinerU OCR markdown）

## 当前进度状态

**截至 2026-09-01：计划 1-6 全部完成，项目功能全量可用。** 12 模式引擎（计划 2-4：core/expr 表达式引擎 + core/solver 求解器 + core/finance 金融引擎 6+5 object）+ UI（计划 5-6：自研排版器、COMP 端到端、金融框架 + 11 模式界面、CASH/STAT 编辑器、光标系统、存储器、学习辅助、DataStore 持久化、双语）。全量 222 测 0 败。

关键过程资产：设计文档 `docs/superpowers/specs/`、计划 1-6 `docs/superpowers/plans/`（各含执行期修订记录）、说明书 OCR `说明书/`。重大技术决策变更：AndroidMath 库不可用（GitHub 下架），经用户确认改为自研 Compose 排版器（core/render + ui/math）。

GitHub 推送：用户推迟，随时可发起（git remote add origin https://github.com/Ruid24/FinCalc.git → push -u origin main）。

- [x] Task 1: `.gitignore`（含 `.dev/` 排除）+ git 本地身份（Ruid / ruid@localhost），已提交（e286859 之后还有文档提交）
- [x] Task 2: `.dev/jdk` = Temurin JDK 17.0.20+8，`.dev/env.sh` 已创建
- [x] Task 3: `.dev/android-sdk`（cmdline-tools/latest 结构 + platform-tools + platforms;android-34 + build-tools;34.0.0，许可证已接受）
- [x] Task 4: `.dev/gradle-8.9` + 项目根目录 gradle wrapper（`gradlew`、`gradlew.bat`、`gradle/wrapper/*`、单行版 `settings.gradle.kts`）。已验证 `./gradlew --version` = Gradle 8.9 / JVM 17。**Task 4 的 spec/quality 审查已由控制器直接验证通过，但未做独立子代理复审（电量原因从简）**
- [x] Task 5-9（台式机完成）：每任务均按 subagent-driven-development 走完"实现子代理 → 规格符合性审查 → 代码质量审查"，全部 PASS
- [x] 构建验收：`testDebugUnitTest` 全绿；`assembleDebug` 产出 9.4MB 调试 APK（apksigner 验证已签名，可安装）；`assembleRelease` 产物名与 CI 工作流引用一致
- [x] LICENSE 版权人已替换为 GitHub 用户名 **Ruid24**（提交 e8bcd75）
- [ ] Task 10 推送（用户推迟）：用户建公开空仓库 `FinCalc` → `git remote add origin https://github.com/Ruid24/FinCalc.git` → `git push -u origin main`（credential.helper=manager，推送时会弹浏览器授权窗口）；之后到 Actions 页确认 CI 绿

台式机新增提交（从旧到新）：`ecab392` 脚手架 → `b7fcf56` 非 ASCII 路径修复 → `65cf6db` gradlew 可执行位 → `198a7b2` CI 工作流 → `472b94f` LICENSE+README → `e8bcd75` LICENSE 版权人。**工作区干净，无未跟踪文件。**

## 本机环境怪癖（台式机可能也有，注意）

1. **curl 需要 `--ssl-no-revoke`**：本机 schannel 报 `CRYPT_E_REVOCATION_OFFLINE`，所有 curl 下载都要加此参数。
2. **Java 进程访问 services.gradle.org 超时**（curl 正常，疑似系统代理未被 Java 使用）。已采取的缓解：把 `.dev/gradle-8.9` 复制到了 `~/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/` 并创建了 `.ok` 标记，使 `./gradlew` 免下载。**迁移时这个 `~/.gradle` 缓存在工作区之外，需要单独处理（见下文）**。
3. **最大潜在风险**：Task 7 的 `./gradlew build` 需要从 google()/mavenCentral() 下载 AGP/Kotlin/Compose 依赖。若台式机 Java 也网络异常，需配置 Gradle 代理（`~/.gradle/gradle.properties` 加 `systemProp.https.proxyHost` 等）或用镜像仓库。
4. `gradle/wrapper/gradle-wrapper.properties` 多一行 `validateDistributionUrl=false`（无害，本机反而需要）。
5. Windows 版 build-tools 34 的 d8 是 `d8.bat` 不是 `d8.exe`（计划文本笔误，已确认无碍）。
6. Git Bash 的 `tar` 不能解 zip，用 `unzip`。

## 台式机环境实测（2026-08-19 补充）

7. **网络正常**：Java 进程可直连 dl.google.com / repo.maven.apache.org / github.com（均 200、<1s），Gradle 无需配代理。系统存在代理 127.0.0.1:7897，但 Git Bash 的 curl/git 均未使用，直连即可。笔记本上的"Java 无法直连"怪癖在台式机不存在。
8. **Bash 工具传输层折叠反斜杠**：命令里写含 `\` 的字面量（如 sed 表达式）要翻倍书写才能等价到达 bash（`s/\\/\\\\/g` 须写成 `s/\\\\/\\\\\\\\/g`），必要时再加 `MSYS2_ARG_CONV_EXCL='*'` 防路径转换。
9. `gradle.properties` 已含 `android.overridePathCheck=true`（中文路径下一切 gradle 命令的前提，提交 b7fcf56）；`gradlew` 已是 100755 可执行（提交 65cf6db），Linux CI 可直接运行。

## 迁移包内容

- `FinCalc-migrate-2026-08-19.tar`：整个工作区（含 `.git`、`.dev/` 工具链约 2GB、所有文档与代码）
- `gradle-wrapper-cache.tar`：`~/.gradle/wrapper/dists/gradle-8.9-bin` 缓存（让台式机 `./gradlew` 免下载 Gradle 本体）

## 台式机恢复步骤

1. 解压工作区到任意目录（路径可含中文，不影响）：
   ```bash
   tar -xf FinCalc-migrate-2026-08-19.tar -C <目标父目录>
   ```
   得到 `<目标父目录>/金融计算器/`（如在 tar 内是此名）。**Windows 10/11 自带的资源管理器或 `tar` 命令都能解。**
2. 恢复 Gradle wrapper 缓存（可选但强烈建议，避开 Java 下载超时问题）：
   ```bash
   mkdir -p ~/.gradle/wrapper/dists
   tar -xf gradle-wrapper-cache.tar -C ~/.gradle/wrapper/dists
   ```
3. 进入工作区验证：
   ```bash
   cd <工作区>
   source .dev/env.sh
   java -version        # 应显示 17.0.20 Temurin
   ./gradlew --version  # 应显示 Gradle 8.9，且秒回（用了缓存）
   git log --oneline    # 历史应完整
   ```
   `env.sh` 用 `cygpath -w` 动态生成路径，换电脑换目录都自适应，无需修改。
4. 若 `./gradlew --version` 卡在下载：说明缓存没生效，用 `curl -L --ssl-no-revoke -o gradle-8.9-bin.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip` 手动下载，解压到 `~/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9`，并在该目录创建空文件 `gradle-8.9-bin.zip.ok`。

## 给台式机 AI agent 的续接 Prompt（原样复制给新的 Kimi Code 会话）

```text
你正在接续另一台电脑上未完成的工作。请先阅读交接文档 docs/superpowers/HANDOVER.md 了解项目背景、进度与环境怪癖，然后执行以下步骤：

1. 验证环境：source .dev/env.sh && java -version && ./gradlew --version（应显示 JDK 17.0.20 / Gradle 8.9）。若 gradlew 卡下载，按交接文档"恢复步骤 4"处理。
2. 项目是按 superpowers 流程进行的：设计文档在 docs/superpowers/specs/，计划 1 在 docs/superpowers/plans/2026-08-19-fincalc-01-scaffold-ci.md。
3. 计划 1 的 Task 1-4 已完成（细节见交接文档）。请用 subagent-driven-development 流程继续执行 Task 5 到 Task 10：每个任务派发独立实现子代理（把计划里该任务的完整文本粘贴进 prompt，不要让子代理自己去读计划文件），然后依次做规格符合性审查和代码质量审查。Task 10（创建 GitHub 仓库并推送）需要用户配合凭据，到时询问用户。
4. 全部完成后做整体最终代码审查，并向用户汇报计划 1 验收结果。
5. 环境怪癖（重要）：curl 下载一律加 --ssl-no-revoke；本机 Java 进程可能无法直连外网（curl 正常），若 ./gradlew build 下载 Maven 依赖失败，考虑给 Gradle 配代理或镜像。
6. 请用中文与我交流。
```

## 计划 1 之后的路线图（提醒）

计划 2-5 尚未编写：表达式引擎+LaTeX 排版器 → 金融引擎第一批 → 第二批 → UI 全量。计划 1 验收后按 writing-plans 技能依次编写。
