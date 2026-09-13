# kunpeng-dmn-engine 更改说明

[English](./CHANGES.md)

本模块的代码来源于 Camunda 7 平台的 dmn-engine 模块（独立 DMN 决策引擎，
负责解析与执行 DMN 决策模型）：

- 上游仓库：<https://github.com/camunda/camunda-bpm-platform>
- 导入时的来源提交（含上游最后一次提交）：<https://github.com/camunda/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>

## 为什么 Fork

上游 camunda-bpm-platform 仓库已不再积极维护，而本项目需要快速迭代。为避免受制于无维护的上游，
将所需的引擎模块导入本仓库，在此持续维护与演进。

## 本仓库的更改

1. 包名由上游的 `org.camunda.bpm.dmn.*` 重命名为 `com.anyilanxin.kunpeng.engine.dmn.*`。
2. 未保留上游的 Maven `pom.xml`，模块直接接入本仓库的 Gradle 统一构建（`build.gradle`）。
3. 上游文件原有的版权与许可声明保留，本仓库的修改以追加版权行的方式标注。

## 版权与许可注意事项

- 上游原有的版权与许可声明必须保留，请勿修改或删除原文件中的 copyright/license header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准
  （`licenses/` 目录下备有 Apache-2.0 与 MPL-2.0 文本）。
