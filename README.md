# 📱 WOL 远程开机 APK — 编译指南

## 功能说明

这是一个 **Wake-on-LAN（WOL）局域网远程开机** 安卓应用：

- 输入目标主机的 **MAC 地址** 和 **IP 地址**
- 一键发送 **WOL 魔术包（Magic Packet）** 唤醒主机
- 支持自定义端口（默认 9）
- **自动保存配置**，下次打开无需重新输入
- 同时向目标 IP 和广播地址 `255.255.255.255` 发包，成功率更高

---

## 前提条件（目标主机端）

> ⚠️ 手机只负责发包，主机端必须提前配置好才能响应

1. **主机 BIOS 开启 WOL**：
   - 进入 BIOS → Power Management → **Wake on LAN** → `Enabled`

2. **网卡驱动设置**（Windows）：
   - 设备管理器 → 网络适配器 → 右键属性 → 电源管理
   - ✅ 勾选"允许此设备唤醒计算机"
   - ✅ 勾选"只允许幻数据包唤醒计算机"

3. **主机使用有线网络**（WiFi 的 WOL 支持不稳定）

4. **获取主机网卡 MAC 地址**：
   ```
   Windows: ipconfig /all → 找"物理地址"
   Linux:   ip addr show | grep ether
   ```

---

## 三种编译方式（选一种）

### 方式一：GitHub Actions 在线编译（推荐，零环境要求）

1. 在 GitHub 新建仓库
2. 把 `WOL-App` 目录内的所有文件推送上去
3. 进入仓库 → **Actions** → **Build APK** → 手动触发
4. 等待约 3-5 分钟，从 Artifacts 下载 `app-debug.apk`

```bash
git init
git add .
git commit -m "Initial WOL App"
git remote add origin https://github.com/你的用户名/wol-app.git
git push -u origin main
```

---

### 方式二：Android Studio 本地编译

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 打开 `WOL-App` 文件夹作为项目
3. 等待 Gradle 同步（首次会下载依赖，需要网络）
4. 菜单 Build → **Build Bundle(s) / APK(s)** → Build APK(s)
5. APK 在 `app/build/outputs/apk/debug/app-debug.apk`

---

### 方式三：命令行编译（需要 JDK 17 + Android SDK）

```bash
# 1. 配置 local.properties
echo "sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk" > local.properties

# 2. 编译
./gradlew assembleDebug

# 3. APK 位置
app/build/outputs/apk/debug/app-debug.apk
```

---

## APK 安装到手机

1. 把 `app-debug.apk` 传到手机（微信/QQ/数据线均可）
2. 手机 设置 → 安全 → 允许安装未知来源应用
3. 点击 APK 文件安装

---

## 使用方法

| 字段 | 说明 | 示例 |
|------|------|------|
| MAC 地址 | 目标主机网卡物理地址 | `AA:BB:CC:DD:EE:FF` |
| IP 地址 | 目标主机局域网 IP | `192.168.1.100` |
| 端口 | WOL 监听端口（一般默认） | `9` |

填写完毕点 **⚡ 立即开机** 即可发送魔术包。

---

## 项目结构

```
WOL-App/
├── .github/workflows/build.yml     ← GitHub Actions 自动编译
├── app/
│   ├── src/main/
│   │   ├── java/com/wol/app/
│   │   │   └── MainActivity.java   ← 核心逻辑（WOL发包）
│   │   ├── res/values/styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## 常见问题

**Q: 发包成功但主机没有开机？**
- 检查主机 BIOS 是否开启 WOL
- 检查网卡驱动电源管理设置
- 确保手机和主机在同一局域网
- 部分路由器屏蔽 UDP 广播包，尝试填写主机精确 IP

**Q: MAC 格式报错？**
- 支持 `AA:BB:CC:DD:EE:FF`、`AA-BB-CC-DD-EE-FF` 两种格式

**Q: 端口填多少？**
- 99% 情况下用默认 `9`，也有些系统用 `7` 或 `40000`
