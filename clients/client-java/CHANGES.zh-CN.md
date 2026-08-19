# kunpeng-clients-java 更改说明

[English](./CHANGES.md)

本模块的代码来源于 Camunda 8 / Zeebe 仓库的 java client 模块：

- 上游仓库：<https://github.com/camunda/camunda>
- 导入时的来源提交（含上游最后一次提交）：<https://github.com/camunda/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>

## 为什么 Fork

将模块导入本仓库一方面是为了日常维护方便，另一方面是为了后续可以自由修改与扩展——
按自身需要调整代码、追加自己的文件，而不受上游发布节奏的约束。

## 本仓库的更改

1. 当前按导入原样保留：上游 `io.camunda.client.*` 包名、Maven `pom.xml`
   以及 QA 配置（spotbugs、revapi）均未修改。
2. 后续将逐步合入本仓库的 Gradle 统一构建、进行包名重命名等调整，均在后续提交中完成。

## 版权与许可注意事项

- 上游原有的版权与许可声明必须保留，请勿修改或删除原文件中的 copyright/license header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
