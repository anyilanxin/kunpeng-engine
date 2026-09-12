/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalMetaStore.InMemory;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.BusinessMetaEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ConfigurationEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.RaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.BusinessMetaStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link BusinessMetaManager} 应用与投影测试。 */
class BusinessMetaManagerTest {

  @TempDir Path directory;

  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  private static BusinessMetaEntry metaEntry(final String key, final String value) {
    return new BusinessMetaEntry(Map.of(key, value));
  }

  private static BusinessMetaEntry metaEntry(final Map<String, String> entries) {
    return new BusinessMetaEntry(entries);
  }

  private static ConfigurationEntry configurationEntry() {
    return new ConfigurationEntry(
        1234L,
        Set.of(
            new DefaultRaftMember(
                MemberId.from("0"), RaftMember.Type.ACTIVE, Instant.ofEpochSecond(1234))));
  }

  private RaftLog newRaftLog() {
    return RaftLog.builder(meterRegistry)
        .withDirectory(directory.toFile())
        .withName("busimeta-test")
        .withMetaStore(new InMemory())
        .build();
  }

  @Test
  void shouldApplyEntryAndProjectToFile() throws IOException {
    final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p1");
    final BusinessMetaManager manager = new BusinessMetaManager(store);

    manager.applyBusinessMeta(7, 2, metaEntry("sourceId", "5"));

    assertEquals(7, manager.current().appliedIndex());
    assertEquals("5", manager.current().entries().get("sourceId"));
    // 投影文件应携带真实 index/term
    final BusinessMetaStore.BusinessMetaState persisted = store.load().orElseThrow();
    assertEquals(7, persisted.index());
    assertEquals(2, persisted.term());
    assertEquals("5", persisted.entries().get("sourceId"));
    store.close();
  }

  @Test
  void shouldIgnoreStaleEntries() throws IOException {
    final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p2");
    final BusinessMetaManager manager = new BusinessMetaManager(store);

    manager.applyBusinessMeta(7, 2, metaEntry("sourceId", "5"));
    manager.applyBusinessMeta(6, 1, metaEntry("sourceId", "1"));

    assertEquals(7, manager.current().appliedIndex());
    assertEquals("5", manager.current().entries().get("sourceId"));
    store.close();
  }

  @Test
  void shouldReplaceWholeStateEnablingDeletion() throws IOException {
    final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p10");
    final BusinessMetaManager manager = new BusinessMetaManager(store);

    manager.applyBusinessMeta(
        7, 2, metaEntry(Map.of("sourceId", "5", "agentSourceIds", "1,3")));
    // 整体覆盖语义：未携带的 key 即删除（先清空再添加）
    manager.applyBusinessMeta(8, 2, metaEntry(Map.of("sourceId", "6")));

    assertEquals(Map.of("sourceId", "6"), manager.current().entries());
    assertEquals(8, manager.current().appliedIndex());
    // 投影文件同步体现删除
    assertEquals(Map.of("sourceId", "6"), store.load().orElseThrow().entries());
    store.close();
  }

  @Test
  void shouldLoadProjectionOnStart() throws IOException {
    final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p3");
    final BusinessMetaManager first = new BusinessMetaManager(store);
    first.applyBusinessMeta(7, 2, metaEntry("sourceId", "5"));
    store.close();

    // 重建：从文件投影恢复
    final BusinessMetaStore reopened = BusinessMetaStore.open(directory.toFile(), "p3");
    final BusinessMetaManager second = new BusinessMetaManager(reopened);
    second.loadFromStore();
    assertEquals(7, second.current().appliedIndex());
    assertEquals("5", second.current().entries().get("sourceId"));
    reopened.close();
  }

  @Test
  void shouldApplySyncedStateFromLeader() throws IOException {
    final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p4");
    final BusinessMetaManager manager = new BusinessMetaManager(store);

    // leader 下发的全量状态载荷（与 *.state 文件同格式）
    manager.applySyncedState(BusinessMetaStore.encode(9, 3, Map.of("sourceId", "8")));

    assertEquals(9, manager.current().appliedIndex());
    assertEquals("8", manager.current().entries().get("sourceId"));
    // 接收后同步落投影文件
    assertEquals(9, store.load().orElseThrow().index());
    store.close();
  }

  @Test
  void shouldReplayOnlyBusinessMetaEntriesUpToCommitIndex() throws IOException {
    try (final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p5");
        final RaftLog raftLog = newRaftLog()) {
      final BusinessMetaManager manager = new BusinessMetaManager(store);
      // [ConfigurationEntry@1, BusinessMetaEntry@2, ConfigurationEntry@3]
      raftLog.append(new RaftLogEntry(1, configurationEntry()));
      raftLog.append(new RaftLogEntry(1, metaEntry("sourceId", "5")));
      raftLog.append(new RaftLogEntry(1, configurationEntry()));

      manager.applyCommitted(raftLog, 2);

      assertEquals(2, manager.current().appliedIndex());
      assertEquals("5", manager.current().entries().get("sourceId"));

      // 推进到非 busimeta 的 @3：状态不变、无异常
      manager.applyCommitted(raftLog, 3);

      assertEquals(2, manager.current().appliedIndex());
      assertEquals("5", manager.current().entries().get("sourceId"));
    }
  }

