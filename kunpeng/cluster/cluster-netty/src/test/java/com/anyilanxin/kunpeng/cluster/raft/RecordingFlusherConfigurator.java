/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftRule.Configurator;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Builder;
import com.anyilanxin.kunpeng.cluster.raft.journal.CheckedJournalException.FlushException;
import com.anyilanxin.kunpeng.cluster.raft.journal.Journal;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogFlusher;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * 测试辅助配置器：给集群中每个节点装配一个"真刷盘 + 计数"的 flusher 工厂，用于统计
 * {@link com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog} 层面的刷盘次数。
 *
 * <p>注入的 flusher 每次 {@link RaftLogFlusher#flush} 都先执行
 * {@link RaftLogFlusher.Factory#DIRECT} 的真刷盘（绝不能是空操作，否则性能数字失真），
 * 再累加计数并回调 {@link #onFlush(LongConsumer)} 设置的观察者。
 *
 * <p>沿 {@link FaultyFlusherConfigurator} 的存储替换模式注入；替换后的存储沿用
 * {@link RaftStorage.Builder} 缺省配置（32MB 段大小，与生产默认一致）。
 */
public final class RecordingFlusherConfigurator implements Configurator {

  private final AtomicLong flushCount = new AtomicLong();
  private final ConcurrentMap<String, AtomicLong> flushCountPerNode = new ConcurrentHashMap<>();
  private volatile LongConsumer onFlush = ignored -> {};

  /**
   * 设置每次刷盘后的回调。
   *
   * <p>回调收到当前累计刷盘次数，在刷盘线程上同步执行，必须轻量。
   *
   * @param consumer 刷盘观察者
   * @return 自身，便于链式调用
   */
  public RecordingFlusherConfigurator onFlush(final LongConsumer consumer) {
    this.onFlush = Objects.requireNonNull(consumer, "consumer 不能为空");
    return this;
  }

  /** 当前累计刷盘次数（跨所有节点）。 */
  public long flushCount() {
    return flushCount.get();
  }

  /** 指定节点的累计刷盘次数；该节点尚未创建 flusher 时返回 0。 */
  public long flushCount(final String nodeId) {
    final var counter = flushCountPerNode.get(nodeId);
    return counter == null ? 0L : counter.get();
  }

  /** 已创建过 flusher 的节点 id 集合。 */
  public Set<String> knownNodeIds() {
    return Set.copyOf(flushCountPerNode.keySet());
  }

  /** 清零全部计数（不影响已注册的回调）。 */
  public void reset() {
    flushCount.set(0);
    flushCountPerNode.values().forEach(counter -> counter.set(0));
  }

  /** 创建带计数的 flusher 工厂：先真刷盘，再计数并触发回调。 */
  private RaftLogFlusher.Factory recordingFlusherFactory(final String nodeId) {
    return threadContextFactory -> {
      final var nodeCounter =
          flushCountPerNode.computeIfAbsent(nodeId, ignored -> new AtomicLong());
      return new RaftLogFlusher() {
        @Override
        public void flush(final Journal journal) throws FlushException {
          // 先真刷盘，保证刷盘行为与性能测量都等价于 DIRECT 策略
          RaftLogFlusher.Factory.DIRECT.flush(journal);
          final long current = flushCount.incrementAndGet();
          nodeCounter.incrementAndGet();
          onFlush.accept(current);
        }

        @Override
        public boolean isDirect() {
          // flush 同步返回即持久，语义等同 DIRECT；isDirect=true 使 RaftLog.forceFlush
          // 复用本 flusher，提交路径的合并刷盘次数才可被计数观测
          return true;
        }
      };
    };
  }

  /** 用装配了计数 flusher 的存储副本替换 builder 中的原存储。 */
  @Override
  public void configure(final MemberId id, final Builder builder) {
    final var original = Objects.requireNonNull(builder.storage, "storage 不能为空");
    builder.withStorage(
        RaftStorage.builder(builder.meterRegistry)
            .withDirectory(original.directory())
            .withSnapshotStore(original.getPersistedSnapshotStore())
            .withFlusherFactory(recordingFlusherFactory(id.id()))
            .build());
  }
}
