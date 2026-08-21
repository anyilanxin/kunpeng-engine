# 快照子系统技术设计

> 模块：`com.anyilanxin.kunpeng.cluster.raft.snapshot`（含 `impl` / `transfer`）及 `raft/partition` 对接层。
> 本文为快照子系统的完整设计，含已定设计、接口契约与待决策事项。

---

## 1. 背景与目标

快照子系统为 Raft 提供三类能力：

1. **日志压缩边界**：日志一旦越过快照覆盖位点即可被安全截断。
2. **跨节点复制**：落后 follower 的 install 追赶、跨分区的引导/合并传输。
3. **业务状态备份**：把业务状态机序列化为一组文件持久化到磁盘。

设计目标：**存储与复制能力在模块内闭环，拍摄内容与拍摄时机完全由业务侧决定。**

---

## 2. 快照类型与持久快照类

`SnapshotType` 枚举三种类型，各自独立存储、独立语义，且对应**三个不同的持久快照类**：

| 类型 | 持久快照类 | 含义 | 触发/流向 | 存储目录 |
|---|---|---|---|---|
| `REGULAR` | `RaftSnapshot` | 常规快照，本节点本地定时拍摄 | 业务外部编排；leader 的快照用于 install 复制 | `snapshots/snapshot/` |
| `BOOTSTRAP` | `BootstrapSnapshot` | 引导快照，新分区启动时从指定源分区拉取 | 跨分区 **pull** | `snapshots/bootstrap/` |
| `MERGE` | `MergeSnapshot` | 合并快照，分区删除时其 leader 生成、推送到目标分区 leader | 跨分区 **push** | `snapshots/merge/` |

### 2.1 类层次

```java
/** 三种持久快照的公共契约（磁盘格式一致，仅类型不同）。 */
public interface PersistedSnapshot {
  SnapshotMetadata getMetadata();
  long getIndex();
  long getTerm();
  String getId();
  Path getPath();
  SnapshotType getType();
  int version();
  long size();
  SnapshotChunkReader newChunkReader();
  CloseableSilently reserve();
  void delete();
  boolean isCorrupt();
}

public interface RaftSnapshot extends PersistedSnapshot {}       // 常规
public interface BootstrapSnapshot extends PersistedSnapshot {}  // 引导
public interface MergeSnapshot extends PersistedSnapshot {}      // 合并
```

- 三者共享同一磁盘格式（manifest + 分目录 + `.sfc`）与内部实现基类，**仅公共类型不同**。
  拆分是**类型安全**手段，防止把 merge 当 bootstrap 使用。
- 实现类：`FileRaftSnapshot` / `FileBootstrapSnapshot` / `FileMergeSnapshot`，
  各自覆盖包私有基类并固定类型。

---

## 3. 快照标识与元数据

### 3.1 SnapshotId

- **独立类型**（非 String），承载 `index/term/nodeId` 三元组。
- **目录名 / 字符串形式**：`<index>-<term>-<hex(nodeId)>`——取 `IndexedRaftLogEntry` 的
  index()、term()，节点 id 以 **hex** 编码；`SnapshotMetadata.fromSnapshotId` 反向解码。

### 3.2 SnapshotMetadata（业务元数据清单）

```text
index-term-nodeId            # Raft 标识，同时是快照目录名
type                         # SnapshotType
version                      # 业务元数据版本（缺省 1）
biz.<key>=<value>            # 业务信息键值清单（任意多行）
```

### 3.3 manifest 与 .sfc

- `manifest`：行式文本，逐文件记录 `file,size,crc32,name`。
- `.sfc`：提交完成后生成的同名标记文件（SFV 风格逐文件 CRC32）；
  启动时缺失标记的目录视为未完整提交并清除。

---

## 4. 存储层接口

### 4.1 接口拆分

- `PersistedSnapshotStore`：本地持久快照的查询 / 拍摄 / 删除 / 联动。
- `ReceivableSnapshotStore extends PersistedSnapshotStore`：接收式快照入口。

