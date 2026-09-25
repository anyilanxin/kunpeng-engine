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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.repository.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.google.common.collect.ImmutableSet;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 基于 RocksdbTransactionDb 的 AdminRepository 功能测试：position/key/delayed/source 子模块。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class RocksdbAdminRepositoryTest {

  @TempDir Path tempDir;

  private KvStore<AdminRepositoryColumnFamilies> db;
  private AdminRepository repository;

  @BeforeEach
  void setUp() {
    db =
        new com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory<AdminRepositoryColumnFamilies>()
            .createDb(
                tempDir.toString(),
                new SimpleMeterRegistry(),
                1,
                new RocksdbConfiguration(),
                AdminRepositoryColumnFamilies.DEFAULT);
    repository =
        new RocksdbAdminRepositoryFactory(
                db,
                new PartitionSourceMetadata(1, 1, ImmutableSet.of(1)),
                new SimpleMeterRegistry())
            .create();
  }

  @AfterEach
  void tearDown() throws Exception {
    db.close();
  }

  @Test
  void shouldTrackProcessedPosition() {
    // 未处理过任何事件时返回 -1
    assertThat(repository.positionRepository().getLastSuccessfulProcessedRecordPosition())
        .isEqualTo(-1L);

    repository.positionRepository().markAsProcessed(42L);
    assertThat(repository.positionRepository().getLastSuccessfulProcessedRecordPosition())
        .isEqualTo(42L);

    repository.positionRepository().markAsProcessed(100L);
    assertThat(repository.positionRepository().getLastSuccessfulProcessedRecordPosition())
        .isEqualTo(100L);
  }

  @Test
  void shouldGenerateMonotonicallyIncreasingKeys() {
    final var first = repository.keyRepository().nextKey();
    final var second = repository.keyRepository().nextKey();

    assertThat(second).isGreaterThan(first);

    // setKeyIfHigher 只抬升水位，不回退
    repository.keyRepository().setKeyIfHigher(first);
    final var third = repository.keyRepository().nextKey();
    assertThat(third).isGreaterThan(second);
  }

  @Test
  void shouldPersistPositionAndKeysAcrossRepositoryRecreate() {
    repository.positionRepository().markAsProcessed(77L);
    final var lastKey = repository.keyRepository().nextKey();

    // 同一数据库上重建仓库实例，水位应恢复
    final var recreated =
        new RocksdbAdminRepositoryFactory(
                db,
                new PartitionSourceMetadata(1, 1, ImmutableSet.of(1)),
                new SimpleMeterRegistry())
            .create();
    assertThat(recreated.positionRepository().getLastSuccessfulProcessedRecordPosition())
        .isEqualTo(77L);
    assertThat(recreated.keyRepository().nextKey()).isGreaterThan(lastKey);
  }

  private static DelayedRecord delayedRecord(
      final long delayedId, final long dueDate) {
    // structpack 序列化要求枚举属性必须显式设置
    return new DelayedRecord()
        .setDelayedId(delayedId)
        .setDueDate(dueDate)
        .setPartitionType(PartitionType.BUSINESS)
        .setDelayedType(DelayedType.BUSINESS_EXECUTION);
  }

  @Test
  void shouldSaveReadAndDeleteDelayedRecords() {
    final var delayed = repository.delayedRepository();

    final var record =
        new DelayedRecord()
            .setDelayedId(10L)
            .setDueDate(1_000L)
            .setDispatchPlanId(1L)
            .setDispatchPlanExecutionId(2L)
            .setPartitionType(PartitionType.BUSINESS)
            .setDelayedType(DelayedType.BUSINESS_EXECUTION);
    delayed.save(10L, record);

    final var read = delayed.getDispatchDelayed(10L);
    assertThat(read).isNotNull();
    assertThat(read.getDueDate()).isEqualTo(1_000L);
    assertThat(read.getDispatchPlanId()).isEqualTo(1L);
    assertThat(read.getDispatchPlanExecutionId()).isEqualTo(2L);

    // 不存在的延时任务
    assertThat(delayed.getDispatchDelayed(999L)).isNull();

    // 删除后主记录与索引一并清理
    delayed.delete(10L, record);
    assertThat(delayed.getDispatchDelayed(10L)).isNull();
  }

  @Test
  void shouldProcessDueDelayedRecordsInDueDateOrder() {
    final var delayed = repository.delayedRepository();

    delayed.save(1L, delayedRecord(1L, 100L));
    delayed.save(2L, delayedRecord(2L, 200L));
    delayed.save(3L, delayedRecord(3L, 300L));

    // 时间戳 250：到期的 100/200 被消费，返回下一个未到期的 dueDate=300。
    // visitor 收到的 record 是复用的 flyweight 实例，必须立即提取标量值
    final var visitedIds = new java.util.ArrayList<Long>();
    final var nextDueDate = delayed.processDelayBefore(250L, record -> {
      visitedIds.add(record.getDelayedId());
      return true;
    });

    assertThat(visitedIds).containsExactlyInAnyOrder(1L, 2L);
    assertThat(nextDueDate).isEqualTo(300L);

    // processDelayBefore 只扫描不删除：删除 1L 后，再次扫描到期任务时它被跳过
    delayed.delete(1L, delayedRecord(1L, 100L));
    final var secondRoundIds = new java.util.ArrayList<Long>();
    final var afterAll = delayed.processDelayBefore(350L, record -> {
      secondRoundIds.add(record.getDelayedId());
      return true;
    });
    assertThat(secondRoundIds).containsExactly(2L, 3L);
    // 全部到期且消费后没有下一个到期时间
    assertThat(afterAll).isEqualTo(-1L);
  }

  @Test
  void processDelayVisitorCanStopConsumption() {
    final var delayed = repository.delayedRepository();
    delayed.save(1L, delayedRecord(1L, 100L));
    delayed.save(2L, delayedRecord(2L, 100L));

    // visitor 返回 false 表示停止，本轮不继续消费
    final var visits = new AtomicInteger();
    final var nextDueDate =
        delayed.processDelayBefore(
            500L,
            record -> {
              visits.incrementAndGet();
              return false;
            });

    assertThat(visits.get()).isEqualTo(1);
    assertThat(nextDueDate).isEqualTo(100L);
    // 数据未被消费，仍然可读
    assertThat(delayed.getDispatchDelayed(2L)).isNotNull();
  }

  @Test
  void shouldManageNodeSources() {
    final var source = repository.sourceRepository();

    final var meta = new NodeSourceMetaRecord().setVersion(1).setCreateTime(1_000L);
    source.save(meta);
    assertThat(source.getNodeSourceMeta()).isNotNull();
    assertThat(source.getNodeSourceMeta().getVersion()).isEqualTo(1);

    source.update(new NodeSourceMetaRecord().setVersion(2).setUpdateTime(2_000L));
    assertThat(source.getNodeSourceMeta().getVersion()).isEqualTo(2);

    final var node = new NodeSourceRecord().setMemberId("node-1").setSourceId(7);
    source.applied(node);
    final var readNode = source.getNodeSource("node-1");
    assertThat(readNode).isNotNull();
    assertThat(readNode.getSourceId()).isEqualTo(7);

    assertThat(source.getNodeSource("no-such-node")).isNull();
  }

  @Test
  void shouldExposeAppliersAndTransactionContext() {
    assertThat(repository.getAppliers()).isNotNull();
    assertThat(repository.getContext()).isNotNull();
    // 6 个子模块的读写访问器都可用
    assertThat(repository.adminRepository()).isNotNull();
    assertThat(repository.businessRepository()).isNotNull();
    assertThat(repository.delayedRepository()).isNotNull();
    assertThat(repository.keyRepository()).isNotNull();
    assertThat(repository.positionRepository()).isNotNull();
    assertThat(repository.sourceRepository()).isNotNull();
  }

  @Test
  void shouldRunOperationsInRepositoryTransactionAtomically() throws Exception {
    final var positionBefore =
        repository.positionRepository().getLastSuccessfulProcessedRecordPosition();

    try {
      repository
          .getContext()
          .runInTransaction(
              () -> {
                repository.positionRepository().markAsProcessed(555L);
                throw new IllegalStateException("abort batch");
              });
    } catch (final RuntimeException ignored) {
      // 事务操作失败，整个批次回滚
    }

    // 回滚后 position 不应推进
    assertThat(repository.positionRepository().getLastSuccessfulProcessedRecordPosition())
        .isEqualTo(positionBefore);
  }

  @Test
  void shouldListPartitionSources() {
    final var source = repository.sourceRepository();
    assertThat(source.getPartitionSources()).isEmpty();

    // 通过 applier 通道写入一条分区源记录（APPLIED 生命周期，value=2）
    final var record =
        new PartitionSourceRecord()
            .setPartitionGroup("test-group")
            .setPartitionId(1)
            .setSourceId(7);
    repository
        .getAppliers()
        .applyState(
            1L,
            com.anyilanxin.kunpeng.protocol.admin.AdminValueType.PARTITION_SOURCE,
            com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle.fromProtocolValue(
                com.anyilanxin.kunpeng.protocol.admin.AdminValueType.PARTITION_SOURCE, (short) 2),
            record);

    final List<PartitionSourceRecord> sources = source.getPartitionSources();
    assertThat(sources).hasSize(1);
    assertThat(sources.get(0).getPartitionGroup()).isEqualTo("test-group");
    assertThat(sources.get(0).getPartitionId()).isEqualTo(1);
    assertThat(source.getPartitionSource("test-group", 1)).isNotNull();
  }
}
