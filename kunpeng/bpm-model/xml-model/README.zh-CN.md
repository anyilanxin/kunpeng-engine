# kunpeng-xml-model 更改说明

[English](./README.md)

本模块的代码来源于 Camunda 7 平台的 xml-model 模块（通用 XML 模型 API，BPMN/DMN 模型 API 的基础）：

- 上游仓库：<https://github.com/camunda/camunda-bpm-platform>
- 导入时的来源提交（含上游最后一次提交）：<https://github.com/camunda/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>

## 为什么 Fork

上游 camunda-bpm-platform 仓库已不再积极维护，而本项目需要快速迭代。为避免受制于无维护的上游，
将所需的模型模块导入本仓库，在此持续维护与演进。

## 本仓库的更改

1. 当前按导入原样保留：上游 `org.camunda.*` 包名与 Maven `pom.xml` 均未修改。
2. 后续将逐步合入本仓库的 Gradle 统一构建、进行包名重命名等调整，均在后续提交中完成。

## 版权与许可注意事项

- 上游原有的版权与许可声明必须保留，请勿修改或删除原文件中的 copyright/license header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
