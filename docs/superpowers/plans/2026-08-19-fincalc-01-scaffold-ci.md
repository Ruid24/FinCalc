# 计划 1：开发环境 + 项目脚手架 + CI/CD 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Windows 本机（无 Java/Android 环境）搭建免安装命令行工具链，创建 Kotlin + Jetpack Compose 安卓项目骨架，本机构建出可安装的调试 APK，并配置 GitHub Actions 云端自动构建与 Release 发布。

**Architecture:** 单模块 Gradle 项目（`:app`），Kotlin 2.0 + AGP 8.5 + Jetpack Compose (Material 3)。工具链全部安装在项目目录 `.dev/` 内（JDK 17 + Android SDK cmdline-tools + Gradle），不进系统目录、无需管理员权限，删除 `.dev/` 即完全卸载。

**Tech Stack:** JDK 17 (Temurin)、Android SDK 34 + build-tools 34.0.0、Gradle 8.9、AGP 8.5.2、Kotlin 2.0.20、Compose BOM 2024.09.00、JUnit 4。

**路线图（本计划为第 1 个，后续计划另行编写）：**
1. 计划 1（本文档）：环境 + 脚手架 + CI/CD
2. 计划 2：`core/expr` 表达式引擎 + LaTeX 排版器 + `core/solver` 数值求解器（纯 Kotlin，TDD）
3. 计划 3：金融引擎第一批（SMPL/CMPD/CASH/AMRT/CNVR/COST）
4. 计划 4：金融引擎第二批（DAYS/DEPR/BOND/BEVN/STAT）
5. 计划 5：UI 全量（仿真键盘、高分辨率 LaTeX 显示屏、12 模式界面、双语）

**环境前提：** Windows + Git Bash；所有命令在项目根目录 `C:/Users/Ruid/Documents/文档/本科3_暑假/金融计算器` 下执行；每次新开 shell 需先执行 `source .dev/env.sh`（Task 2 创建）。

---

### Task 1: 仓库卫生（.gitignore + git 本地配置）

**Files:**
- Create: `.gitignore`

- [ ] **Step 1: 配置 git 本地身份（仅本仓库生效）**

```bash
git config user.name "Ruid"
git config user.email "ruid@localhost"
```

- [ ] **Step 2: 编写 .gitignore**

```gitignore
# 本地工具链（约 2GB，永不上传）
.dev/
# Android/Gradle
local.properties
.gradle/
**/build/
*.apk
*.keystore
# IDE
.idea/
*.iml
```

- [ ] **Step 3: 提交**

```bash
git add .gitignore
git commit -m "chore: 添加 .gitignore（排除 .dev 工具链与构建产物）"
```

预期输出：`1 file changed`。

---

### Task 2: 安装 JDK 17 到 .dev/ 并创建环境脚本

**Files:**
- Create: `.dev/env.sh`
- Create: `.dev/jdk/`（下载解压产生）

- [ ] **Step 1: 下载 Temurin JDK 17（Windows x64 zip）**

```bash
mkdir -p .dev
curl -L -o .dev/jdk17.zip "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
```

预期：`.dev/jdk17.zip` 约 180MB。若 Adoptium 下载失败，备用地址：`https://download.oracle.com/java/17/archive/jdk-17.0.12_windows-x64_bin.zip`。

- [ ] **Step 2: 解压并重命名为 .dev/jdk**

```bash
cd .dev && tar -xf jdk17.zip && mv jdk-17* jdk && rm jdk17.zip && cd ..
```

- [ ] **Step 3: 创建环境脚本 .dev/env.sh**

```bash
cat > .dev/env.sh << 'EOF'
# 用法：source .dev/env.sh（每次新开 Git Bash 都要执行一次）
export DEVROOT="$PWD/.dev"
export JAVA_HOME="$(cygpath -w "$DEVROOT/jdk")"
export ANDROID_HOME="$DEVROOT/android-sdk"
export PATH="$DEVROOT/jdk/bin:$DEVROOT/gradle-8.9/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF
source .dev/env.sh
```

说明：`JAVA_HOME` 用 `cygpath -w` 转成 Windows 形式（`C:\...`），Gradle/sdkmanager 的脚本在 Git Bash 下对这种形式兼容最好。

- [ ] **Step 4: 验证 Java**

```bash
java -version
```

预期输出包含：`openjdk version "17.`（Temurin）。

---

### Task 3: 安装 Android SDK 命令行工具与平台包

**Files:**
- Create: `.dev/android-sdk/`（下载解压产生）

- [ ] **Step 1: 下载并解压 cmdline-tools（注意必须放在 cmdline-tools/latest/ 子目录）**

