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
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftElectionConfig;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogFlusher;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试辅助配置器：当 {@code faultyWhen} 判定为真时，给编号最小的若干节点装配一个注定刷盘失败的
 * flusher，从而在测试中模拟磁盘故障场景；其余节点保持正常，保证集群仍能选出健康 leader。
 */
public record FaultyFlusherConfigurator(
    int faultyFlusherNumber,
    Supplier<Boolean> faultyWhen,
    Runnable notifyFaultyFlush,
    boolean leaderFaulty,
    boolean withDataLoss)
    implements Configurator {

  private static final Logger LOG = LoggerFactory.getLogger(FaultyFlusherConfigurator.class);

  /** 优先级选举的总投票权重上限。 */
  private static final int ELECTION_QUOTA = 5;

  /** 健康节点被强制压低后的最低优先级。 */
  private static final int MIN_SAFE_PRIORITY = 2;

  /**
   * 构造一个可控故障的 flusher 工厂：判定条件命中时抛出 {@link FlushException}，
   * 可选地先截断日志最后一条记录来模拟数据丢失。
   */
  private RaftLogFlusher.Factory brokenFlusherFactory() {
    return journalSupplier ->
        new RaftLogFlusher() {
          @Override
          public void flush(final Journal journal) throws FlushException {
            if (!faultyWhen.get()) {
              journal.flush();
              return;
            }
            notifyFaultyFlush.run();
            if (withDataLoss) {
              journal.deleteAfter(journal.getLastIndex() - 1);
            }
            throw new FlushException(new IOException("Failed sync"));
          }
        };
  }

  @Override
  public void configure(final MemberId id, final Builder builder) {
    final int numericId = Integer.parseInt(id.id());
    final boolean faulty = numericId <= faultyFlusherNumber;

    if (faulty) {
      LOG.trace("failing flusher for member {}", id);
      replaceStorageWithBrokenFlusher(builder);
    } else {
      LOG.trace("not failing flusher for member {} ", id);
    }
    builder.withElectionConfig(
        RaftElectionConfig.ofPriorityElection(ELECTION_QUOTA, priorityFor(numericId, faulty)));
  }

  /** 用装配了故障 flusher 的存储副本替换 builder 中的原存储。 */
  private void replaceStorageWithBrokenFlusher(final Builder builder) {
    final var original = Objects.requireNonNull(builder.storage);
    builder.withStorage(
        RaftStorage.builder(builder.meterRegistry)
            .withDirectory(original.directory())
            .withSnapshotStore(original.getPersistedSnapshotStore())
            .withFlusherFactory(brokenFlusherFactory())
            .build());
  }

  /**
   * 计算节点选举优先级：若允许 leader 也故障，则故障节点拿到高优先级；
   * 否则压低健康节点的优先级，确保它们优先当选。
   */
  private int priorityFor(final int numericId, final boolean faulty) {
    final int demoted = Math.max(ELECTION_QUOTA - numericId, MIN_SAFE_PRIORITY);
    if (leaderFaulty) {
      return faulty ? demoted : numericId;
    }
    return faulty ? numericId : demoted;
  }
}
