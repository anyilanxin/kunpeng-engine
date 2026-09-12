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

import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogReader;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.BusinessMetaEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.BusinessMetaStore;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分区业务元数据编排：raft 日志为真相源，{@code *.state} 文件为投影。
 *
 * <p>提交推进时增量扫描 (scannedIndex, commitIndex] 应用 BusinessMetaEntry 条目并投影写文件； 日志压缩形成缺口（appliedIndex+1
 * &lt; firstIndex）时经同步通道向 leader 拉取全量状态整体覆盖。
 */
public final class BusinessMetaManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(BusinessMetaManager.class);

  private final BusinessMetaStore store;
  private final PartitionBusinessMeta current = new PartitionBusinessMeta();

  /** 扫描水位：所有类型条目均已扫描到的日志 index（与 appliedIndex 分离，避免提交热路径重复解码应用条目）。 */
  private long scannedIndex = -1;

  /**
   * 同步水位：发起拉取时的日志 firstIndex。同一压缩缺口（firstIndex 不变）在 leader 确认无更新 （响应不新于 appliedIndex）后不再重复拉取；失败时由
   * markSyncFailed 重置以便重试。
   */
  private long syncAttemptedFirstIndex = -1;

  /** 状态实际推进后的回调（raft 线程触发）：RaftContext 借此通知业务元数据变更监听器。 */
  private Runnable stateChangeCallback = () -> {};

  public BusinessMetaManager(final BusinessMetaStore store) {
    this.store = store;
  }

  /** 注册状态变化回调（RaftContext 装配时调用）；启动投影加载、陈旧条目与未推进的同步响应不触发。 */
  public void setStateChangeCallback(final Runnable stateChangeCallback) {
    this.stateChangeCallback = stateChangeCallback;
  }

  /** 启动时从文件投影恢复（文件即最近一次应用的提交状态）。 */
  public synchronized void loadFromStore() {
    try {
      store
          .load()
          .ifPresent(
              state -> {
                current.apply(state.index(), state.term(), state.entries());
                scannedIndex = Math.max(scannedIndex, state.index());
                LOGGER.info(
                    "Loaded business meta from projection: index={}, term={}, keys={}",
                    state.index(),
                    state.term(),
                    state.entries().keySet());
              });
    } catch (final IOException e) {
      LOGGER.warn("Failed to load business meta projection, starting empty", e);
    }
  }

  /** 当前内存状态（只读）。 */
  public PartitionBusinessMeta current() {
    return current;
  }

  /**
   * 提交推进回调：增量扫描并应用 BusinessMetaEntry 条目（leader/follower 统一走此路径； 重启时由 initial setCommitIndex
   * 触发补放，scannedIndex 水位幂等去重）。 ApplicationEntry/ConfigurationEntry 等其他条目仅推进扫描水位，天然被 instanceof 跳过，
   * 因此每个提交批次至多扫描一次增量，不会重复解码已扫过的应用条目。
   */
  public synchronized void applyCommitted(final RaftLog log, final long commitIndex) {
    if (commitIndex <= scannedIndex) {
      return;
    }
    final long from = Math.max(scannedIndex + 1, log.getFirstIndex());
    try (final RaftLogReader reader = log.openUncommittedReader()) {
      reader.seek(from);
      while (reader.hasNext()) {
        final var entry = reader.next();
        if (entry.index() > commitIndex) {
          break;
        }
        if (entry.entry() instanceof final BusinessMetaEntry businessMetaEntry) {
          applyBusinessMeta(entry.index(), entry.term(), businessMetaEntry);
        }
        // 应用成功后才推进水位: 提前推进会让失败条目被永久跳过
        scannedIndex = entry.index();
      }
    } catch (final Exception e) {
      LOGGER.warn("Failed to replay business meta entries up to {}", commitIndex, e);
    }
  }

  /**
   * 判断是否需要向 leader 拉取全量状态：日志压缩缺口（appliedIndex+1 &lt; firstIndex）时为 true。 同一缺口首次判定时记录
   * syncAttemptedFirstIndex 并告警；leader 确认无更新后同一缺口不再重复拉取。
   */
  public synchronized boolean needsLeaderSync(final RaftLog log) {
    final long firstIndex = log.getFirstIndex();
    final boolean gap = current.appliedIndex() + 1 < firstIndex;
    if (!gap || firstIndex <= syncAttemptedFirstIndex) {
      return false;
    }
    syncAttemptedFirstIndex = firstIndex;
    LOGGER.warn(
        "Business meta replay gap: appliedIndex={}, log firstIndex={}; requesting sync from leader",
        current.appliedIndex(),
        firstIndex);
    return true;
  }

  /** 同步请求失败/leader 不可用：重置水位以便下次触发重试。 */
  public synchronized void markSyncFailed() {
    syncAttemptedFirstIndex = -1;
  }

  /** 应用 leader 下发的全量状态（载荷 = {@link BusinessMetaStore#encode} 编码），幂等防回退。 */
  public synchronized void applySyncedState(final byte[] payload) {
    final Optional<BusinessMetaStore.BusinessMetaState> state = BusinessMetaStore.decode(payload);
    if (state.isEmpty()) {
      LOGGER.warn("Leader sync returned unreadable business meta, allowing retry");
      markSyncFailed();
      return;
    }
    final var meta = state.get();
    if (meta.index() <= current.appliedIndex()) {
      // leader 确认无更新（含 leader 也为空）：保留水位，同一缺口不再重复拉取
      LOGGER.debug(
          "Leader sync state index {} not newer than applied {}, gap confirmed resolved",
          meta.index(),
          current.appliedIndex());
      return;
    }
    current.apply(meta.index(), meta.term(), meta.entries());
    scannedIndex = Math.max(scannedIndex, meta.index());
    try {
      store.store(meta.index(), meta.term(), meta.entries());
    } catch (final IOException e) {
      LOGGER.warn(
          "Failed to persist business meta projection at index {}; it is rebuildable from log",
          meta.index(),
          e);
    }
    stateChangeCallback.run();
  }

  /** 应用单条 busimeta 条目（整体覆盖语义；包可见供测试）：写投影文件失败仅告警。 */
  synchronized void applyBusinessMeta(
      final long index, final long term, final BusinessMetaEntry entry) {
    if (index <= current.appliedIndex()) {
      return;
    }
    final Map<String, String> entries = entry.entries();
    current.apply(index, term, entries);
    try {
      store.store(index, term, entries);
    } catch (final IOException e) {
      LOGGER.warn(
          "Failed to persist business meta projection at index {}; it is rebuildable from log",
          index,
          e);
    }
    stateChangeCallback.run();
  }
}
