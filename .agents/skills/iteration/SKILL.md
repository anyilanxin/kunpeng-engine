---
name: iteration
description: Full-chain iteration orchestrator for everyday changes to existing functionality. Use when a change spans multiple engine layers (protocol Record / LifeCycle enum / repository Entity / engine / broker / gateway / client SDK) and should land in one pass — field adjustments, validation or state machine tweaks, bug fixes, enum value additions, interface parameter changes. Trigger phrases like "改一下XX / XX加个字段 / XX调一下 / 改个功能 / 迭代一下". Broad-but-shallow large changes go orchestrator-dispatch mode; single-module deep work is done in place following the module's standards skills; new subsystems or serialization-format work use a design-first flow instead.
---

# 全链路迭代开发专家（iteration）

## 技能定位

- **核心定位**：迭代阶段的全链路改动编排者——把分散在 record-standards / enum-standards / repository-standards / structpack 等规范技能里的"改已有功能"流程串成一条线，一次完成「协议 + 存储 + 引擎逻辑 + 客户端 SDK + 测试 + 文档」的跨层修改
- **阶段定位**：引擎主体功能已成型后的日常迭代，修改已有功能时使用
- **核心价值**：避免小改动在多个技能间手动切换，防"改了 A 忘了 B"（Record 加了字段 Entity 没同步、枚举加了状态文档没更新）

### 与单域技能的边界

| 场景 | 用什么 |
|------|--------|
| 改动跨多个模块/层（Record 字段牵动 Entity/引擎/客户端的链路） | **本技能（iteration）** |
| 单模块内部实现/修 bug（不动协议契约） | 直接改，遵循对应规范技能（record/enum/repository-standards） |
| 序列化格式、协议编号池、新 Record 字段定义 | 先读 `structpack` / `enum-standards`（破坏性评估先行） |
| 理解/改动 scheduler、eventlog、cluster-dispatch 内部机制 | 对应知识库技能（scheduler / eventlog-design / cluster-dispatch-design） |
| 提交环节 | `commit-standards` |

> iteration 是**编排者**：引用各规范技能的细则（不重复），聚焦"全链路改动的影响排查、执行顺序、契约一致性自检"。

## 与用户沟通规范

执行者是 AI，但**对话对象通常是产品/业务背景的人**。对外沟通按业务语言范式——代码是给 AI 执行用的，不是默认拿来"汇报"的。

- 先说**业务结论**（这个改动让引擎/功能变成什么样、解决了什么问题），再说**改动范围**（动了哪些模块），技术细节最后且只给关键点
- **默认不贴大段代码**：用「文件 + 一句话说明改了什么」代替代码块与 diff
- 代码只在三种情况出现：用户主动要求看；需要用户拍板的技术决策（给关键片段 + 建议）；关键契约点（字段名/编号/key-id 一行说明）
- **四段式汇报**（改动计划与完成汇报都用）：① 业务结论 ② 改动范围（文件+一句话）③ 风险与待确认 ④ 待验证
- **主动点拨**有学习价值的设计与坑（讲 why，一两句业务语言），不打断主线
- 自检准则：每段输出前自问"产品经理看了能直接理解业务影响吗？"，不能 → 翻译成业务语言

## 铁律

### 1. 先分析影响范围，再动手改代码

迭代改动的核心风险是跨层遗漏（Record 加了字段，Entity 的 wrap/unwrap 没同步；LifeCycle 加了状态，编号池与 INTENT_CLASSES 没接）。执行前先扫描代码形成完整改动清单。

### 2. 大改动不硬扛，但也不轻易交出去

- **广而浅的大改动**（一批字段、几个命令、多处调校验——面广但每处不深）：转入**总控派发模式**——iteration 当总控不下场，把各模块派发给专项执行窗口（subagent），总控负责契约与汇总
- **深而窄的大改动**（新调度算法、重构某块核心机制、深度调试）：建议走设计先行流程（先出设计文档评估），iteration 不强行接
- **深浅判断**：深点能不能被收敛进一个执行窗口的契约里。能 → 派发吃得下；不能（跨层联动逻辑很深）→ 退回设计先行

### 3. 全链路契约必须一致

kunpeng 的跨层契约是：协议 Record 字段 ↔ Entity 持久化字段 ↔ 客户端 SDK DTO 三方对齐；LifeCycle 枚举码值 ↔ `INTENT_CLASSES` 注册 ↔ `fromProtocolValue` 解析；Entity 的 `wrap`/`unwrap` 成对。任何一处改了，其余各处和技能文档必须同步，自检阶段逐项核对。

### 4. 协议兼容变更必须先评估，不直接改

以下属于**破坏性/团队决策级**变更，遇到时停下来不直接改：重排 `PROCESS_INDEX_*`/`RECORD_INDEX_*` 编号池（Raft 日志反序列化错位）、变更 structpack key-id 身份、改批帧格式、改 gRPC 协议。处理方式：先产出变更方案（改什么/为什么/影响范围/兼容与回滚策略）交评估，确认后才执行。

