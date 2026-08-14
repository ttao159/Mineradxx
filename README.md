# Mineradio Android

> 把 Mineradio 桌面版音乐播放器完整移植到 Android，并整合 Folia 全屏歌词舞台！
>
> 前端能力来自 [XxHuberrr/Mineradio](https://github.com/XxHuberrr/Mineradio) 与 [chthollyphile/folia-major](https://github.com/chthollyphile/folia-major)，WebView 壳框架源自 [bolaosi/Mineradio-Android](https://github.com/bolaosi/Mineradio-Android)。

Mineradio 是一款沉浸式音乐播放器，融合天气电台、歌词舞台、粒子视觉和 3D 歌单架。
本安卓版在此基础上内置**双模式**：完整播放器（Mineradio）与全屏歌词舞台（Folia），应用内一键切换。

---

## 双模式

| 模式 | 说明 | 入口 |
| ---- | ---- | ---- |
| 播放器 | 完整 Mineradio 播放器：网易云/QQ/酷狗/Spotify/汽水音乐搜索与播放、扫码登录、天气电台、3D 粒子、均衡器 | 默认启动 |
| 歌词舞台 | Folia 全屏动态歌词播放器：实时歌词滚动、AI 主题生成、可视化背景 | 右上角「歌词舞台」按钮切换 |

两个模式各自独立运行，共享同一远程后端与登录态。

---

## 功能状态

| 功能 | 状态 |
| ---- | ---- |
| UI / 3D 粒子 / 歌词 | 正常 |
| 网易云搜索/播放 | 正常 |
| QQ 音乐搜索/播放 | 正常 |
| 酷狗音乐搜索/播放 | 正常 |
| Spotify 搜索/播放 | 正常（需配置客户端密钥） |
| 汽水音乐搜索/播放 | 正常 |
| 扫码登录（网易云/QQ） | 正常 |
| 汽水音乐扫码登录 | 不可用（依赖 Electron，纯 Node 后端降级） |
| 天气电台 | 正常 |
| 均衡器 | 正常 |
| 桌面歌词 | 不可用 |

---

## 下载 APK

从本仓库 [Releases](https://github.com/ttao159/Mineradxx/releases/latest) 下载，或参考下方「自行构建」。

要求：Android 8.0+，建议 4GB 内存以上，需开启「允许安装未知来源应用」。

---

## 部署后端（必须，免费）

App 依赖 Node.js 后端提供音乐搜索/登录/播放接口。本仓库携带**完整桌面版后端**（网易云/QQ/酷狗/Spotify/汽水音乐全部模块），推荐用 Railway 免费部署。

### 一键部署到 Railway

1. 打开 [railway.app](https://railway.app) → 用 GitHub 登录
2. 点击 **New Project** → **Deploy from GitHub**
3. 选择本仓库
4. **关键**：Root Directory 设为 `app/src/main/assets/nodejs-project`
5. 点击 **Deploy**
6. 等待 2-3 分钟，复制生成的域名（类似 `https://xxx.up.railway.app`）
7. 把域名填到 `app/src/main/java/com/mineradio/android/MainActivity.java` 的 `API_BASE_URL` 常量
8. 重新构建 APK（见下）

> 后端同时暴露两套接口：`/api/*` 供播放器模式使用，标准 NeteaseCloudMusicApi 路径（`/search`、`/song/url/v1` 等）供歌词舞台模式使用。

---

## 自行构建

### 方式一：GitHub Actions（推荐）

1. 把本仓库推到你的 GitHub（需要在你自己的账号下，无需原仓库推送权限）
2. 进入 Actions → **Build Mineradio APK** → **Run workflow**
3. 等待 5-10 分钟，在 Artifacts 下载 APK

### 方式二：本地构建

环境要求：JDK 17、Android SDK、Node.js 18+。

```bash
# 1. 安装后端依赖
cd app/src/main/assets/nodejs-project
npm install

# 2. 构建 APK（Windows 可直接运行 build-apk.bat）
cd ../..
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/
```

---

## 项目结构

```
app/src/main/
  assets/
    index.html          Mineradio 播放器前端
    folia/              Folia 歌词舞台前端（构建产物）
    vendor/             第三方库 (Three.js, GSAP, music-tempo)
    nodejs-project/     完整后端服务器
      server.js         主服务（/api/* 封装路由 + 静态资源）
      netease-std.js    标准 NeteaseCloudMusicApi 路由（歌词舞台用）
      kugou-api.js     酷狗
      spotify-api.js   Spotify
      qq-vip-api.js    QQ 音乐 VIP
      qishui-api.js    汽水音乐
      qishui-auth-v6.js 汽水扫码登录（Electron 降级）
      cuefield/         Cue 曲目解析
      qishui-audio-decryptor/ 汽水音频解密
      package.json
  java/.../MainActivity.java  WebView 容器 + 双模式路由 + API base 注入
  java/.../NodeService.java   Node 后端存根（默认禁用，API 走远程）
```

---

## 二次开发：更新 Folia 歌词舞台

歌词舞台前端来自 [chthollyphile/folia-major](https://github.com/chthollyphile/folia-major)，为适配 Android 做了两处定制：

1. `src/services/netease.ts` 的 `getConfiguredApiBase()` 优先读取 `window.__NCM_API_BASE`（运行时注入），无则回退到 `VITE_NETEASE_API_BASE`。
2. 使用 `ELECTRON=true npm run build` 构建，使产物资源路径为相对路径，可直接从 `assets/folia/` 子目录加载。

重新构建后把 `dist/` 内容覆盖到 `app/src/main/assets/folia/`，并去掉 `registerSW.js` / `manifest.webmanifest`（WebView 内禁用 Service Worker）。

---

## 许可

遵循原项目 [XxHuberrr/Mineradio](https://github.com/XxHuberrr/Mineradio)（GPL-3.0）的许可协议。