```bash
source .dev/env.sh
curl -L -o .dev/cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
mkdir -p .dev/android-sdk/cmdline-tools
cd .dev/android-sdk/cmdline-tools && tar -xf ../../cmdtools.zip && mv cmdline-tools latest && cd ../../.. && rm .dev/cmdtools.zip
```

- [ ] **Step 2: 接受许可证**

```bash
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager.bat" --sdk_root="$(cygpath -w "$ANDROID_HOME")" --licenses > /dev/null 2>&1
```

说明：`yes` 不断输入 `y` 自动接受全部许可证；输出较长故丢弃。此命令失败不会中断后续，Step 3 成功即代表许可证已接受。

- [ ] **Step 3: 安装平台包**

```bash
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager.bat" --sdk_root="$(cygpath -w "$ANDROID_HOME")" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

预期输出结尾：`[=======================================] 100%` 且无 `Warning: Failed to find package`。

- [ ] **Step 4: 验证**

```bash
ls "$ANDROID_HOME/platforms/android-34/android.jar" "$ANDROID_HOME/build-tools/34.0.0/d8.exe" "$ANDROID_HOME/platform-tools/adb.exe"
```

预期：三个路径都列出，无 `No such file or directory`。

---

### Task 4: 安装 Gradle 8.9 并生成 Wrapper

**Files:**
- Create: `.dev/gradle-8.9/`（下载解压产生）
- Create: `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: 下载解压 Gradle**

```bash
source .dev/env.sh
curl -L -o .dev/gradle.zip "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
cd .dev && tar -xf gradle.zip && rm gradle.zip && cd ..
gradle --version
```

预期输出包含：`Gradle 8.9` 与 `JVM: 17.`。

- [ ] **Step 2: 生成 Wrapper（先创建最小 settings 文件，wrapper 任务需要项目存在）**

```bash
printf 'rootProject.name = "FinCalc"\n' > settings.gradle.kts
gradle wrapper --gradle-version 8.9 --distribution-type bin
./gradlew --version
```

预期：`./gradlew --version` 输出 `Gradle 8.9`。

---

### Task 5: Gradle 构建脚本（根项目 + app 模块）

**Files:**
- Modify: `settings.gradle.kts`（Task 4 生成的单行文件，替换为完整内容）
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `local.properties`（已被 .gitignore 排除）

- [ ] **Step 1: settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FinCalc"
include(":app")
```

- [ ] **Step 2: 根 build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
```

- [ ] **Step 3: gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 4: app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fincalc.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fincalc.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 5: 生成 local.properties（指向 .dev 内的 SDK，已 gitignore）**

```bash
source .dev/env.sh
printf 'sdk.dir=%s\n' "$(cygpath -w "$ANDROID_HOME" | sed 's/\\/\\\\/g')" > local.properties
cat local.properties
```

预期输出形如：`sdk.dir=C:\\Users\\Ruid\\...\\.dev\\android-sdk`

---

### Task 6: 应用骨架（Manifest + MainActivity + 主题 + 双语字符串）

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/fincalc/app/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-zh-rCN/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/test/java/com/fincalc/app/SanityTest.kt`

- [ ] **Step 1: AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="@string/app_name"
        android:theme="@style/Theme.FinCalc"
        android:allowBackup="true"
        android:supportsRtl="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 2: MainActivity.kt（占位首页，证明 Compose 可用）**

```kotlin
package com.fincalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaceholderScreen()
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.placeholder_title))
    }
}
```

- [ ] **Step 3: 双语字符串（values/ 为英文回退，values-zh-rCN/ 为中文；应用内语言切换在计划 5 实现）**

`app/src/main/res/values/strings.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FinCalc</string>
    <string name="placeholder_title">FinCalc — FC-200V style financial calculator (under construction)</string>
</resources>
```

`app/src/main/res/values-zh-rCN/strings.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FinCalc</string>
    <string name="placeholder_title">FinCalc —— 对标 FC-200V 的金融计算器（建设中）</string>
</resources>
```

- [ ] **Step 4: themes.xml（Compose 项目用 NoActionBar 主题即可）**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FinCalc" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 5: SanityTest.kt（验证 JVM 单元测试管线，后续真正的核心引擎测试从计划 2 开始）**

```kotlin
package com.fincalc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SanityTest {
    @Test
    fun `test pipeline works`() {
        assertEquals(4, 2 + 2)
    }
}
```

---

### Task 7: 首次构建与验证（本计划的验收点）

- [ ] **Step 1: 单元测试**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest
```

