---
name: record-standards
description: Use when writing or modifying protocol Record classes (*Record extends UnifiedRecordValue) in kunpeng-engine — creating a Record skeleton, adding or changing fields, choosing Property types, writing getters/setters/addXxx, choosing key constants, converting Entity-Record collections, or writing nested ObjectProperty setters
---

# Record 编写规范（协议 Record 类）

协议 Record 类（`*Record extends UnifiedRecordValue<Self>` 实现 `XxxRecordValue`，由 structpack 层序列化）的编写规范。

## 何时用

- 给 `*RecordValue` 接口实现具体的 `*Record` 类
- 新增字段、修改 setter、加 addXxx 方法
- 选择 Property 字段类型或 key 常量
- 写集合字段、`addXxx` 封装、Entity ↔ Record `wrap()`/`unwrap()` 互转
- 写嵌套 `ObjectProperty` 字段的 setter

## 速查

- **类骨架**：`extends UnifiedRecordValue<Self> implements XxxRecordValue` + `@AutoDeclareProperties`
- **字段命名**：内部叫 `xxxProp`，private final
- **Property id**：构造器首参为 id（强制，1..127），同一 Record 内唯一且永不复用；新字段可省略 id 由构建任务自动分配
- **key**：先查 `RecordConstant`，有就用常量；没有就写字面量字符串（**不要自己往 RecordConstant 加常量**）
- **getter**：`@Override` 来自接口的，读 `xxxProp.getValue()`
- **setter**：返回 `Self` 链式；String/Binary 类型必须有 String + DirectBuffer 两个重载
- **Buffer getter**：`@JsonIgnore public DirectBuffer getXxxBuffer()`，对 String/Binary 才提供
- **集合**：默认走 `addXxx(commandRecord)`，不要直接暴露 `ArrayProperty xxx()`
- **构造器**：`super(N)` 必须等于 `declareProperty` 调用次数
- **新记录底部**：`@Override protected Self newRecord() { return new Self(); }`
- **嵌套集合互转**：`wrap()` / `unwrap()` 必须遍历原始 `xxx()` 拿 `ArrayProperty`，**不要**用 `getXxx()`（会多分配一个 ArrayList）；String 走 Buffer 变体零拷贝
- **嵌套对象 setter**：`ObjectProperty` 禁止直接 `setValue(引用)`（别名共享，源对象复用后数据被污染），必须用 `BufferUtil.copyInto(source, target)` 桥接深拷贝（null 自动清空）；见章节 07

## 章节

| # | 文件 | 内容 | 何时看 |
|---|------|------|--------|
| 01 | [01-class-skeleton-and-constructor.md](./01-class-skeleton-and-constructor.md) | 类声明、`super(N)` 容量、`declareProperty` 链、Property id | 第一次搭骨架 |
| 02 | [02-property-field-types.md](./02-property-field-types.md) | `LongProperty`、`StringProperty`、`BinaryProperty`、`EnumProperty`、`ArrayProperty` 等 | 字段类型选型 |
| 03 | [03-key-constants.md](./03-key-constants.md) | 哪些 key 用 `RecordConstant` 常量、哪些用字面量 | 加新字段时 |
| 04 | [04-getters-and-setters.md](./04-getters-and-setters.md) | `String`/`DirectBuffer` 双 setter、`@JsonIgnore` Buffer getter | 写字段访问方法 |
| 05 | [05-collections-and-addxxx.md](./05-collections-and-addxxx.md) | `ArrayProperty` 暴露 vs `addXxx(commandRecord)` 封装、wrap/unwrap | 集合字段设计 |
| 06 | [06-typical-examples.md](./06-typical-examples.md) | 标量 / 集合 / 嵌套 Response Record 完整模板 | 抄代码时 |
| 07 | [07-nested-object-setter-bridge-copy.md](./07-nested-object-setter-bridge-copy.md) | `ObjectProperty` 嵌套 Record 的 setter 必须走 write→byte[]→read 桥接深拷贝 | 写嵌套对象 setter 时 |

## 编译命令

```bash
./gradlew :kunpeng:protocol:protocol-impl:compileJava --no-daemon
```

`BUILD SUCCESSFUL` 即可，immutables 警告与本规范无关。