  @Test
  void shouldIgnoreDuplicateCallback() throws IOException {
    try (final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p6");
        final RaftLog raftLog = newRaftLog()) {
      final BusinessMetaManager manager = new BusinessMetaManager(store);
      raftLog.append(new RaftLogEntry(1, configurationEntry()));
      raftLog.append(new RaftLogEntry(1, metaEntry("sourceId", "5")));

      manager.applyCommitted(raftLog, 2);
      // 同一 commitIndex 重复回调：扫描水位去重，状态不变、无异常
      manager.applyCommitted(raftLog, 2);

      assertEquals(2, manager.current().appliedIndex());
      assertEquals("5", manager.current().entries().get("sourceId"));
    }
  }

  @Test
  void shouldSkipStaleSyncedState() throws IOException {
    try (final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p7")) {
      final BusinessMetaManager manager = new BusinessMetaManager(store);
      manager.applyBusinessMeta(7, 2, metaEntry("sourceId", "5"));

      // 落后于内存水位的全量状态：不得回退内存与投影文件
      manager.applySyncedState(BusinessMetaStore.encode(5, 1, Map.of("sourceId", "1")));

      assertEquals(7, manager.current().appliedIndex());
      assertEquals("5", manager.current().entries().get("sourceId"));
      assertEquals(7, store.load().orElseThrow().index());
    }
  }

  @Test
  void shouldRequestSyncOncePerGap() throws IOException {
    try (final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p9");
        final RaftLog raftLog = newRaftLog()) {
      final BusinessMetaManager manager = new BusinessMetaManager(store);
      // 模拟快照安装后日志重置：appliedIndex(-1)+1 < firstIndex(10)，形成压缩缺口
      raftLog.reset(10);

      assertTrue(manager.needsLeaderSync(raftLog));
      // 同一缺口不重复发起
      assertFalse(manager.needsLeaderSync(raftLog));
      // 失败重置后可重试
      manager.markSyncFailed();
      assertTrue(manager.needsLeaderSync(raftLog));

      // leader 确认无更新（如 leader 同样为空）：同一缺口不再重复拉取
      manager.applySyncedState(BusinessMetaStore.encode(-1, 0, Map.of()));
      assertFalse(manager.needsLeaderSync(raftLog));
    }
  }

  @Test
  void shouldTolerateCompactionGapOnReplay() throws IOException {
    try (final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p8");
        final RaftLog raftLog = newRaftLog()) {
      final BusinessMetaManager manager = new BusinessMetaManager(store);
      raftLog.append(new RaftLogEntry(1, metaEntry("sourceId", "5")));
      manager.applyCommitted(raftLog, 1);
      assertEquals(1, manager.current().appliedIndex());

      // 模拟快照安装后日志重置：firstIndex(10) 越过 appliedIndex+1，形成压缩缺口
      raftLog.reset(10);

      // 缺口区间不可重放：不异常、不越界应用；重复回调仅告警一次
      manager.applyCommitted(raftLog, 10);
      manager.applyCommitted(raftLog, 10);

      assertEquals(1, manager.current().appliedIndex());
      assertEquals("5", manager.current().entries().get("sourceId"));
    }
  }

  @Test
  void shouldFireStateChangeCallbackOnlyOnActualChange() throws IOException {
    try (final BusinessMetaStore store = BusinessMetaStore.open(directory.toFile(), "p11");
        final RaftLog raftLog = newRaftLog()) {
      final AtomicInteger callbackCount = new AtomicInteger();
      final BusinessMetaManager manager = new BusinessMetaManager(store);
      manager.setStateChangeCallback(callbackCount::incrementAndGet);

      // 提交推进应用条目：回调 1 次
      raftLog.append(new RaftLogEntry(1, metaEntry("sourceId", "5")));
      manager.applyCommitted(raftLog, 1);
      assertEquals(1, callbackCount.get());

      // 陈旧条目（index 不大于 applied）：不回调
      manager.applyBusinessMeta(0, 0, metaEntry("sourceId", "1"));
      assertEquals(1, callbackCount.get());

      // leader 确认无更新（index 不新于 applied）：不回调
      manager.applySyncedState(BusinessMetaStore.encode(1, 1, Map.of("sourceId", "1")));
      assertEquals(1, callbackCount.get());

      // 同步全量状态推进：再回调 1 次
      manager.applySyncedState(BusinessMetaStore.encode(9, 3, Map.of("sourceId", "6")));
      assertEquals(2, callbackCount.get());

      // 启动投影加载（重建 manager）：不回调
      final BusinessMetaManager reloaded = new BusinessMetaManager(store);
      reloaded.setStateChangeCallback(callbackCount::incrementAndGet);
      reloaded.loadFromStore();
      assertEquals(2, callbackCount.get());
    }
  }
}
