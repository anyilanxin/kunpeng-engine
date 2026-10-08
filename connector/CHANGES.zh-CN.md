# kunpeng-connectors 更改说明

[English](./CHANGES.md)

本目录的代码来源于 Camunda Connectors 仓库（connector SDK、运行时与开箱即用连接器）——凡未单独注明来源的内容均来自该仓库；来自其他项目的部分是例外，会在其所在目录单独附来源说明：

- 上游仓库：<https://github.com/camunda/connectors>
- 导入时的来源提交（含上游最后一次提交）：<https://github.com/camunda/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>

## 为什么 Fork

将项目导入本仓库一方面是为了日常维护方便，另一方面是为了后续可以自由修改与扩展——
按自身需要调整代码、加入自己的文件，而不受上游发布节奏的约束。

## 本仓库的更改

1. 导入时仅提取上游仓库根目录全部内容至本仓库 `connectors/` 目录：上游 Maven 工程结构
   （根 `pom.xml`、`parent/pom.xml`、Maven wrapper（`.mvn/`、`mvnw`）、`.github/` 工作流与
   QA 配置）原样保留；同时删除了所有 Camunda License 1.0 的文件。
2. 导入后的调整：文件协议头统一为标准 Apache 2.0 头并追加 anyilanxin 版权声明；删除了
   生成的 `element-templates/versioned/` 产物；统一代码风格；修正少量文档与版本引用。
3. **已裁剪为纯源码形态**：删除了上游 CI/QA（`.github/`、`.ci/`）、e2e 测试、上游文档、社区文件与
   Maven 根工程（根/parent `pom.xml`、Maven wrapper）；补充了 `LICENSE`、`licenses/`（Apache-2.0 /
   MPL-2.0）并更新了 `NOTICE`。
4. `connectors/` 仅作源码保留——不参与本仓库的 Gradle 统一构建，也不再以 Maven 构建（Maven 根工程已移除）。
5. **构建文件由 Maven 切换为 Gradle**：各模块目录均改为 `build.gradle`，依赖按原 `pom.xml` 翻译
   （内部 `io.camunda.connector` 构件映射为本地 `project(...)` 引用；版本取自本仓库 BOM 或按已删除的
   parent POM 显式锁定）；全部 `pom.xml` 已删除。`connectors/` 暂未纳入本仓库 Gradle 统一构建。
6. **目录更名为 `connector/` 并重新导入 HTTP 连接器族**：源码目录由 `connectors/` 更名为 `connector/`；
   重新导入上游 `http/http-base` 与 `http/rest`（`connector-http-json`）并配好 Gradle 构建文件
   （上游同级的 `polling`、`graphql` 模块未导入）。element-template-generator 各模块（`http-dsl`、
   `openapi-parser`、`postman-collections-parser`）原先指向外部的 `connector-http-base` 引用改为本地
   `:connector:http:http-base` 工程。

## 版权与许可注意事项

- 上游项目以 Apache 2.0 协议发布。文件协议头已在原有版权之上追加了 anyilanxin 版权声明，
   请勿修改或删除。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
