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

[推送与远程仓库]
- Date: 2026-08-14
- Context: Discovered by Agent while pushing backend changes to origin
- Category: Workflow & Collaboration
- Instructions:
  - 远程：`origin` → `https://github.com/ttao159/Mineradxx`，分支 `main`
  - 环境 git helper 只读，推送需内嵌凭证：`git -c credential.helper= push "https://<user>:<token>@github.com/ttao159/Mineradxx.git" main`
  - 注意 `-c credential.helper=` 必须放在 `git` 之后、`push` 之前；写成 `git push -c ...` 会报 `unknown switch 'c'`

[后端构建、测试与审计]
- Date: 2026-08-14
- Context: Discovered by Agent while validating the Node backend
- Category: Build Methods
- Instructions:
  - 后端目录：`app/src/main/assets/nodejs-project/`
  - 单元测试：`npm test`（node --test，用例位于 `tests/*.test.js`）
  - 依赖审计：`npm audit --audit-level=high`，需保持 0 漏洞
  - 本地冒烟：`PORT=8080 node server.js`（HOST 默认 0.0.0.0），生产为 Railway 远端