### 4.2 查询（按类型显式方法，返回对应类型）

```java
// 常规快照
Optional<RaftSnapshot> getLatestSnapshot();          // 仅常规类型的最新
Optional<RaftSnapshot> getSnapshotAt(long index);
CompletableFuture<Long> getCompactionBound();        // 压缩下界

// 引导 / 合并（专用 getter，返回专用类型）
Optional<BootstrapSnapshot> getBootstrapSnapshot();
Optional<MergeSnapshot> getMergeSnapshot();
```

### 4.3 拍摄（按类型分方法）

```java
// 常规（raft）快照
Either<SnapshotException, PersistableSnapshot> newTransientSnapshot(
    long index, long term, Map<String, Object> businessInfo);

// 引导 / 合并（形态同 raft，类型与方法名待最终确认）
Either<SnapshotException, PersistableSnapshot> newBootstrapSnapshot(
    long index, long term, Map<String, Object> businessInfo);
Either<SnapshotException, PersistableSnapshot> newMergeSnapshot(
    long index, long term, Map<String, Object> businessInfo);

// 引导 / 合并：从既有快照"复制"产生（业务提供复制回调）
ActorFuture<PersistedSnapshot> copyForBootstrap(BiConsumer<Path, Path> copySnapshot);
```

`copyForBootstrap` 语义：`BiConsumer<Path, Path>` 为 `(源快照目录, 目标 pending 目录)`，
业务实现"如何复制/变换"；模块负责建 pending 目录、调用回调、再走三阶段提交。
**源快照目录的来源待确认**（见 §8-⑥）。

### 4.4 接收

```java
/**
 * 为给定快照 id（{@code index-term-nodeId}）创建待接收快照的临时目录，返回其 pending 句柄，
 * 由传输逻辑逐片写入、校验，直到 persist() 提交。
 *
 * @throws SnapshotException.SnapshotAlreadyExistsException 若已存在相同快照
 */
CompletableFuture<PersistableSnapshot> newReceivedSnapshot(String snapshotId);
```

- **接收与拍摄统一为 `PersistableSnapshot`**：不再有独立的 `ReceivedSnapshot` 类型。
  "拍摄"与"接收"都是 pending 快照，仅内容来源不同。
- 分片写入由模块传输逻辑依据 `SnapshotChunk` 定位到 `getPath()` 下并校验 checksum，
  `PersistableSnapshot` 本身不暴露 `apply`。

### 4.5 删除与联动

**删除统一命名为 `delete()`**，定义在三种持久快照类上（非 store 上的类型专用方法）：

```java
// 业务删除某类型快照：取最新 → delete()
store.getLatestSnapshot().ifPresent(RaftSnapshot::delete);        // 删除最新常规快照
store.getBootstrapSnapshot().ifPresent(BootstrapSnapshot::delete); // 删除引导快照
store.getMergeSnapshot().ifPresent(MergeSnapshot::delete);         // 删除合并快照
```

- 常规/引导/合并的保留策略：引导与合并**只保留最新一个**（新快照提交即删旧），常规按
  `maxSnapshotCount` 保留——因此"删最新"即清空该类型。
- store 级删除仅保留按索引的批量操作：

```java
CompletableFuture<Integer> deleteSnapshotsFrom(long index);  // 删除 index 及之后
ActorFuture<Void> abortPendingSnapshots();
void addSnapshotListener(PersistedSnapshotListener listener);
void removeSnapshotListener(PersistedSnapshotListener listener);
ActorFuture<Void> close();
```

#### PersistedSnapshotListener（commit 后通知）

```java
@FunctionalInterface
public interface PersistedSnapshotListener {
  void onNewPersistedSnapshot(PersistedSnapshot persistedSnapshot);
}
```

- **触发时机**：快照 **commit（持久化）之后**同步通知——无论来源是本地拍摄还是接收（install /
  跨分区拉取），提交完成即回调。
