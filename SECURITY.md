# Security Policy

## Supported Versions

当前只维护最新公开版本（`main` 分支的最新 Release APK）。

## APK 安全提示

请只从本仓库的 [GitHub Releases](https://github.com/ttao159/Mineradxx/releases) 下载 APK，或
参照 README「自行构建」章节在本地编译。不要安装来源不明的 APK，也不要安装被二次打包、注入过
广告或修改过后端地址的第三方 APK。

## Reporting a Vulnerability

如果你发现安全问题，请通过 GitHub Issues 联系仓库作者。

请不要在公开 Issue 中直接贴出 Cookie、Token、账号信息、私密链接或可复现的敏感数据。

## Sensitive Data

Mineradxx 不应收集或上传用户 Cookie。登录态保存在用户自行部署的后端实例上。

提交问题反馈前，请先确认没有附带：

- `.cookie`、`.qq-cookie` 等登录态文件
- 本地音乐文件
- 用户账号截图
- 调试日志中的 Cookie、Token 或隐私路径
- 自部署后端的公网地址（如不想公开）
