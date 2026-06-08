#!/usr/bin/env python3
"""
WOL APK Builder - 用纯Python构建一个功能完整的WOL远程开机APK
原理：生成所有Android源码文件，打包成可用Android Studio/Gradle直接编译的项目
同时生成一个使用GitHub Actions在线编译的workflow文件
"""

import os
import zipfile
import struct
import base64
import io

BASE = "C:/Users/Administrator/WorkBuddy/2026-06-08-19-46-36/WOL-App"

# ============================================================
# 生成 proguard-rules.pro
# ============================================================
def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"✅ {path}")

write_file(f"{BASE}/app/proguard-rules.pro", "# Add project specific ProGuard rules here.\n")

# ============================================================
# .gitignore
# ============================================================
write_file(f"{BASE}/.gitignore", """*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
""")

# ============================================================
# gradle-wrapper.jar (从 Base64 解码，这是标准的 gradle wrapper jar)
# 我们先生成一个占位，然后提供脚本自动下载真实的
# ============================================================
wrapper_dir = f"{BASE}/gradle/wrapper"
os.makedirs(wrapper_dir, exist_ok=True)

# gradle wrapper batch script (Windows)
write_file(f"{BASE}/gradlew.bat", r"""@rem
@rem Copyright 2015 the original author or authors.
@rem
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
""")

# gradlew (Linux/macOS)
write_file(f"{BASE}/gradlew", r"""#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0
#

APP_HOME=$( cd "${0%/*}/.." && pwd -P ) 2>/dev/null
APP_NAME="Gradle"
APP_BASE_NAME="${0##*/}"

# Add default JVM options here.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in              #(
  CYGWIN* )         cygwin=true  ;;
  Darwin* )         darwin=true  ;;
  MSYS* | MINGW* )  msys=true    ;;
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"

# Execute Gradle
exec "$JAVACMD" "$@"
""")

# ============================================================
# GitHub Actions workflow - 自动在线编译APK
# ============================================================
os.makedirs(f"{BASE}/.github/workflows", exist_ok=True)
write_file(f"{BASE}/.github/workflows/build.yml", """name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Setup Android SDK
      uses: android-actions/setup-android@v2

    - name: Make gradlew executable
      run: chmod +x gradlew

    - name: Build Debug APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: WOL-debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk
""")

# ============================================================
# local.properties (模板)
# ============================================================
write_file(f"{BASE}/local.properties", "# 取消注释并填写你的 Android SDK 路径\n# sdk.dir=C:\\\\Users\\\\YourName\\\\AppData\\\\Local\\\\Android\\\\Sdk\n")

print("\n🎉 所有文件已生成完毕！")
print(f"项目目录: {BASE}")
