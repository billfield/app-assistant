# MathCoach Android — 命令行 Build 指南

本指南适用于**未安装 Android Studio** 的环境，通过命令行编译出 APK 并安装到手机。

---

## 1. 安装 JDK 17

Gradle 8.x + AGP 8.x 强制要求 JDK 17。

**Windows:**
```bash
winget install EclipseAdoptium.Temurin.17.JDK
```

验证：
```bash
java -version
# 应输出 openjdk version "17.x.x"
```

---

## 2. 安装 Android SDK（命令行工具）

不需要完整 Android Studio，只需要 cmdline-tools + 必要的 SDK 组件。

### 2.1 下载 cmdline-tools

访问：https://developer.android.com/studio#command-tools

下载 `commandlinetools-win-XXXXXXXX_latest.zip`（约 130MB）。

### 2.2 解压到固定目录

```bash
mkdir D:\Android\Sdk\cmdline-tools\latest
# 解压 cmdline-tools 的 bin/ lib/ NOTICE.txt source.properties 到 D:\Android\Sdk\cmdline-tools\latest\
```

最终目录结构：
```
D:\Android\Sdk\
└── cmdline-tools\
    └── latest\
        ├── bin\
        │   ├── sdkmanager.bat
        │   └── avdmanager.bat
        ├── lib\
        └── source.properties
```

### 2.3 接受 license 并安装组件

```bash
set ANDROID_HOME=D:\Android\Sdk
set PATH=%PATH%;D:\Android\Sdk\cmdline-tools\latest\bin

sdkmanager --licenses
# 全部输入 y 接受

sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

这一步会下载约 300MB 的 SDK 平台文件。

---

## 3. 配置项目

### 3.1 创建 local.properties

```bash
cd D:\pythonProject\MathTools\android
copy local.properties.template local.properties
```

编辑 `local.properties`，把 SDK 路径改成你实际的安装路径：

```properties
sdk.dir=D:\\Android\\Sdk
```

注意 Windows 路径要用双反斜杠转义。

---

## 4. Build APK

### 4.1 Debug APK（推荐首版）

```bash
cd D:\pythonProject\MathTools\android
gradlew.bat assembleDebug
```

第一次会下载约 1GB 的 Gradle + 依赖，需要 10-30 分钟。

成功后 APK 在：
```
android\app\build\outputs\apk\debug\app-debug.apk
```

### 4.2 Release APK（需签名）

```bash
gradlew.bat assembleRelease
```

未配置签名会失败。第 3 轮会加签名配置。

---

## 5. 安装到手机

### 5.1 开启手机 USB 调试

设置 → 关于手机 → 连点版本号 7 次开启开发者模式 → 开发者选项 → 打开"USB 调试"。

### 5.2 用 USB 线连电脑

```bash
adb devices
# 应看到设备序列号 + "device"
```

如果显示 "unauthorized"，手机上会弹出授权框，点允许。

### 5.3 安装 APK

```bash
adb install android\app\build\outputs\apk\debug\app-debug.apk
```

---

## 6. 常见问题

### Q: gradlew.bat 提示找不到 SDK？

检查 `local.properties` 里的 `sdk.dir` 路径，**用双反斜杠**：
```properties
sdk.dir=D:\\Android\\Sdk
```

### Q: sdkmanager 提示 "Could not determine SDK root"?

设置环境变量：
```bash
set ANDROID_HOME=D:\Android\Sdk
set ANDROID_SDK_ROOT=D:\Android\Sdk
```

### Q: 编译失败 "package xxx does not exist"？

清除 Gradle 缓存重试：
```bash
gradlew.bat clean
rmdir /s /q %USERPROFILE%\.gradle\caches
gradlew.bat assembleDebug
```

### Q: KaTeX 公式不渲染？

需要下载 KaTeX 资源到 `app\src\main\assets\katex\`，详见该目录下的 README.md。

### Q: 手机安装后打不开 / 闪退？

连上 adb 看日志：
```bash
adb logcat | findstr mathcoach
```

---

## 7. 推荐配置（可选）

### 7.1 加速 Gradle

编辑 `%USERPROFILE%\.gradle\gradle.properties`：

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.daemon=true
```

### 7.2 国内镜像

如果 Gradle 下载慢，在 `android\build.gradle.kts` 的 `pluginManagement.repositories` 顶部加：

```kotlin
maven("https://maven.aliyun.com/repository/google")
maven("https://maven.aliyun.com/repository/central")
maven("https://maven.aliyun.com/repository/gradle-plugin")
```

在 `android\settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 顶部加：

```kotlin
maven("https://maven.aliyun.com/repository/google")
maven("https://maven.aliyun.com/repository/central")
```

---

## 8. 下一步

- 首次启动 App 后，去「设置」Tab 配置服务商和 API Key
- 在「解题」Tab 选择图片 → 输入问题 → 点「开始分析」
- 完整功能按 3 轮迭代交付，详见 `plans/app-abundant-cosmos.md`
