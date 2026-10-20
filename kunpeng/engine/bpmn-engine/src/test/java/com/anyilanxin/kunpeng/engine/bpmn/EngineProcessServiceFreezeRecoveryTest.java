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
package com.anyilanxin.kunpeng.engine.bpmn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerClock;
import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.LogFlowControl;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryFactory;
import com.anyilanxin.kunpeng.repository.business.RocksdbBusinessRepositoryFactory;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.InstantSource;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 引擎级冻结标记恢复测试：broker 手术中重启后内存冻结态丢失，凭 PARTITION_STATE 持久标记重入冻结窗口； 冻结写标记、恢复先清标记再开闸、冻结窗口内幂等。
 *
 * <p>事件日志与命令面为 mock（空日志空提交），仓储为真实 RocksDB 临时库——静止判定在空日志下立即成立，排干 future 即时完成。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class EngineProcessServiceFreezeRecoveryTest {
  @TempDir Path tempDir;

  private ActorScheduler scheduler;
  private KvStore<BusinessRepositoryColumnFamilies> db;
  private BusinessRepository repository;
  private EventLogWriter logStreamWriter;
  private EventLog logStream;
  private EngineProcessService engine;

  @AfterEach
  void tearDown() throws Exception {
    if (scheduler != null) {
      scheduler.close();
    }
    if (db != null) {
      db.close();
    }
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
    logStreamWriter = mock(EventLogWriter.class);
    logStream = mock(EventLog.class);
    final EventLogReader reader = mock(EventLogReader.class);
    final BatchEntryReader batchReader = mock(BatchEntryReader.class);
    when(logStream.newReader()).thenReturn(reader);
    when(logStream.newBatchReader()).thenReturn(batchReader);
    when(logStream.newWriter()).thenReturn(logStreamWriter);
    when(logStream.getLastCommittedPosition()).thenReturn(0L);
    when(logStream.getLastWrittenPosition()).thenReturn(0L);
    when(logStream.getFlowControl()).thenReturn(mock(LogFlowControl.class));
    final RaftPartitionSource partitionSource = mock(RaftPartitionSource.class);
    when(partitionSource.getPartitionId()).thenReturn(PartitionId.from("business-raft", 1));
    // BeanFactory 仅部署解析使用，本测试不处理真实部署记录（null 与 dmn-engine 测试先例一致）
    final BusinessRepositoryFactory repositoryFactory =
        new RocksdbBusinessRepositoryFactory(db, partitionSource, null, new SimpleMeterRegistry());
    repository = repositoryFactory.create();

    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("freeze-recovery-test")
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
    // 启动引擎 actor：走完 starting→started 相位（日志读写器初始化、恢复链、冻结标记恢复）
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

  @Test
  void frozenMarkerIsRecoveredAcrossEngineRestart() throws Exception {
    // 手术窗口内崩溃：标记已持久化（drain=true），内存冻结态随进程丢失
    openEngine();
    repository.frozenStateRepository().markFrozen(true);

    // 引擎启动恢复链：凭标记重入冻结窗口（含排干），writer 重新冻结
    assertTrue(await(engine::isFrozen, 10_000), "重启后应凭持久标记重入冻结窗口");
    verify(logStreamWriter, timeout(10_000)).freeze();

    // 恢复：先清标记再开闸
    engine.resumeFromFreeze().get(10, TimeUnit.SECONDS);
    assertNull(repository.frozenStateRepository().frozenMarker());
    assertFalse(engine.isFrozen());
    verify(logStreamWriter, timeout(10_000)).unfreeze();
  }

  @Test
  void freezeWritesMarkerAndResumeClearsIt() throws Exception {
    openEngine();
    // 等启动链真正完成（恢复链跑完、监听注册）：生产上冻结命令只能到达已启动的引擎，
    // 提前冻结会与恢复链交错出生产不可能出现的顺序
    verify(logStream, timeout(10_000)).registerRecordAvailableListener(engine);

    final var frozen = engine.freezeAndDrain(false);
    frozen.get(10, TimeUnit.SECONDS);
    assertFalse(repository.frozenStateRepository().frozenMarker(), "冻结应落 drain=false 标记");
    // 幂等：冻结窗口内重复调用（看门狗重发）返回同一在途 future
    assertSame(frozen, engine.freezeAndDrain(true));

    engine.resumeFromFreeze().get(10, TimeUnit.SECONDS);
    assertNull(repository.frozenStateRepository().frozenMarker());
    assertFalse(engine.isFrozen());
  }
}
