# 06 存储层（repository/business-repository + kvstore + rocksdb）

## 本端职责与边界

**职责（本端 owns）**：

- **状态落地**：EVENT Record → Entity → KvStore → RocksDB（applier 按 `EVENT/valueType/valueState` 三元组分发；直答型命令不进来）
- **读服务**：`Immutable*` 只读仓储族（流程定义缓存、async、key 发号等）供引擎查询
- **key 发号仓储**：`keyGeneratorRepository().nextKey(sourceId)`——分区 source 维度单调发号
- **持久化机制**：kvstore 抽象之下的 RocksDB 列族布局、快照与写前日志；分区事务 commit 语义
- **崩溃恢复语义**：Entity 是纯派生数据，重启由日志重放重建（wrap 纯赋值天然幂等）

**边界（不做）**：

- ❌ 不做业务判断——applier 是纯搬运（wrap + put），遇到非法状态不裁决
- ❌ 不被网关/客户端直接触达——唯一写路径是引擎 `addEvent`→applier，唯一读路径是引擎 immutable 仓储
- ❌ 不产生日志——只消费 EVENT；日志的生成与定序属于引擎与 eventlog

**输入 → 输出**：EVENT Record（引擎事务内）→ RocksDB 键值；查询请求 → Entity/Record 快照

> 定位一句话：存储是**仓库管理员**——按单（EVENT）入库、按单出库，不问货为什么进为什么出。

存储没有"存 Record"这回事——**EVENT Record 经 Applier 转成 Entity 才落 RocksDB**。直答型命令（05 模式 A）不涉及本章。

## 应用链

```
writer.addEvent(key, lifeCycle, requestId, value)          [05-engine]
  → processingCollect.addState(...)                        同事务
  → RocksdbBusinessRepositoryApplier.applyState(key, valueType, valueState, recordValue)
      applierMap.get(RecordType.EVENT, valueType, valueState)   # 三元组定位
  → BusinessApplier.applyState(key, recordValue)
  → XxxEntity.wrap(record) → KvStore put → RocksDB        （经分区事务 commit 持久化）
```

事务边界：`EngineProcessService` 的 `transaction.run/commit/rollback` 包住"日志追加 + 状态应用"，`EngineRollbackException` 触发整体回滚——**Entity 写入永远在事务内，不要在 applier 外另开写路径**。

## 新增持久化状态的三件套

以"流程实例"为参照：

| 件 | 位置 | 规范来源 |
|----|------|---------|
| 域 LifeCycle 状态 + Record 字段 | `protocol-business/.../record/command/xxx/XxxLifeCycle.java` + Record | `enum-standards` / `record-standards` |
| Entity + wrap/unwrap | `business-repository/.../modules/xxx/record/XxxEntity.java` | `repository-standards` |
| Applier 注册 | `business-repository/.../modules/xxx/applier/RepositoryXxxApplierRegister.java` | 本文 |

### Entity 规范要点（细节看 repository-standards）

```java
public class JobEntity ... {   // extends UnpackedObject implements DbValue
  public void wrap(final JobRecord record) { ... }    // Record → Entity：字段逐个搬运
  public JobRecord unwrap(final JobRecord record) { ... }   // Entity → Record：reset 后回填
}
```

- `wrap`/`unwrap` **必须成对且覆盖全部字段**——编译只是底线，漏一个字段 = 读写不对称的静默数据丢失
- 集合属性（如 `starterEventsProp`）两边都要循环搬运（参照 `ProcessDefinitionEntity`）
- 字段裁剪（不落库的 Record 字段）允许，但要在 Entity 类注释里写明裁剪清单

### Applier 注册两处

1. 模块内：`RepositoryXxxApplierRegister.register(appliers, repository)` 逐状态挂 `BusinessApplier`（`valueType()` + `valueState()` 双标记，只收 `RecordType.EVENT`）
2. 总注册：`RocksdbBusinessRepositoryApplier.initRegister()` 加一行（**漏了 = EVENT 静默不落库**，applier 为 null 直接跳过，不报错——最危险的遗漏点）

## 读路径

- 引擎内读：`writer.getRepository().xxxRepository()`（`ImmutableBusinessRepository` 族，快照读）
- key 发号：`writer.getKeyRepository()` / `keyGeneratorRepository().nextKey(sourceId)`（与实体路由分区绑定）
- RocksDB 细节与列族布局见 `repository/rocksdb`、`repository/kvstore`（KvStore 是 RocksDB 之上的 KV 抽象，repository 不直接摸 RocksDB API）

## 快照与恢复（背景）

分区重启 = eventlog 重放：COMMAND/EVENT 逐条再走一遍处理器 + applier，Entity 状态重建。因此：

- **处理器必须可重放**（幂等），applier 也必须（wrap 是纯赋值，天然幂等）
- Entity 是纯派生数据，理论上可删库重放重建——不要往 Entity 里塞只有首次写入才有的信息

## 验证

```bash
./gradlew :kunpeng:repository:business-repository:compileJava
```

Entity 测试参照模块内现有测试：wrap→unwrap 断言字段往返一致；applier 测试断言 KvStore 键值。
