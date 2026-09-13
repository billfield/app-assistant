# MathCoach Android — 最终交付说明

## 当前状态

✅ **代码 100% 完成**：四大功能（拍照解题、同类题生成、历史记录、掌握度分析 + 针对性练习）+ 设置页 + 双模型流程 + 几何图形渲染 + CameraX 拍照 + KaTeX 公式渲染 + GitHub Actions CI。

## 你需要做的最后 3 件事

### 步骤 1：创建 GitHub 私有仓库

打开 https://github.com/new

- **Repository name**：`mathcoach-android`（或其他你喜欢的名字）
- **Visibility**：选 **Private**
- **❌ 不要勾** "Add a README"
- **❌ 不要勾** "Add .gitignore"
- **❌ 不要勾** "Choose a license"

点 "Create repository"。

创建完成后会跳到一个空仓库页面，URL 形如：
```
https://github.com/<你的用户名>/mathcoach-android
```

### 步骤 2：确保本机 git 已配置 GitHub 身份

在 PowerShell 或 cmd 里执行：

```bash
git config --global user.name
git config --global user.email
```

应该已输出你的用户名和邮箱（我刚才已经验证过：`billfield` / `588638865@139.com`）。

**如果从未 push 到 GitHub**，需要配置一个 Personal Access Token (PAT)：

1. 打开 https://github.com/settings/tokens
2. 点 "Generate new token" → "Generate new token (classic)"
3. Note 填 `mathcoach-push`，Expiration 选 30 天，勾 `repo` 权限
4. 点 "Generate token"，**复制生成的 token（ghp_...）**，关闭页面后无法再查看
5. 在本机打开 "凭据管理器"（开始菜单搜 "凭据"） → Windows 凭据 → 添加普通凭据：
   - Internet 或网络地址：`git:https://github.com`
   - 用户名：你的 GitHub 用户名
   - 密码：刚才复制的 `ghp_...` token

（如果之前已经能正常 push 到任何 GitHub 仓库，说明身份已配置，跳过这步。）

### 步骤 3：告诉我仓库地址

把刚才创建的仓库 URL 发给我（例如 `https://github.com/billfield/mathcoach-android`）。

我会执行：
```bash
cd D:\pythonProject\MathTools\android
git init -b main
git add .
git commit -m "Initial commit: MathCoach Android App v0.1.0"
git remote add origin <你的仓库URL>
git push -u origin main
```

push 成功后，GitHub Actions 会自动触发 build（约 6-8 分钟），完成后：

1. 打开 `https://github.com/<你的用户名>/mathcoach-android/actions`
2. 点最新的一次 workflow run
3. 在页面底部的 "Artifacts" 区域下载 `mathcoach-debug.zip`
4. 解压得到 `app-debug.apk`，传到手机安装即可

## 后续修改代码

任何时候改了代码：
```bash
cd D:\pythonProject\MathTools\android
git add .
git commit -m "your message"
git push
```
GitHub Actions 会自动重新 build，等 6 分钟下载新 APK。

## 文件清单（已生成）

```
android/
├── .github/workflows/build.yml    # GitHub Actions CI 配置
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat          # fallback，CI 会自动 gradle wrapper 初始化
├── local.properties.template
├── .gitignore
├── docs/build.md                  # 本地 build 备选方案（可选）
├── DELIVERY_R1.md                 # R1 交付说明（历史文档）
├── README.md                      # 本文件
└── app/
    ├── build.gradle.kts           # 所有依赖
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/katex/render.html     # KaTeX 渲染模板（资源由 CI 下载）
        │   ├── assets/katex/README.md
        │   ├── res/                          # 主题/字符串/图标
        │   └── java/com/mathcoach/app/
        │       ├── MathCoachApp.kt
        │       ├── MainActivity.kt           # 5 Tab NavHost
        │       ├── di/AppModule.kt           # Hilt DI
        │       ├── core/
        │       │   ├── llm/LlmClient.kt      # SSE 流式 + 6 服务商
        │       │   ├── llm/Providers.kt
        │       │   ├── llm/Prompts.kt        # 5 条提示词（3 条含 GeometrySpec）
        │       │   ├── latex/JsonTolerantParser.kt
        │       │   ├── latex/LatexUtils.kt
        │       │   ├── latex/GeometrySpecParser.kt
        │       │   ├── latex/ProblemAnalysisParser.kt
        │       │   ├── geometry/GeometryView.kt   # Canvas 绘制几何图形
        │       │   ├── katex/KatexText.kt         # WebView 封装
        │       │   └── util/ImageCodec.kt
        │       ├── data/
        │       │   ├── local/AppDatabase.kt / Entities.kt / Daos.kt / SettingsDataStore.kt
        │       │   └── repository/HistoryRepository.kt / CacheRepository.kt
        │       ├── domain/
        │       │   ├── model/Models.kt + Profile.kt
        │       │   ├── analyzer/KnowledgeAnalyzer.kt + WeakSpotAnalyzer.kt
        │       │   └── usecase/AnalysisUseCase.kt + GenerateUseCase.kt + TargetedPracticeUseCase.kt
        │       └── feature/
        │           ├── solve/SolveScreen.kt + SolveViewModel.kt + CameraScreen.kt
        │           ├── history/HistoryScreen.kt + HistoryViewModel.kt
        │           ├── analytics/AnalyticsScreen.kt + AnalyticsViewModel.kt + MasteryRadarChart.kt
        │           ├── practice/PracticeScreen.kt + PracticeViewModel.kt
        │           └── settings/SettingsScreen.kt + SettingsViewModel.kt
        └── test/java/com/mathcoach/app/
            ├── JsonTolerantParserTest.kt
            ├── LatexUtilsTest.kt
            ├── KnowledgeAnalyzerTest.kt
            └── WeakSpotAnalyzerTest.kt
```

## 功能验证清单（装 APK 后）

| # | 功能 | 验证方法 |
|---|---|---|
| 1 | 设置服务商 | 设置 Tab → 选通义千问 / Moonshot → 填 Key → 保存 → 测试连接应 ✅ |
| 2 | 拍照解题 | 解题 Tab → 📸 拍照 → 输"帮我分析" → 应看到流式 + 卡片（含 LaTeX 渲染） |
| 3 | 相册选图 | 解题 Tab → 📷 从相册选择 |
| 4 | 同类题 | 解题卡片底部"生成 3 道同类题" → 应看到 3 张新卡 |
| 5 | 历史记录 | 历史 Tab → 应看到刚拍的题；筛选 / 删除 / 导出 |
| 6 | 掌握度 | 拍 5+ 道题后 → 掌握度 Tab → 雷达图 + 列表 + 薄弱项 |
| 7 | 针对性练习 | 练习 Tab → 滑块选 5 道 → 生成 → 应优先出薄弱知识点 |
| 8 | 几何图形 | 拍含三角形/圆的题 → 应看到 Canvas 绘制的图形 |
| 9 | 双模型 | 设置 → 启用双模型 → 填 vision/reasoning 模型 → 拍照解题走两阶段 |
| 10 | 缓存 | 同一张图 + 同样的提问再拍一次 → 应秒回 |
