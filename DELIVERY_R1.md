# MathCoach Android App — 第 1 轮交付说明

## 本轮已完成（P0 + P1 部分）

✅ Gradle 骨架 + Hilt + Compose + 5 个 Tab 导航
✅ Room schema + DataStore + Repository
✅ `LlmClient` + SSE 流式解析 + 6 个 Provider 配置
✅ 5 条提示词常量（3 条已改造为 GeometrySpec JSON 协议）
✅ `JsonTolerantParser` + `LatexUtils` + 单测
✅ 设置页：Provider / API Key / 模型 / 双模型开关 / 缓存开关 / 连通性测试
✅ 相册图片选择 + EXIF 旋转处理 + JPEG 压缩 + Base64 编码
✅ **拍照解题完整链路**：相册 → 流式渲染 → JSON 解析 → 自动保存历史
✅ KaTeX WebView 封装（需手动下载 KaTeX 资源到 assets）
✅ 命令行 build 文档（无需 Android Studio）
✅ CameraX 拍照按钮占位（下轮实现）

## 第 1 轮待办（用户侧）

### 1. 安装 JDK 17 + Android SDK 命令行工具

详见 [android/docs/build.md](android/docs/build.md)。

```bash
# 检查
java -version                    # 应输出 17.x
sdkmanager --version             # 应能找到
```

### 2. 配置 SDK 路径

```bash
cd D:\pythonProject\MathTools\android
copy local.properties.template local.properties
# 编辑 local.properties，把 sdk.dir 改成你的 SDK 实际路径
```

### 3. 初始化 Gradle Wrapper（只需做一次）

当前 `gradlew.bat` 是个 fallback 脚本。需要先用本机 gradle 生成官方 wrapper：

```bash
# 下载 https://services.gradle.org/distributions/gradle-8.7-bin.zip
# 解压到 D:\gradle-8.7，把 D:\gradle-8.7\bin 加入 PATH
# 然后执行：
cd D:\pythonProject\MathTools\android
gradle wrapper --gradle-version 8.7
```

完成后 `gradlew.bat` 会被官方 wrapper 覆盖，此后就可以脱离系统 Gradle 使用。

### 4. 下载 KaTeX 资源（可选但推荐）

不下载公式会以原始 LaTeX 源码显示。

```bash
# 下载 https://github.com/KaTeX/KaTeX/releases/download/v0.16.11/katex.zip
# 解压，把以下文件复制到 android\app\src\main\assets\katex\：
#   katex.min.css
#   katex.min.js
#   fonts/ 目录（全部）
```

### 5. Build & 安装

```bash
cd D:\pythonProject\MathTools\android
gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 6. 验证

1. 打开 App，进入「设置」Tab
2. 选择服务商（推荐"通义千问"或"Moonshot"），填 API Key，点"保存 Key"
3. 填主模型名（如 `qwen-vl-max` 或 `kimi-k2.6`）
4. 点"测试连接"，应看到 ✅
5. 进入「解题」Tab，点"📷 从相册选择"，选一张带数学题的图片
6. 点"开始分析"，应看到流式文本 → 解析后的卡片（含题型/难度/知识点/LaTeX 公式）
7. 退出 App 重进，「历史」Tab 仍显示"完整功能将在第 2 轮交付"（说明历史记录已写入 Room，第 2 轮会展示）

## 已知限制（下轮解决）

- ❌ CameraX 拍照（用相册代替）
- ❌ 同类题生成 UI
- ❌ 历史 Tab 完整功能
- ❌ 掌握度 / 针对性练习 / 几何图形渲染
- ❌ 双模型流程
- ⚠️ KaTeX 公式需手动下载资源文件

## 故障排查

| 问题 | 排查 |
|---|---|
| 编译错 "package xxx does not exist" | 删 `.gradle` 缓存重新 build |
| KaTeX 公式显示原始文本 | 检查 assets/katex/ 下是否有 katex.min.js/css/fonts |
| LLM 调用 401 | 检查 API Key 是否保存成功、Provider 是否选对 |
| LLM 返回无法解析 | 在「设置」里把模型换成更强的 vision 模型（qwen-vl-max / kimi-k2.6 / gpt-4o） |
| 图片不显示 | 检查相册权限，在系统设置里手动授权 |

## 文件清单（重要）

```
android/
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat             # fallback 脚本，需先 gradle wrapper 初始化
├── local.properties.template          # SDK 路径模板
├── .gitignore
├── docs/build.md                      # 详细命令行 build 指南
└── app/
    ├── build.gradle.kts               # 所有依赖版本
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/katex/render.html   # KaTeX 渲染模板
        ├── assets/katex/README.md     # KaTeX 资源下载说明
        ├── res/                       # 主题/字符串/图标
        └── java/com/mathcoach/app/
            ├── MathCoachApp.kt        # @HiltAndroidApp
            ├── MainActivity.kt        # 5 Tab NavHost
            ├── di/AppModule.kt        # Hilt DI
            ├── core/
            │   ├── llm/LlmClient.kt   # SSE 流式客户端
            │   ├── llm/Providers.kt   # 6 个服务商
            │   ├── llm/Prompts.kt     # 5 条提示词（3 条含 GeometrySpec）
            │   ├── latex/JsonTolerantParser.kt
            │   ├── latex/LatexUtils.kt
            │   ├── latex/GeometrySpecParser.kt
            │   ├── latex/ProblemAnalysisParser.kt
            │   ├── katex/KatexText.kt # WebView 封装
            │   └── util/ImageCodec.kt
            ├── data/
            │   ├── local/AppDatabase.kt
            │   ├── local/Entities.kt
            │   ├── local/Daos.kt
            │   ├── local/SettingsDataStore.kt
            │   └── repository/HistoryRepository.kt
            │   └── repository/CacheRepository.kt
            ├── domain/
            │   ├── model/Models.kt    # ProblemAnalysis / HistoryEntry / GeometrySpec
            │   ├── model/Profile.kt   # KnowledgeMastery / LearningProfile
            │   └── usecase/AnalysisUseCase.kt
            └── feature/
                ├── solve/SolveScreen.kt + SolveViewModel.kt
                ├── settings/SettingsScreen.kt + SettingsViewModel.kt
                ├── history/HistoryScreen.kt       (占位)
                ├── analytics/AnalyticsScreen.kt   (占位)
                └── practice/PracticeScreen.kt     (占位)
```
