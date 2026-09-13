@rem Gradle wrapper script (fallback when gradle-wrapper.jar is not yet present)
@rem
@rem 首次使用：
@rem   1. 安装 Gradle 8.7+ (https://gradle.org/install/)，或用任意 IDE 自带的 Gradle
@rem   2. 在本目录执行：gradle wrapper --gradle-version 8.7
@rem      会自动生成 gradle-wrapper.jar 和官方 gradlew.bat
@rem   3. 之后就可以用 gradlew.bat assembleDebug 命令了
@rem
@rem 如果你已经看到了官方的 gradlew.bat（这个文件被覆盖了），说明 wrapper 已经初始化。

@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] 未找到 gradle 命令
    echo.
    echo 请先初始化 Gradle Wrapper，两种方式任选：
    echo.
    echo 方式 A（推荐）：临时安装 Gradle 8.7
    echo   1. 下载 https://services.gradle.org/distributions/gradle-8.7-bin.zip
    echo   2. 解压到任意目录，把 bin 目录加入 PATH
    echo   3. 在本目录执行：gradle wrapper --gradle-version 8.7
    echo   4. 之后 gradlew.bat 就可以脱离系统 Gradle 使用了
    echo.
    echo 方式 B：用 Android Studio 内置 Gradle
    echo   如果你装了 Android Studio，直接用它打开本项目即可，
    echo   不需要命令行 gradlew.bat
    echo.
    exit /b 1
)
gradle %*