预期输出：`BUILD SUCCESSFUL`，且 `app/build/test-results/testDebugUnitTest/` 下生成 XML 报告。

- [ ] **Step 2: 构建调试 APK**

```bash
./gradlew assembleDebug
ls -la app/build/outputs/apk/debug/app-debug.apk
```

预期：`BUILD SUCCESSFUL` 且 APK 文件存在（约 3-8MB）。

- [ ] **Step 3: 提交**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/ app/
git commit -m "feat: 安卓项目脚手架（Kotlin 2.0 + Compose），本机构建通过"
```

---

### Task 8: GitHub Actions（云端构建 APK + 打标签自动发布 Release）

**Files:**
- Create: `.github/workflows/android.yml`

- [ ] **Step 1: 编写工作流**

```yaml
name: Android CI

on:
  push:
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run unit tests
        run: ./gradlew testDebugUnitTest

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk

  release:
    if: startsWith(github.ref, 'refs/tags/v')
    needs: build
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build release APK
        run: ./gradlew assembleRelease

      - name: Publish to GitHub Releases
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/app-release-unsigned.apk
          generate_release_notes: true
```

说明：Release APK 为未签名包（GitHub Actions 无法持有私人签名密钥）；debug 构建由 Gradle 自动用调试密钥签名可直接安装。对用户而言：每次 push 可在 Actions 页面下载 debug APK；打 `v*` 标签自动生成 Release 页面。正式发布的 APK 签名问题在计划 5 收尾阶段处理（届时用本地调试签名 APK 或配置签名密钥）。

- [ ] **Step 2: 提交**

```bash
git add .github/workflows/android.yml
git commit -m "ci: GitHub Actions 构建 APK，打 v* 标签自动发 Release"
```

---

### Task 9: MIT 许可证 + 双语 README 骨架

**Files:**
- Create: `LICENSE`
- Create: `README.md`

- [ ] **Step 1: LICENSE（MIT，版权人写 GitHub 用户名占位——推送前替换为真实用户名）**

```text
MIT License

Copyright (c) 2026 FinCalc contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 2: README.md（双语骨架，截图与完整功能列表在计划 5 补全）**

```markdown
# FinCalc

[English](#english) | [中文](#中文)

## 中文

对标卡西欧 FC-200V 的开源安卓金融计算器，支持 12 种计算模式（TVM 复利、现金流 NPV/IRR、摊销、债券、折旧、损益分析、统计回归等），输入公式实时 LaTeX 自然排版显示。

- 状态：开发中（脚手架阶段）
- 构建：Android Studio 打开本仓库，或命令行 `./gradlew assembleDebug`
- 许可：MIT

## English

An open-source Android financial calculator modeled after the Casio FC-200V, with 12 calculation modes (TVM, cash-flow NPV/IRR, amortization, bonds, depreciation, break-even analysis, statistics/regression) and real-time LaTeX-typeset natural display of expressions.

- Status: work in progress (scaffolding)
- Build: open in Android Studio, or `./gradlew assembleDebug`
- License: MIT
```

- [ ] **Step 3: 提交**

```bash
git add LICENSE README.md
git commit -m "docs: MIT 许可证与双语 README 骨架"
```

---

### Task 10: 创建 GitHub 远程仓库并首次推送（需要用户配合）

- [ ] **Step 1: 用户创建 GitHub 仓库**

让用户在 GitHub 网页创建名为 `FinCalc` 的**公开空仓库**（不要勾选自动生成 README/LICENSE），或若本机 `gh` 已登录则执行：

```bash
gh repo create FinCalc --public --source=. --remote=origin
```

- [ ] **Step 2: 关联远程并推送**

```bash
git remote add origin "https://github.com/<用户名>/FinCalc.git"   # 若上一步用 gh 则跳过本行
git push -u origin main
```

注意：推送需要用户的 GitHub 凭据；若本机无凭据，由用户在弹出的登录窗口中完成，或改用 `gh auth login`。此步骤由执行者与用户协作完成，无法纯自动化。

- [ ] **Step 3: 验证云端 CI**

打开 `https://github.com/<用户名>/FinCalc/actions`，确认首次 `Android CI` 运行成功（绿勾），且 Artifacts 中出现 `app-debug`。

---

## 完成标准（计划 1 验收）

- [ ] 本机 `./gradlew assembleDebug` 成功产出 `app-debug.apk`
- [ ] 本机 `./gradlew testDebugUnitTest` 全绿
- [ ] GitHub 仓库公开，CI 绿，Artifacts 可下载 APK
- [ ] `.dev/` 工具链未被 git 追踪（`git status` 干净）