- **主要用途**：
  1. 日志压缩联动——`RaftContext` 注册监听器，`onNewPersistedSnapshot → logCompactor.compactFromSnapshots()`，
     快照一落盘就推进压缩边界；
  2. 业务处理——消费方按 `getType()` 区分常规/引导/合并，触发业务恢复或后续动作。
- **调用线程**：在提交完成线程（当前为异步提交的完成回调）同步执行，监听器实现不应执行耗时操作。

### 4.6 Pending 快照句柄

```java
public interface PersistableSnapshot {
  SnapshotId snapshotId();
  Path getPath();                             // 业务/传输直接写内容到该目录
  ActorFuture<PersistedSnapshot> persist();   // 提交（三阶段②③）
  ActorFuture<Void> abort();                  // 清理临时目录
}
```

---

## 5. 快照生命周期

### 5.1 拍摄（三阶段模型，业务外部编排）

```
阶段① 拍摄到临时目录（pending 快照）
  newTransientSnapshot(...) → 业务写 getPath() 目录（内容不可见）

阶段② 提交到目标快照目录（pending → 真实存储）
  persist() → 生成 manifest → 空快照拒绝 → 逐文件 CRC
           → 原子 move 到 snapshots/{type}/{id}/ → 目录 fsync
           → 生成同名 .sfc → 保留策略

阶段③ 删除临时目录
  成功：原子 move 已消费掉临时目录
  失败/空快照/异常：abort() 递归清理
```

- 三阶段全部由模块实现，业务只在阶段①提供内容文件。
- **pending 快照是一个列表**：同一节点可能同时"拍摄一个 + 接收一个"多个进行中的快照，
  每个 pending 各自一个临时目录，由存储层统一跟踪与清理。

编排示例：

```java
SnapshotProvider provider = ...;                       // 业务实现
var store = raftPartitionServer.getPersistedSnapshotStore();
final var pending = store.newTransientSnapshot(index, term, provider.businessInfo());
if (pending.isRight()) {
  provider.takeSnapshot(pending.get().getPath());      // 业务直接写目录
  pending.get().persist();                             // 提交
}
```

### 5.2 接收（清单先行，两步协议）

```
① 服务端发送安装请求，携带快照的 sfc（文件校验清单 = manifest）
② 接收方据清单创建接收快照目录
③ 拉取/接收清单中的文件内容（逐块写入、逐块校验）
④ 快照转为可用快照（校验 manifest → 原子提交 → .sfc）
⑤ 处理业务（listener 通知 → 压缩 / 业务恢复）
```

| 场景 | 清单来源 | 内容来源 |
|---|---|---|
| 常规 install（同分区） | leader 发送的 `InstallRequest`（沿用现有 raft install 逻辑） | leader 逐块 push `InstallRequest` |
| 引导（跨分区） | 第一步拉取远程快照清单（`getLatestSnapshot` 的元数据/sfc） | 第二步按清单逐批 pull 文件内容 |
| 合并（跨分区） | 源 leader 推送的首批携带（manifest 也在分片中） | 源 leader 逐批 push 到目标接收处理器 |

---

## 6. 复制与传输

| 场景 | 模型 | 机制 |
|---|---|---|
| 常规追赶（同分区 follower 落后到压缩区） | leader **push** | `shouldReplicateSnapshot`：有快照 ∧ follower 快照更旧 ∧ `log.firstIndex > member.currentIndex` → 逐块 `InstallRequest` |
| 引导（跨分区） | 新分区主动 **pull** | 目标分区知道源是 leader → `pullSnapshot(源分区Id, BOOTSTRAP, ...)` → 拓扑解析源分区 leader → 逐批拉取落盘 |
| 合并（跨分区） | 源分区主动 **push** | 源分区（被删除方）知道合并目标是 leader → 源 leader 把 MERGE 快照**推送到**目标分区 leader |

