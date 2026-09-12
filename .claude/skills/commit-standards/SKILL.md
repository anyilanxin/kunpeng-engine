---
name: commit-standards
description: Use when committing changes in kunpeng-engine — running spotlessApply before commit, writing commit messages, or deciding when to commit or push. Covers the root-directory spotlessApply rule and the project's git commit message conventions
---

# 提交规范（spotlessApply + git commit）

改动完成到 git 提交的公共规范。核心两条：**格式化必须在仓库根目录整库执行** `./gradlew spotlessApply`；**commit message 英文分条、不带 Claude 署名尾行、先审核再提交**。

## 何时用

- 改动完成、准备提交前
- 执行任何 spotlessApply / 格式化操作前
- 编写 commit message 时
- 决定要不要 commit / push 时

## spotlessApply 规则

**必须在仓库根目录执行，只允许整库格式化：**

```bash
./gradlew spotlessApply
```

- ❌ **不要用模块级任务** `./gradlew :<module>:spotlessApply`（如 `:dist:spotlessApply`）：spotless 插件未应用到全部子模块，模块级任务选择会直接失败：

  ```text
  Cannot locate tasks that match ':dist:spotlessApply' as task 'spotlessApply' not found in project ':dist'.
  ```

- 模块级 `./gradlew :<module>:compileJava --no-daemon` 做编译验证没问题；**只有格式化必须根级**。

## 提交流程（按顺序）

1. 改动完成后**不要急着 commit**：先 `git diff` 展示改动供审核
2. 得到确认后，根目录执行 `./gradlew spotlessApply` 格式化
3. **不主动 commit / push**，除非用户明确要求
4. commit message 按下方规范书写
5. 多个不相关改动拆成多个 commit，每个 commit 只讲一个主题

## commit message 规范

- 英文书写，首行 `type: 摘要`，type 取 feat / refactor / fix / test / chore / docs
- 正文分条写清**改了什么、为什么**
- **不要**包含 `Co-Authored-By: Claude ...` 尾行

```text
feat: add admin/business dispatch actuator endpoints

- expose AdminDispatchClient/BusinessDispatchClient as actuator endpoints
- map structpack response records to plain DTOs for JSON output
- add protocol module dependencies to dist build
```

## 常见错误

| 错误 | 正确做法 |
|------|----------|
| `./gradlew :dist:spotlessApply`（task not found） | 根目录执行 `./gradlew spotlessApply` |
| 改完直接 commit | 先展示 diff 供审核，确认后再提交 |
| message 带 `Co-Authored-By: Claude` 尾行 | 不带任何 Claude 署名 |
| 中文 / 无 type 前缀的 message | 英文 `type: 摘要` + 分条正文 |
| 一个 commit 混多个主题 | 按功能拆分为多个 commit |
