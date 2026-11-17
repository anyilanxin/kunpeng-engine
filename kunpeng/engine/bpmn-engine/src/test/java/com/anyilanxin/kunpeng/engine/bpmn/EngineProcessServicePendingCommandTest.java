/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseWriter;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.PendingCommandRegistry;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerClock;
import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.LogFlowControl;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.RocksdbBusinessRepositoryFactory;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.InstantSource;
import java.util.Map;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 调度命令经过主引擎处理后必须释放在途登记，允许后续合法调度并避免永久残留。 */
class EngineProcessServicePendingCommandTest {
  @TempDir Path tempDir;

  private KvStore<BusinessRepositoryColumnFamilies> db;
  private BusinessRepository repository;

  @AfterEach
  void tearDown() throws Exception {
    if (db != null) {
      db.close();
    }
  }

  @Test
  void shouldReleaseTimerAndJobCommandsAfterProcessing() throws Exception {
    final long timerId = 101;
    final long jobId = 202;
    final var timerEntry =
        loggedEntry(
            1,
            timerId,
            TimerLifeCycle.TRIGGER,
            new TimerEventRecord().setProcessDefinitionKey("test"));
    final var jobEntry = loggedEntry(2, jobId, JobLifeCycle.TIME_OUT, emptyJob());
    final var reader = mock(EventLogReader.class);
    when(reader.hasNext()).thenReturn(true, true, false, true, true, false);
    when(reader.next()).thenReturn(timerEntry, jobEntry);

    final var engine = newEngine(reader);
    engine.onActorStarting();
    final var registry = pendingRegistry(engine);
    registry.add(TimerLifeCycle.TRIGGER, timerId);
    registry.add(JobLifeCycle.TIME_OUT, jobId);
    assertTrue(registry.contains(TimerLifeCycle.TRIGGER, timerId));
    assertTrue(registry.contains(JobLifeCycle.TIME_OUT, jobId));

    // timer 不存在、job 未激活，两个处理器均走合法跳过路径；处理结束仍须释放登记。
    engine.processNextEvent();
    engine.processNextEvent();

    assertFalse(registry.contains(TimerLifeCycle.TRIGGER, timerId));
    assertFalse(registry.contains(JobLifeCycle.TIME_OUT, jobId));
  }

  @Test
  void shouldReleaseTimerAfterProcessingRollback() throws Exception {
    final long timerId = 303;
    final var timer =
        new TimerEventRecord()
            .setTimerId(timerId)
            .setDueDate(1_000)
            .setProcessDefinitionKey("missing")
            .setProcessDefinitionId(999_999)
            .setTimerElementType(TimerElementType.PROCESS_START_EVENT);
    final var reader = mock(EventLogReader.class);
    final var entry = loggedEntry(1, timerId, TimerLifeCycle.TRIGGER, timer);
    when(reader.hasNext()).thenReturn(true, true, false);
    when(reader.next()).thenReturn(entry);
    final var engine = newEngine(reader);
    engine.onActorStarting();
    final var transaction = repository.getContext().getCurrentTransaction();
    transaction.run(() -> repository.timerEventRepository().save(timerId, timer));
    transaction.commit();
    final var registry = pendingRegistry(engine);
    registry.add(TimerLifeCycle.TRIGGER, timerId);

    // timer 存在但定义缺失，会在 TRIGGER 处理中异常并回滚；回滚后记录仍在、登记必须释放。
    engine.processNextEvent();

    assertFalse(registry.contains(TimerLifeCycle.TRIGGER, timerId));
    assertNotNull(repository.timerEventRepository().query(timerId));
  }

  private EngineProcessService newEngine(final EventLogReader reader) {
    db =
        new DefaultRocksdbFactory<BusinessRepositoryColumnFamilies>()
            .createDb(
                tempDir.resolve("live").toString(),
                new SimpleMeterRegistry(),
                0,
                new RocksdbConfiguration(),
                BusinessRepositoryColumnFamilies.DEFAULT);
    final var logStream = mock(EventLog.class);
    when(logStream.newReader()).thenReturn(reader);
    when(logStream.newBatchReader()).thenReturn(mock(BatchEntryReader.class));
    when(logStream.newWriter()).thenReturn(mock(EventLogWriter.class));
    when(logStream.getFlowControl()).thenReturn(mock(LogFlowControl.class));
    final var partitionSource = mock(RaftPartitionSource.class);
    when(partitionSource.getPartitionId()).thenReturn(PartitionId.from("business-raft", 1));
    final var repositoryFactory =
        new RocksdbBusinessRepositoryFactory(db, partitionSource, null, new SimpleMeterRegistry());
    final var commandApiHandle = mock(CommandApiHandle.class);
    doReturn(mock(BrokerResponseWriter.class))
        .when(commandApiHandle)
        .newResponse(any(), anyLong(), anyInt());
    final var engine =
        new EngineProcessService(
            mock(JobDeliveryPort.class),
            null,
            logStream,
            repositoryFactory,
            commandApiHandle,
            partitionSource,
            mock(InterPartitionCommandSender.class),
            new SimpleMeterRegistry(),
            TimerClock.passthrough(InstantSource.system()),
            mock(ActorSchedulingService.class));
    repository = repositoryFactory.create();
    return engine;
  }

  private static PendingCommandRegistry.Stageable pendingRegistry(final EngineProcessService engine)
      throws ReflectiveOperationException {
    final var field = EngineProcessService.class.getDeclaredField("pendingCommandRegistry");
    field.setAccessible(true);
    return (PendingCommandRegistry.Stageable) field.get(engine);
  }

  private static JobRecord emptyJob() {
    return new JobRecord()
        .setJobType("test")
        .setJobKind(JobKindType.ACTIVITY)
        .setProcessDefinitionKey("test")
        .setProcessDefinitionId(1)
        .setProcessInstanceId(1)
        .setState(JobState.ACTIVE)
        .setVariables(Map.of())
        .setLocalVariables(Map.of());
  }

  private static LoggedEntry loggedEntry(
      final long position,
      final long key,
      final ValueLifeCycle lifeCycle,
      final UnifiedRecordValue value) {
    final var metadata = new RecordMetadata();
    metadata
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .valueLifeCycle(lifeCycle);
    final var metadataBuffer = bufferOf(metadata);
    final var valueBuffer = bufferOf(value);
    final var entry = mock(LoggedEntry.class);
    when(entry.getPosition()).thenReturn(position);
    when(entry.getKey()).thenReturn(key);
    when(entry.isSkipProcessing()).thenReturn(false);
    when(entry.getMetadata()).thenReturn(metadataBuffer);
    when(entry.getMetadataOffset()).thenReturn(0);
    when(entry.getMetadataLength()).thenReturn(metadataBuffer.capacity());
    when(entry.getValue()).thenReturn(valueBuffer);
    when(entry.getValueOffset()).thenReturn(0);
    when(entry.getValueLength()).thenReturn(valueBuffer.capacity());
    doCallRealMethod().when(entry).readMetadata(any());
    doCallRealMethod().when(entry).readValue(any());
    return entry;
  }

  private static UnsafeBuffer bufferOf(final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter value) {
    final var buffer = new UnsafeBuffer(new byte[value.getLength()]);
    value.write(buffer, 0);
    return buffer;
  }
}