**方向设计依据**：传输方向由"发起方确定知道对方角色"决定——
- bootstrap：新分区启动时明确知道要从哪个分区的 **leader** 拉取（拉方有明确目标）→ pull；
- merge：被删除分区明确知道要把数据合并到哪个分区的 **leader**（推方有明确目标）→ push；
- raft install：leader 的 matchIndex 表最清楚 follower 落后程度 → leader push。

- 跨分区传输服务端只在 **leader** 时注册（角色经成员属性广播），离开 leader 自动卸载。
- 分区角色经成员属性 `raft.partition.{分区名}` 广播，`RaftPartitionTopology` 按需解析 leader。
- chunk 线格式：自描述二进制（magic + version + 帧类型 + 定长载荷）。

### 6.x 两种跨分区方向的角色分工

| | bootstrap（pull） | merge（push） |
|---|---|---|
| 源分区 leader | 注册**分片提供**服务端（按批发） | 主动**推送客户端**（逐批发给目标） |
| 目标分区 leader | 主动拉取客户端 | 注册**分片接收**服务端（按批收 → SnapshotChunkAppender） |
| 触发方 | 新分区启动 | 源分区删除前 |

merge 推送侧与 bootstrap 拉取侧复用同一套批量分片协议（仅方向相反）；目标侧接收处理器
同样只在 leader 角色注册、离开卸载。

### 6.1 分片契约 `SnapshotChunk`

```java
public interface SnapshotChunk {
  String getSnapshotId();        // 唯一快照标识
  int getTotalCount();           // 该快照的总分片数
  String getChunkName();         // 当前分片名（文件名@字节偏移）
  long getChecksum();            // 内容校验和（CRC32）
  byte[] getContent();           // 当前分片内容
  default ByteBuffer getContentBuffer() {
    return ByteBuffer.wrap(getContent()).asReadOnlyBuffer();
  }
  long getFileBlockPosition();   // 分片在文件内的偏移
  long getTotalFileSize();       // 分片所属文件的总大小
  long getContentLength();       // 分片内容长度（字节）
}
```

### 6.2 批量传输（传输单元 = 批，非单片）

传输以**批量分片（batch）**为单元，而非单个 `SnapshotChunk`：

- **单文件不限大小**：任意大的文件按分片尺寸切成多片，可跨批发送；
- **批量受限**：一批内打包多个分片（可来自多个文件），**累计字节数 ≤ maxBatchSize**；
  追加下一片会超限时封批发送；
- **maxBatchSize 在快照模块/传输服务创建时指定**（构造参数）；
- 批打包规则：按文件字典序、文件内偏移序依次取片，贪心装批；
- 每批携带"是否还有后续批"标志与末片位置（断点续传按批推进，起点 = 上批末片名）；
- 接收方对批内逐片校验 checksum、按 `文件名@偏移` 写入 pending 目录——**写入侧同样批量**：
  `SnapshotChunkAppender` 缓冲到达的分片，累计字节 ≥ **maxWriteBatchBytes（缺省 1MiB）** 时整批
  写盘（按文件分组、文件内按偏移定位，**每文件每批一次 force**）；单片超限独占一批（最少 1 片）；
  `verifyComplete()`/`persist()` 前先 `flush()` 清尾批（manifest 文件本身也是分片传输的）。

```
发送侧                                接收侧
  chunk 流(字典序)                      逐片校验+落盘
   ├─ 批1: [f1@0, f1@64k, f2@0]  ───→  写入 3 片
   ├─ 批2: [f2@64k, f3@0]        ───→  写入 2 片
   └─ (hasMore=false 结束批)      ───→  persist() 提交
```

全部批次完成后 `persist()` 提交、失败 `abort()` 清理——与拍摄的三阶段一致。

---

## 7. 存储能力清单

启动逐文件 CRC 校验、manifest/分片/目录落盘 fsync、快照预留防删、同位点幂等重拍、
空快照拒绝、接收 totalCount 校验、偏移式断点续传、`.sfc` 提交标记、store 删除/关闭。

