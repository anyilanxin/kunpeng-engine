---
name: bpm
description: BPMN 2.0.2 official specification reference (normative spec PDF + non-normative examples PDF) for element semantics, process classification, XSD definitions and quick pdftotext lookup during bpm-model/model-parse and engine development.
---

# BPMN 标准参考

## 文件

| 文件 | 性质 |
|------|------|
| [Business Process Model and Notation.pdf](./Business%20Process%20Model%20and%20Notation.pdf) | 规范正文（normative），BPMN 2.0.2（OMG，2013） |
| [BPMN 2.0 examples non-normative document.pdf](./BPMN%202.0%20examples%20non-normative%20document.pdf) | 官方示例集（non-normative），各元素图例与 XML 样例 |

## 何时用

- `kunpeng/bpm-model`（bpmn-model / model-parse / xml-model）开发时核对元素属性语义、结构约束与 XSD 定义
- 引擎实现与标准语义有出入、需要标准原文仲裁时
- 回答"BPMN 标准是否定义了 X"类问题（先查本页"已索引结论"，未命中再翻 PDF）

## 怎么查（PDF 无文本索引）

pdftotext 提取后 grep 关键词，按页脚页码/表号回 PDF 定位：

```bash
cd .agents/skills/bpm
pdftotext -q "Business Process Model and Notation.pdf" /tmp/bpmn-spec.txt
grep -n -i "keyword" /tmp/bpmn-spec.txt
```

规范正文排版惯例：属性定义在正文表格（Table N.M），机器约束在文末 XSD 章（`xsd:complexType`/`xsd:simpleType`），两处都要核对。

## 已索引结论

- [01-流程分类标准定义.md](./01-流程分类标准定义.md) —— 流程类型三分类（processType/isExecutable §10.2.1 + Table 10.1 + XSD）、Category/CategoryValue 标注分类（Table 8.22/8.23 + XSD）、Group 图形联动、supports 关联、属性组合矩阵与引擎落地对位，全文整理

### 流程分类的定义（2026-10-13 分析）

BPMN 2.0.2 对"流程分类"有明确定义，分三层：

1. **流程类型三分类** —— §10.2.1 "Types of BPMN Processes"（p.147）：
   - **Private Non-executable (internal)**：私有不可执行，面向文档化建模，不含执行细节（如条件表达式）
   - **Private Executable (internal)**：私有可执行，按 Clause 14 语义执行——引擎部署对象就是这类
   - **Public**：公开流程，只保留与外部参与者交互的活动与消息次序（BPMN 1.2 名为 abstract）
2. **XML 属性** —— Table 10.1（p.145）+ XSD `tProcess`：
   - `processType: {None | Private | Public}`，默认 `None`（未定义）；XSD 枚举 `tProcessType`
   - `isExecutable: boolean [0..1]`：与 Private 组合出三分类（Private+true / Private+false / Public）
3. **元素级语义分类** —— Table 8.22/8.23（p.69-70）`Category`/`CategoryValue`：用户自定义分类（如 Category="Region"，值 North/South/East/West），配合 Group 图形化展示，供文档与分析用途，**不参与执行语义**——不是流程类型，是标注机制。

结论：若引擎要落"流程分类"字段，标准对位物是 `processType` + `isExecutable`（结构化、参与语义）；业务侧自由分类（按部门/业务线分组）对位物是 `Category`（自由文本、不参与语义）。
