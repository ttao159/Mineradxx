# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

### User Instruction Entry
User instruction entries should follow this format:

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

### Project Knowledge Entry
Entries discovered by the Agent during task execution should follow this format:

[Project Knowledge Summary]
- Date: [YYYY-MM-DD]
- Context: Discovered by Agent while performing [specific task description]
- Category: [Operations & Deployment|Build Methods|Testing Methods|Troubleshooting & Debugging|Workflow & Collaboration|Environment Configuration]
- Instructions:
  - [Specific knowledge points, described line by line]

## Deduplication Strategy
- Before adding a new entry, check for similar or identical instructions.
- If a duplicate is found, skip the new entry or merge it with the existing one.
- When merging, update the context or date information.
- This helps avoid redundant entries and keeps the memory file tidy.

## Entries

[版本号管理]
- Date: 2026-08-14
- Context: 用户明确指示：每次更新后版本号也要更新；后补充：bug 修复无需更新版本号
- Instructions:
  - 新增功能/功能变更后需递增版本号并一并提交推送；纯 bug 修复、遗漏补齐不递增版本号
  - Android 版本：`app/build.gradle` 的 `versionCode`（+1）与 `versionName`
  - 应用/后端版本：`app/src/main/assets/nodejs-project/package.json` 与 `package-lock.json` 的 `version`（顶部与 `packages[""]` 两处需同步）
  - 后端 `/api/app/version` 读取 package.json 的 version，前端更新检测面板展示该版本号

[推送与远程仓库]
- Date: 2026-08-14
- Context: Discovered by Agent while pushing backend changes to origin
- Category: Workflow & Collaboration
- Instructions:
  - 远程：`origin` → `https://github.com/ttao159/Mineradxx`，分支 `main`
  - 环境 git helper / `/root/.netrc` 内的 token（fine-grained，93 字符）只有读权限：fetch、upload-pack 返回 200，push（receive-pack）返回 403
  - push 需用户提供有写权限的 classic PAT（`ghp_` 开头，40 字符），内嵌凭证：`git -c credential.helper= push "https://ttao159:<token>@github.com/ttao159/Mineradxx.git" main`
  - 注意 `-c credential.helper=` 必须放在 `git` 之后、`push` 之前；写成 `git push -c ...` 会报 `unknown switch 'c'`
  - 用 URL 形式 push/fetch 不会更新 `origin/main` remote-tracking 引用，push 后需 `git fetch origin main` 同步本地引用

[后端构建、测试与审计]
- Date: 2026-08-14
- Context: Discovered by Agent while validating the Node backend
- Category: Build Methods
- Instructions:
  - 后端目录：`app/src/main/assets/nodejs-project/`
  - 单元测试：`npm test`（node --test，用例位于 `tests/*.test.js`）
  - 依赖审计：`npm audit --audit-level=high`，需保持 0 漏洞
  - 本地冒烟：`PORT=8080 node server.js`（HOST 默认 0.0.0.0），生产为 Railway 远端

[Android APK 构建]
- Date: 2026-08-14
- Context: Discovered by Agent while assembling the Android debug APK
- Category: Build Methods
- Instructions:
  - 构建命令：`cd /workspace && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew assembleDebug --console=plain`
  - APK 输出：`app/build/outputs/apk/debug/app-debug.apk`
  - 前端单体文件：`app/src/main/assets/index.html`；改完须同步到 `app/src/main/assets/nodejs-project/public/index.html` 并校验两份 md5 一致
  - 构建为 CPU/内存密集型任务，用 background terminal 执行（设 timeout 与 cpu 限制），勿用前台 bash 直接跑