---

## 8. 开放问题（待决策）

1. ~~删除语义~~ → **已定**：删除统一命名 `delete()`，定义在三种持久快照类上；引导/合并只保留
   最新一个（删最新即清空该类型），常规按 maxSnapshotCount 保留。
2. **Future 类型迁移范围**：拍摄侧已定 `Either` + `ActorFuture`、接收侧保持 `CompletableFuture`。
   是否需要把接收侧（及 RaftServer/分区/传输链路）统一到 `ActorFuture`？
3. **压缩边界范围**：`getCompactionBound()` 是否收窄为仅常规快照（引导/合并不参与日志压缩）？
4. **SnapshotProvider 最终形态**：保留为纯 SPI 是否足够，还是增加 `onRecoverFromSnapshot` 等
   恢复回调（对应草稿测试 `SnapshotHandler` 的三方法形态）？
5. **businessInfo 序列化**：`Map<String, Object>` 无法直接进文本 manifest——用 JSON/二进制序列化，
   还是限定标量类型？`version` 是否并入 businessInfo 还是恢复独立参数？
6. **copyForBootstrap 源路径**：源快照目录从哪来——默认最新常规快照，还是调用方显式指定？
7. **业务侧装配**：分区组装层的构造形态（草稿测试期望直接注入 membership/communication/topology 服务）。

---

## 9. 实现状态

### 已实现

- 三类型三类（`RaftSnapshot`/`BootstrapSnapshot`/`MergeSnapshot` + `FileXxx` 实现与 `FilePersistedSnapshot` 抽象基类）
- `SnapshotId` 类型 + hex 节点 id 编码（目录名 `<index>-<term>-<hex(nodeId)>`）
- 接口形态改造：类型化查询（`getLatestSnapshot`→`Optional<RaftSnapshot>` 等）、
  拍摄三方法 `Either<SnapshotException, PersistableSnapshot>` + `ActorFuture`、
  `copyForBootstrap`、接收统一为 `PersistableSnapshot`（取消 `TransientSnapshot`/`ReceivedSnapshot`）
- `PersistableSnapshot`（snapshotId/getPath/persist/abort）+ 拍摄/接收双模式 `FilePersistableSnapshot`
- `SnapshotChunkAppender`：接收侧分片写入与 totalCount 校验（install 与跨分区传输共用）
- 传输批量分片（`maxBatchSize`，缺省 4MiB，`RaftPartitionConfig.snapshotTransferMaxBatchSize` 可配；
  贪心装批、末片名续传、hasMore 收尾）
- 保留策略按类型：常规 `maxSnapshotCount`；引导/合并只留最新
- `getCompactionBound()` 仅统计常规快照
- `SnapshotChunk.getContentBuffer()` 只读 ByteBuffer
- pending 快照列表显式跟踪（`abortPendingSnapshots` 统一 abort）
- 存储能力清单（§7）、install 复制、跨分区传输、拓扑、三态监听器（此前已实现，本次适配）

### 待实现 / 待确认

- ~~merge 推送方向改造~~ → **已实现**：`SnapshotPushServer`（目标 leader 注册接收处理器，
  按快照 id 建会话、逐批经 Appender 批量写入、末批校验提交）+ `SnapshotPushClient`（源 leader
  贪心装批主动推送）+ `RaftPartitionServer.pushMergeSnapshot(目标分区Id, 尺寸)`；复用批量分片
  协议与装批逻辑（`fillBatch` 共用），方向由"谁知道对方角色"决定（bootstrap=目标拉 / merge=源推）
- §8 剩余开放问题（②接收侧 Future 是否统一 ActorFuture、④恢复回调、⑦业务侧装配形态）
- **行为兼容性提示**：目录名 hex 化后，旧明文目录名的存量快照重启会被清理；
  传输响应线格式变更为批量帧，跨版本混部不兼容
- `AtomixRaftSimpleTest`（业务集成草稿）待其引用的 API 落地后编译通过
