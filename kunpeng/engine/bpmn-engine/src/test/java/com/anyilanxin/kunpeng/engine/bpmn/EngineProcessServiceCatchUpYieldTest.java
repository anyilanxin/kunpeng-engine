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
package com.anyilanxin.kunpeng.engine.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerClock;
import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.LogFlowControl;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.RocksdbBusinessRepositoryFactory;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.InstantSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * 引擎主循环追平期间让出：整个积压是一个 job，不设预算时会独占载体线程，外部提交的控制指令只能等积压排完。到量后必须经外部队列尾续续跑。
 *
 * <p>事件日志读器为 mock（按需产出跳过处理的条目），仓储为真实 RocksDB 临时库。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("引擎主循环追平期间让出")
class EngineProcessServiceCatchUpYieldTest {

  private static final int BACKLOG = 5_000;

  @TempDir Path tempDir;

  private ActorScheduler scheduler;
  private KvStore<BusinessRepositoryColumnFamilies> db;
  private EngineProcessService engine;

  /** 读器尚可产出的条目数；测试线程置位后才"有新日志" */
  private final AtomicInteger remaining = new AtomicInteger();

  /** 最近一次 onProcessed 的 position */
  private final AtomicLong lastProcessed = new AtomicLong(-1);

  @AfterEach
  void tearDown() throws Exception {
    if (scheduler != null) {
      scheduler.close();
    }
    if (db != null) {
      db.close();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("追平期间外部提交的控制任务在第一个分片之后就得到执行，且积压最终全部处理")
  void externallySubmittedJobRunsBeforeCatchUpBacklogIsDrained() throws Exception {
    openEngine();
    remaining.set(BACKLOG);
    final AtomicLong processedWhenControlJobRan = new AtomicLong(-1);

    // 同一线程背靠背：先触发追平（入外部队列），再投一个控制任务（排在其后）。
    // 第一个分片要逐条建条目，耗时远大于这两次入队之间的间隔
    engine.onRecordAvailable();
    engine
        .run(() -> processedWhenControlJobRan.set(lastProcessed.get()))
        .get(10, TimeUnit.SECONDS);

    assertThat(processedWhenControlJobRan.get())
        .as("控制任务执行时已处理的 position")
        .isBetween(0L, BACKLOG / 2L);
    assertTrue(await(() -> lastProcessed.get() == BACKLOG - 1, 10_000), "续跑应排完整个积压");
  }

  private void openEngine() throws Exception {
    db =
        new DefaultRocksdbFactory<BusinessRepositoryColumnFamilies>()
            .createDb(
                tempDir.resolve("live").toString(),
                new SimpleMeterRegistry(),
                0,
                new RocksdbConfiguration(),
                BusinessRepositoryColumnFamilies.DEFAULT);
    final EventLogWriter logStreamWriter = mock(EventLogWriter.class);
    final EventLog logStream = mock(EventLog.class);
    final EventLogReader reader = mock(EventLogReader.class);
    final BatchEntryReader batchReader = mock(BatchEntryReader.class);
    when(reader.hasNext()).thenAnswer(invocation -> remaining.get() > 0);
    when(reader.next())
        .thenAnswer(
            invocation -> {
              final long position = BACKLOG - remaining.getAndDecrement();
              final LoggedEntry entry = mock(LoggedEntry.class);
              when(entry.getPosition()).thenReturn(position);
              when(entry.isSkipProcessing()).thenReturn(true);
              return entry;
            });
    when(logStream.newReader()).thenReturn(reader);
    when(logStream.newBatchReader()).thenReturn(batchReader);
    when(logStream.newWriter()).thenReturn(logStreamWriter);
    when(logStream.getLastCommittedPosition()).thenReturn(0L);
    when(logStream.getLastWrittenPosition()).thenReturn(0L);
    final LogFlowControl flowControl = mock(LogFlowControl.class);
    doAnswer(
            invocation -> {
              lastProcessed.set(invocation.getArgument(0));
              return null;
            })
        .when(flowControl)
        .onProcessed(anyLong());
    when(logStream.getFlowControl()).thenReturn(flowControl);
    final RaftPartitionSource partitionSource = mock(RaftPartitionSource.class);
    when(partitionSource.getPartitionId()).thenReturn(PartitionId.from("business-raft", 1));
    final var repositoryFactory =
        new RocksdbBusinessRepositoryFactory(db, partitionSource, null, new SimpleMeterRegistry());

    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("catch-up-yield-test")
            .build();
    scheduler.start();

    engine =
        new EngineProcessService(
            mock(JobDeliveryPort.class),
            null,
            logStream,
            repositoryFactory,
            mock(CommandApiHandle.class),
            partitionSource,
            mock(InterPartitionCommandSender.class),
            new SimpleMeterRegistry(),
            TimerClock.passthrough(InstantSource.system()),
            scheduler);
    scheduler.submitActor(engine).get(10, TimeUnit.SECONDS);
  }

  private static boolean await(final BooleanSupplier condition, final long timeoutMillis)
      throws InterruptedException {
    final long deadline = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return true;
      }
      Thread.sleep(20);
    }
    return condition.getAsBoolean();
  }
}
