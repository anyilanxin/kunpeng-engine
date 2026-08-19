# kunpeng-connector 更改说明

[English](./CHANGES.md)

本目录的代码来源于 Camunda Connectors 仓库：

- 上游仓库：<https://github.com/camunda/connectors>
- 导入时的来源提交（含上游最后一次提交）：<https://github.com/camunda/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>

导入范围包括：Connector SDK、开箱即用的 Connectors、Connector Runtime、
element-template 生成器、secret providers、apps 与 e2e 测试，以及 Maven 构建。

## 为什么 Fork

将 Connectors 导入本仓库，一方面方便自己后续迭代，另一方面便于适配 kunpeng engine——
可以自由修改与扩展代码，不受上游发布节奏的约束。

## 本仓库的更改

1. **导入时删除了所有以 Camunda License 1.0 协议发布的代码**，本目录剩余代码均为
   Apache-2.0 许可（详见各自文件的 license header）。
2. 其余按导入原样保留：上游 `io.camunda.connector.*` 包名、Maven pom 与模块结构均未修改。
3. 后续将逐步合入本仓库的 Gradle 统一构建并适配 kunpeng engine，均在后续提交中完成。
4. **后续新增的文件，其适用协议以文件自身的 license header 与版权声明为准**，不再采用
   Apache 协议。

## 版权与许可注意事项

- 上游原有的版权声明必须保留，请勿修改或删除原文件中的 copyright header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
- 上游原始 README（Camunda Connectors 项目文档）已被本更改说明取代，
  其内容可通过上方链接的归档上游提交查看。
