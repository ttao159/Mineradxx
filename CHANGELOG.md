# Changelog

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循语义化版本。

## [Unreleased]

### 新增

- 后端单元测试（`netease-std` 与各 API 模块纯函数，共 6 个用例），接入 `npm test`
- CI 增加「后端单元测试」与「依赖安全审计」门禁步骤
- `LICENSE`、`NOTICE.md`、`PRIVACY.md`、`SECURITY.md` 治理文档

### 修复

- 未登录时播放失败：`shouldInterceptRequest` 增加 Java 层 `/api/` 代理，覆盖原生 `<audio>/<img>` 资源请求
- Folia 歌词舞台无法登录：改为文档起始注入 `window.__NCM_API_BASE`
- DIY 模式无反应、横屏遮挡：原生模式切换按钮移出前端 `#top-right` 区域
- 横屏控件重叠：旋屏按钮改为文字版并同步文案

### 安全

- 后端依赖 `music-metadata` 通过 `overrides` 锁定至 `11.14.0`，`npm audit` 漏洞清零

## [1.0.0] - 2026-08-09

### 新增

- Mineradio Android 双模式应用初始版本
  - 完整播放器（Mineradio）与全屏歌词舞台（Folia）一键切换
  - 网易云 / QQ / 酷狗 / Spotify / 汽水音乐搜索与播放
  - 扫码登录（网易云 / QQ）
  - 天气电台、3D 粒子、均衡器
- Railway 后端一键部署配置（`railpack.json`）
- 横竖屏切换按钮（移除全部热键）
- 今日聆听 dock、接下来播放、为你挑选、精选乐评
