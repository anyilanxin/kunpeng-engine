---
name: repository-standards
description: Use when writing or modifying repository Entity classes (*Entity extends UnpackedObject implements DbValue) in kunpeng admin-repository — creating an Entity for a protocol Record, writing the mandatory wrap/unwrap Record conversion, mapping or pruning Record fields for persistence, or converting nested/collection/string fields between Entity and Record
---

# Entity 编写规范（repository 持久化实体）

admin-repository 中 Entity（`*Entity extends UnpackedObject implements DbValue`）的编写规范。Entity 是 **record 规则与 repository 规则的整合**：字段声明沿用 Record 规范（structpack Property 体系），互转方法遵守 repository 规范（必须有 `wrap`/`unwrap`）。

## 何时用

- 新建与 protocol `*Record` 对应的持久化 Entity
- Record 字段变化后同步 Entity（含 wrap/unwrap）
- 判断 Record 的哪些字段需要落库、哪些裁剪
- 写嵌套对象/集合/字符串字段的 Entity ↔ Record 互转

## 速查

- **类骨架**：`extends UnpackedObject implements DbValue` + `@AutoDeclareProperties`
- **字段声明**：与 record-standards 完全一致——`xxxProp` 命名、Property id（1..127）、key、`super(N)` 等于 `declareProperty` 次数、`// formatting:off` 链式声明
- **必须有 `wrap(final XxxRecord record)`**：`reset()` 后全字段 `setXxx(record.getXxx())` 链式赋值（从 Record 读入 Entity）
- **必须有 `unwrap(final XxxRecord record)`**：`record.reset()` 后 `record.setXxx(getXxx())` 链式回填，最后 `return record`（从 Entity 写回 Record）
- **字段可以裁剪**：Entity 只落库需要的字段（如 `PartitionSourceEntity` 不存 Record 的 target 字段）；wrap/unwrap 只同步实际存在的字段，unwrap 后 Record 其余字段保持默认值
- **嵌套 structpack 对象**：直接复用 Record 类型作 `ArrayProperty<XxxRecord>`（Entity 不另建嵌套 Entity 类），拷贝用 buffer round-trip（私有静态 `appendAll`）；标量集合（如 `ArrayProperty<IntegerValue>`）直接 `add().setValue(...)`，无需 buffer 拷贝
- **字符串字段**：`BufferUtil.bufferAsString` / `wrapString`（null 检查）+ `@JsonIgnore` Buffer getter，与 record-standards 相同
- **wrap/unwrap 与字段同步**：Record 增删字段时必须同步 Entity 属性 + wrap/unwrap，漏掉即静默丢数据

## 权威模板（参考实现）

| 模板 | 覆盖场景 |
|---|---|
| `modules/delayed/record/DelayedRecordEntity` | 纯标量字段（**基准模板**） |
| `modules/business/record/BusinessClusterMetaEntity` | 嵌套 `ArrayProperty<XxxRecord>` + buffer 拷贝 |
| `modules/business/record/BusinessDispatchPlanExecutionEntity` | String/Binary 字段 + `BufferUtil` |
| `modules/source/record/PartitionSourceEntity` | 字段裁剪（Record 的 target 字段不落库） |
| `modules/source/record/NodeSourceMetaEntity`、`PartitionSourceMetaEntity` | 字段裁剪（Record 的 sources 集合不落库，只存版本元信息） |

## 完整模板（DelayedRecordEntity 节选）

```java
@AutoDeclareProperties
public class DelayedRecordEntity extends UnpackedObject implements DbValue {
  // structpack-ids[DelayedRecordEntity]: 1,2,3,4,5,6
  private final LongProperty delayedIdProp = new LongProperty(1, "DELAYED_ID", -1);
  // ... 其余字段与 DelayedRecord 一一对应

  public DelayedRecordEntity() {
    super(6);
    // formatting:off
    declareProperty(delayedIdProp)
      // ... 声明链与 DelayedRecord 一致
      ;
    // formatting:on
  }

  public void wrap(final DelayedRecord record) {
    reset();
    setDelayedId(record.getDelayedId())
        .setPartitionType(record.getPartitionType())
        // ... 全字段链式
        ;
  }

  public DelayedRecord unwrap(final DelayedRecord record) {
    record.reset();
    record
        .setDelayedId(getDelayedId())
        .setPartitionType(getPartitionType())
        // ... 全字段链式
        ;
    return record;
  }

  // getter / fluent setter，与 record-standards 相同
}
```

## 字段映射规则

| Record 字段类型 | Entity 字段类型 | wrap/unwrap 写法 |
|---|---|---|
| 标量（Long/Integer/Enum…） | 同型 Property | `setXxx(record.getXxx())` / `record.setXxx(getXxx())` |
| String | `StringProperty` | 同上；setter 内部走 `wrapString`/`bufferAsString` |
| 嵌套 `ArrayProperty<XxxRecord>` | `ArrayProperty<XxxRecord>`（复用 Record 类） | 私有静态 `appendAll(source, target)`：`record.write(buffer)` → `target.add().wrap(buffer, 0, len)` |
| 标量集合 `ArrayProperty<IntegerValue>` | 同型 | `target.add().setValue(v.getValue())` 循环 |
| 不落库的字段 | 不声明 | wrap/unwrap 不出现；unwrap 后保持 Record 默认值 |

## 编译命令

```bash
./gradlew :kunpeng:repository:admin-repository:compileJava --no-daemon
```
