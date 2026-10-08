# kunpeng-script-connector 更改说明

[English](./CHANGES.md)

本目录的代码来源于 Camunda Community Hub 的 script-connector 项目——凡未单独注明来源的内容均来自
该项目；来自其他项目的部分是例外，会在其所在目录单独附来源说明：

- 上游仓库：<https://github.com/camunda-community-hub/script-connector>
- 导入时的来源提交（含上游最后一次提交）：<https://github.com/camunda-community-hub/script-connector/commit/7acc5cb4ba6c0750f7b366df38f112f30ecf8578>
- 上游仓库的 fork 存档（用作代码存档）：<https://github.com/anyilanxin/script-connector/commit/7acc5cb4ba6c0750f7b366df38f112f30ecf8578>

本目录是上级 `connector/` 树默认来源（Camunda Connectors 仓库）之外的例外来源，参见
[../CHANGES.zh-CN.md](../CHANGES.zh-CN.md)。

## 为什么 Fork

将项目导入本仓库一方面是为了日常维护方便，另一方面是为了后续可以自由修改与扩展——
按自身需要调整代码、加入自己的文件，而不受上游发布节奏的约束。

## 本仓库的更改

1. 导入时仅提取上游仓库根目录全部内容至本目录：上游 Maven 工程结构（根 `pom.xml`、`connector` 与
   `runtime` 两个模块、`.github/` 工作流与社区文件）原样保留。

## 版权与许可注意事项

- 上游项目以 Apache 2.0 协议发布。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
