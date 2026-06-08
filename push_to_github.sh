#!/bin/bash
# WOL-App 推送到 GitHub 自动编译脚本
# 在你自己的电脑（能上 GitHub 的）终端执行

echo "=== WOL 远程开机 APK — 推送到 GitHub ==="
echo ""

cd "WOL-App" || exit 1

git init
git branch -m main
git config user.name "coooopy"
git config user.email "coooopy@users.noreply.github.com"

git add .
git commit -m "feat: WOL远程开机APK 初始版本

- Wake-on-LAN 魔术包发送功能
- 简洁可配置界面 (MAC + IP + 端口)
- 自动保存配置到 SharedPreferences
- GitHub Actions 自动编译 workflow"

git remote add origin https://github.com/coooopy/ww.git

echo ""
echo "开始推送..."
git push -u origin main

echo ""
echo "推送完成！"
echo ""
echo "下一步："
echo "1. 打开 https://github.com/coooopy/ww/actions"
echo "2. 点击左侧 Build APK workflow"
echo "3. 点击 Run workflow → Run workflow"
echo "4. 等待约 3-5 分钟"
echo "5. 在 workflow 运行结果页下载 app-debug.apk"
