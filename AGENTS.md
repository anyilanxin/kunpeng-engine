# AGENTS.md — kunpeng-engine · 微服务编排引擎

本文件为 AI 编码代理（ZCode / Codex / Cursor 等）在本仓库工作时的指引，遵循 [AGENTS.md 开放规范](https://agents.md)。Claude Code 通过根目录 `CLAUDE.md` 的 `@AGENTS.md` 引用同源加载——**本文件是单一事实源，改动只改这里**，勿单独改 CLAUDE.md。

## 项目概述

微服务编排引擎（Kunpeng Engine）：以 [BPMN 2.0](https://www.omg.org/spec/BPMN/2.0.2/) 定义可视化流程，跨微服务编排业务过程。Zeebe 架构风格（broker/gateway 分离、分区事件日志、Raft 复制、无关系型数据库、水平扩展、exporter 数据导出）基础上做了大量自研调整与快速迭代。

- **技术栈**：Java 25（toolchain）+ Gradle 多模块 monorepo；Spring Boot 4.1.0（BOM 形态，非应用壳）；gRPC + protobuf（gateway/协议层）；RocksDB（存储）；JUnit 5 + jqwik + JUnit4 vintage（测试）
- **坐标**：group `com.anyilanxin.kunpeng`，版本见 `gradle.properties`；仓库 `github.com/anyilanxin/kunpeng-engine`，主分支 `main`
- **许可（红线）**：多许可仓库（AGPL-3.0 / Apache-2.0 / MPL-2.0，见 `licenses/` 与 README「License Notice」）。**上游文件的版权头一律不动**；新建文件加自家 AGPL 版权头（参考现有文件头部样式）

## 仓库结构

```
kunpeng-engine/
├── kunpeng/           # 核心引擎（settings.gradle 按二级目录自动 include）
│   ├── bpm-model/     # BPMN/DMN/XML 模型与解析（bpmn-model、dmn-model、model-parse、xml-model）
│   ├── broker/        # broker 本体与客户端（broker、broker-client、broker-admin-client）
│   ├── cluster/       # 集群能力（cluster、cluster-config、cluster-dispatch 调度子系统、cluster-raft）
│   ├── engine/        # 执行引擎（bpmn-engine、dmn-engine、script-engine）
│   ├── gateway/       # 网关（gateway、gateway-grpc、gateway-job、gateway-protocol、gateway-rest）
│   ├── protocol/      # 协议层（protocol-admin(-impl)、protocol-business(-impl)、protocol-common；Record/LifeCycle 定义处）
│   ├── repository/    # 持久化（admin-repository、business-repository、kvstore、rocksdb）
│   ├── eventlog/      # 分区级事件日志（无锁定序、EL 批帧、AIMD 流控）→ 技能 eventlog-design
│   ├── scheduler/     # Actor 调度器（gate + wakeTickets 并发协议）→ 技能 scheduler
│   ├── sink/          # sink 日志外发（sink-api 契约、sink-common、sink 实现）→ 技能 sink-design
│   ├── structpack/    # Record 序列化层（key-id 自描述帧）→ 技能 structpack
│   ├── configuration/ # 引擎配置
│   └── utils/         # 公共工具
├── clients/           # 客户端 SDK（client-java、spring-boot-starter-client）
├── connectors/        # Camunda Connectors 生态 fork——独立 Maven 工程（自带 .mvn/parent），勿用 Gradle 构建
├── backup-stores/     # 备份存储（当前为空，占位）
├── bom/               # 三个 BOM：dependency-bom（总依赖）、client-sdk-bom、connector-sdk-bom
├── dist/              # 发行版装配
├── licenses/          # 多许可文件（AGPL-3.0 / Apache-2.0 / MPL-2.0）
├── buildSrc/          # 自研 Gradle 插件：kunpeng.code-spotless（格式化）、kunpeng.build-publish、kunpeng.spring-boot、kunpeng.sbe-java、kunpeng.grpc-java、kunpeng.dependency-update + common-config.gradle（私仓凭据等）
├── .agents/skills/    # 技能体系（见下文「技能体系」）
├── .claude/           # Claude Code 配置（settings.local.json 启用 codegraph MCP）
└── .codegraph/        # codegraph 工具索引数据
```

> **模块发现规则**：`settings.gradle` 扫描 `kunpeng/` 各固定二级目录与根下带 `build.gradle` 的目录自动 include——**新增模块 = 建目录 + build.gradle**，无需改 settings.gradle（例外才进 `excludes`）。

## 开发流程

1. **按「技能引用时机」选入口**（见下文技能体系章节的任务类型表）：跨模块日常迭代走 `iteration` 编排；单点工作先读对应技能——技能里是硬规则不是参考。
2. 编码 → 编译验证（模块粒度 `./gradlew :kunpeng:protocol:protocol-business:compileJava`，或整库 `./gradlew buildSkipTest`）。
3. 提交前：`./gradlew spotlessApply` **必须在仓库根目录整库执行**（模块级任务不存在，会报错），然后按提交规范写 commit message（英文 type 前缀分条、不带 AI 署名尾行、先审核再提交）。

## 编码原则

### 简洁编码

- 以功能实现为核心，保持代码简单高效；每个代码块都有明确目的
- 使用最直接的方式解决问题，避免不必要的抽象层
- 只在必要时添加异常处理和边界检查

### 避免过度设计

- 满足当前业务需求即可，不为未来不确定的需求预先设计
- 遵循"三次法则"：相似代码出现三次再考虑抽象，不要提前抽取
- 新增功能优先考虑对现有代码的最小改动，而非引入新架构

### 合理性评估

在执行要求前从四个维度评估，发现问题应指出并等待确认：

- **技术合理性**：是否符合项目技术栈与架构约定，是否有更合适的实现方式
- **业务合理性**：是否符合引擎定位，是否存在功能蔓延
- **规范一致性**：是否违反本文件、`.agents/skills/` 技能、既有代码风格
- **潜在风险**：并发正确性、序列化兼容（协议编号！）、性能、不必要的复杂度

```
用户提出要求 → 评估合理性 → 发现问题? ─ 是 → 指出问题 + 解释原因 + 给出建议 → 用户确认 → 执行
                              └ 否 → 直接执行
```

## 技能体系（.agents/skills/）

技能 = `.agents/skills/` 下一个子目录 + `SKILL.md`（YAML frontmatter `name` + `description`；**description 用英文**便于技能匹配，正文中文；目录/文件名一律英文；引用文档放同目录编号文件）。总览入口：[.agents/skills/SKILL.md](./.agents/skills/SKILL.md)（kunpeng-skills）。

| 技能 | 主题 | 何时用 |
|------|------|--------|
| `iteration` | 全链路迭代开发专家：跨模块/跨层既有功能改动的一次性编排（影响排查→计划确认→固定顺序执行→契约自检），大改动分流（广而浅→总控派发 / 深而窄→设计先行），协议兼容变更先评估不直接改 | 日常迭代改动跨 protocol/repository/engine/clients 多层（字段、校验、bug、枚举、状态机）；"改一下XX / 加个字段 / 迭代一下" |
| `record-standards` | 协议 Record 类（`*Record extends UnifiedRecordValue`）编写规范：类骨架、Property 字段类型、key 常量、getter/setter/addXxx、wrap/unwrap、嵌套对象 | 给 `*RecordValue` 实现具体 Record 类、增改字段/集合方法 |
| `enum-standards` | LifeCycle/State 枚举规范：`CommandValueLifeCycle` vs `CommandApiValueLifeCycle` 两种 archetype 契约、`PROCESS_INDEX_*`/`RECORD_INDEX_*` 编号池 | 新建/修改枚举、增删状态、申请或重排索引编号 |
| `repository-standards` | 持久化 Entity 规范（`*Entity extends UnpackedObject implements DbValue`，必须有 wrap/unwrap） | 新建/修改与 Record 对应的 Entity、字段落库映射与裁剪 |
| `structpack` | Record 序列化层：key-id 自描述帧格式、id 身份管理强规则、document（msgpack 变量）处理、性能基线 | 开发新 Record、排查序列化问题 |
| `scheduler` | Actor 调度器：gate + wakeTickets 并发协议、ActorCell 执行模型、相位机、future 语义、使用契约 | 理解调度原理、排查调度/启动链问题、写 actor 消费代码 |
| `eventlog-design` | 事件日志流：append 管线（无锁定序器/AIMD 流控）、EL 批帧格式、读路径、EventStore SPI 与 Raft 桥接 | 理解/改动 eventlog、排查恢复与流控问题 |
| `cluster-dispatch-design` | 集群调度子系统：调度计划生命周期、拓扑 diff 计划生成、执行明细下发与 ACK、延迟看门狗、source 治理 | 理解/改动 cluster-dispatch、排查调度问题 |
| `commit-standards` | 提交规范：spotlessApply 根目录整库执行、commit message 英文分条无署名、先审核再提交 | 改动准备提交前 |
| `sink-design` | sink 日志外发体系：RecordSink SPI 全生命周期、位置确认与重试、SinkService 运行时架构、SBE wire 协议与指标 | 理解/改动 sink、排查位置确认或暂停恢复问题 |
| `workjob` | 分布式 Job 推拉系统设计 v1.6：注册同步、竞争消费、broker 能力三原语、分区化数据面（leader 绑定与水位线）、deadline 簿记与 Resolve、故障矩阵 | 理解/改动 gateway/broker job 派发链路、排查 job 投递问题 |

新增技能时：建目录写 SKILL.md（含英文 description），并同步更新总览 `SKILL.md` 的模块表。

### 技能引用时机（按任务类型定入口）

| 任务类型 | 引用什么 |
|---------|---------|
| **日常迭代**：跨模块/跨层的既有功能改动（字段调整、校验/状态机修改、bug 修复、枚举值更新、接口参数变更） | `iteration` 全链路编排，一次完成「协议 + 存储 + 引擎逻辑 + 客户端 SDK + 测试 + 文档」 |
| 纯单模块内部实现/修 bug（不动协议契约） | 直接改，遵循对应规范技能（`record-standards` / `enum-standards` / `repository-standards`） |
| 序列化格式、协议编号池、新 Record key 定义 | `structpack` + `enum-standards`（破坏性评估先行，编号错=Raft 反序列化错位） |
| 理解/改动调度器、事件日志、集群调度子系统 | `scheduler` / `eventlog-design` / `cluster-dispatch-design` |
| 理解/改动 job 推拉派发链路（gateway-job / broker job 能力服务 / client 消费端） | `workjob`（设计契约：竞争消费、三原语、deadline 簿记） |
| 改动收尾提交 | `commit-standards`（spotlessApply 根目录整库 + 英文分条 message） |

> 分流规则（iteration 铁律）：跨层全链路改动用 iteration；**广而浅**的大改动走总控派发模式（iteration 当总控把各模块派发专项窗口）；**深而窄**的大改动（新算法/重构核心机制）走设计先行，iteration 不强行接；**破坏性协议变更**（重排编号池/改 key-id/改批帧格式）必须先出方案交评估，不直接改。

## 构建与常用命令

```bash
./gradlew build                       # 整库构建（含测试）
./gradlew buildSkipTest               # 跳过测试构建（根 build.gradle 注册）
./gradlew :kunpeng:eventlog:test      # 单模块测试（JUnit5 + jqwik）
./gradlew spotlessApply               # 格式化（仅根目录整库执行有效，提交前必跑）
./gradlew spotlessCheck               # 格式化校验

# connectors/ 是独立 Maven 工程
cd connectors && mvn clean install
```

- **JDK**：toolchain 25；`JavaExec`/`Test` 已统一注入 `--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED`，勿在模块里重复加
- **私仓**：`com.anyilanxin.*` 依赖走阿里云私仓（`buildSrc/common-config.gradle`，凭据读环境变量 `ALIYUN_REPO_USERNAME` 等），构建需凭据可达
- **根工程约束**：root 的 `boot*`/`jar` 任务已禁用（聚合壳）；无 `build.gradle` 的空目录除 `clean` 外任务全部禁用；jqwik 数据库已关（不落 `.jqwik-database`）

## 硬约束与已知坑

- **协议编号池不可乱动**：`PROCESS_INDEX_*` / `RECORD_INDEX_*` 有严格连续性规则，编号错会导致 **Raft 日志反序列化错位**——申请/重排编号必须走 `enum-standards` 技能流程。
- **序列化兼容**：structpack Record 的 key-id 是身份强管理（改字段=改协议），动手前读 `structpack` 技能；Entity 与 Record 必须成对同步 wrap/unwrap。
- **spotlessApply 只在根目录跑**：模块级不存在该任务，报错即用错了地方。
- **connectors 用 Maven**：`connectors/` 是 Camunda Connectors fork、自带 Maven Wrapper 与 parent，Gradle 命令对它无效。
- **版权头**：上游（Zeebe/Camunda 系）文件保留原版权头不动；新文件加自家 AGPL 头。
- **格式化即门禁**：Spotless（googleJavaFormat，经 `kunpeng.code-spotless` 插件）在编译前自动 apply，本地提交前手动跑一遍避免 CI 噪音。

## AGENTS 规范与多工具识别

- 本文件遵循 [AGENTS.md 开放规范](https://agents.md)：仓库根目录 `AGENTS.md`、纯 Markdown、UTF-8；**子目录可放 `AGENTS.md` 叠加生效**（代理进入该目录工作时合并上下文）——新增子目录级约束时建议采用，而不是把所有内容堆进本文件。
- **Claude Code 识别**：根 `CLAUDE.md` 仅含 `@AGENTS.md` 引用（Claude Code memory import 语法），内容与本文件同源；新版 Claude Code 亦原生读取 AGENTS.md，两条路径都指向本文件。
- **MCP**：`.claude/settings.local.json` 已启用 codegraph MCP server（代码图谱检索）。

## 文档导航

| 文档 | 用途 |
|------|------|
| [README.zh-CN.md](./README.zh-CN.md) | 项目简介与许可说明 |
| [.agents/skills/SKILL.md](./.agents/skills/SKILL.md) | 技能总览索引（kunpeng-skills） |
| [buildSrc/common-config.gradle](./buildSrc/common-config.gradle) | 私仓/仓库与公共构建配置 |
| [dist/README.md](./dist/README.md) | 发行版装配说明 |
