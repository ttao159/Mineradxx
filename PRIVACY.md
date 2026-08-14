# Mineradxx 隐私与用户数据说明

Mineradxx（Mineradio Android）是 Android 端音乐播放器，前端能力来自
[XxHuberrr/Mineradio](https://github.com/XxHuberrr/Mineradio) 与
[chthollyphile/folia-major](https://github.com/chthollyphile/folia-major)。项目不会把用户登录状态、
Cookie、播放历史、搜索历史或本地缓存提交到 GitHub 仓库。

## 后端与登录态

本应用本身不内置可公开复用的音乐平台密钥，播放/搜索/登录接口依赖一个自部署的 Node.js 后端
（推荐 Railway 免费部署）。网易云音乐、QQ 音乐、酷狗音乐、汽水音乐等平台的登录 Cookie
保存在**用户自行部署的后端实例**上，只有部署者本人可以访问；仓库中不含任何登录态或 Cookie。

## 本地数据

应用可能在设备本机保存以下数据：

- 第三方音乐平台登录 Cookie（保存于自部署后端）
- 搜索历史
- 自定义歌词与歌词布局设置
- 播放器视觉与均衡器设置

这些数据用于本地体验，不属于开源仓库内容。

## 不应上传的内容

以下内容不应提交到 GitHub：

- `.cookie`、`.qq-cookie` 等登录态文件
- `node_modules/`
- 构建产物（`app/build/`、APK）
- 用户上传的本地音乐文件
- 用户账号信息、Cookie、Token、二维码登录状态
- 后端运行日志中含 Cookie/Token 的片段

## 第三方平台

用户通过网易云音乐、QQ 音乐、酷狗音乐、Spotify、汽水音乐等第三方平台登录时，应遵守对应平台的
用户协议与会员权益规则。Mineradxx 不提供绕过付费、破解会员、绕过音质限制或重新分发音乐内容的能力。