> 加 Record 字段（新 key）、加枚举状态（顺延编号池）、加 Entity 字段属于**低风险**（向后兼容），iteration 可正常处理。判断不清时按破坏性对待（先评估）。

## 改动分级

| 等级 | 特征 | 处理 |
|------|------|------|
| **小改动** | 单模块内逻辑调整、bug 修复、常量/文案、不跨层的实现细节 | 本技能一次搞定（或直接改+对应规范技能） |
| **中改动** | 跨模块字段链路（Record+Entity+引擎逻辑）、新命令/新状态（顺延编号）、接口参数调整 | 本技能一次搞定，执行前需更详细规划 |
| **大改动** | **协议编号池重排、序列化格式变更**、新模块/新子系统、跨 broker-gateway-client 多点联动 | **广而浅**→总控派发模式；**深而窄**→设计先行；**破坏性协议变更→先出方案交评估** |

**分级依据**：按跨层数量与是否有结构性/协议性变更判断。

## 工作流程

### 步骤1：理解需求

明确要改什么、哪个模块/哪条链路。有歧义先确认：改哪个模块（protocol/engine/broker/gateway/repository/scheduler/eventlog/cluster/clients）、具体改什么（加字段？改状态机？修 bug？）、是否暴露到客户端 SDK。

### 步骤2：分析影响范围

扫描代码，按以下维度逐一排查，形成完整文件清单：

| 层 | 排查内容 |
|----|---------|
| `kunpeng/protocol/` | Record 类是否涉及（字段/getter/setter/addXxx → 走 record-standards）；LifeCycle 枚举是否涉及（状态/编号池 → 走 enum-standards，**编号错=Raft 反序列化错位**） |
| `kunpeng/repository/` | Entity 是否涉及（**必须同步 wrap/unwrap** → 走 repository-standards）；落库字段映射与裁剪 |
| `kunpeng/engine/` `broker/` `gateway/` `cluster/` | 业务逻辑、状态机分支、命令处理是否涉及 |
| `kunpeng/scheduler/` `eventlog/` | 是否触及调度契约 / 事件日志格式（先读对应知识库技能） |
| `clients/` | 契约变化是否需要暴露到 client-java / spring-boot-starter-client |
| 测试 | 对应模块测试是否需要同步（JUnit5/jqwik） |
| 技能文档 | 改动触及 scheduler/eventlog/cluster-dispatch/structpack 的设计事实 → 对应知识库技能文档要同步更新 |

**分析完先定执行路径**：小/中 → 步骤 3~6 自执行；大+广而浅 → 总控派发；大+深而窄/破坏性协议变更 → 设计先行/出方案评估，停止自执行。

### 步骤3：制定改动计划（四段式展示，等用户确认）

① 业务结论 ② 改动等级与范围（文件+一句话清单，按 protocol→repository→engine→gateway→clients→test→docs 分组）③ 风险与待确认（破坏性、需拍板点）④ 待验证（哪些要实际跑起来确认）。用业务语言请求确认，不让用户读代码才敢确认。

### 步骤4：执行改动（固定顺序）

```
1. protocol（Record / LifeCycle / 编号池）→ 编译验证 :kunpeng:protocol:*:compileJava
2. repository（Entity + wrap/unwrap）→ 编译验证
3. engine / broker / gateway / cluster 逻辑 → 编译验证
4. clients SDK（如涉及）
5. 测试同步
6. 技能/设计文档同步
```

执行原则：每改完一组用业务语言一句话说明；遇到意外立即告知不绕过；风格与现有代码一致；不改不相关的代码，不做"顺手"改动。

### 步骤5：全链路契约自检

| 检查项 | 说明 |
|--------|------|
| **字段三方一致** | Record 字段 = Entity 落库字段 = 客户端 DTO（按需裁剪的除外，但要明确） |
| **wrap/unwrap 成对** | Entity 互转覆盖新字段，编译只是底线，语义要对 |
| **编号池连续** | 新枚举状态顺延编号，`INTENT_CLASSES`/`fromProtocolValue` 已接入（enum-standards） |
| **key-id 身份** | 新字段用新 key，不复用/不改既有 key（structpack 强规则） |
| **编译验证** | 每层改完跑模块 `compileJava`；全链路完跑 `./gradlew buildSkipTest` |
| **测试覆盖** | 改动逻辑有对应测试；现有测试没被改挂 |
| **文档同步** | 触及知识库技能设计事实的已更新 |
| **格式化** | 提交前根目录整库 `./gradlew spotlessApply`（commit-standards） |

### 步骤6：汇报完成

四段式：① 业务结论 ② 改动范围（文件+一句话）③ 自检结果 ④ 待验证。不贴 diff。

## 与其他技能协作

- **record-standards / enum-standards / repository-standards**：各层规范来源（iteration 执行时遵循）
- **structpack / scheduler / eventlog-design / cluster-dispatch-design**：触及对应模块时先读（设计事实与风险）
- **commit-standards**：收尾提交（spotlessApply 整库 + commit message 规范）
