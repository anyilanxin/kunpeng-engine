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
package com.anyilanxin.kunpeng.repository.business;

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.eventlog.RecordAvailableListener;
import com.anyilanxin.kunpeng.kvstore.RepositoryTransaction;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.modules.key.MutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.MutableProcessedPositionRepository;
import com.anyilanxin.kunpeng.scheduler.Actor;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志事件重放状态机
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessRepositoryProcessService extends Actor implements RecordAvailableListener {
  private static final Logger LOG = LoggerFactory.getLogger(BusinessRepositoryProcessService.class);
  private final EventLog logStream;
  private BatchEntryReader logStreamReader;
  private boolean processing = false;
  private long processPosition = -1;
  private MutableProcessedPositionRepository repositoryPosition;
  private MutableKeyGeneratorRepository repositoryKey;
  private final RecordMetadata metadata = new RecordMetadata();
  private RepositoryTransaction currentTransaction;
  private final RaftPartitionSource partitionSource;
  private final PartitionId partitionId;
  private final BusinessRepositoryAppliers applier;
  private final BusinessRepository repository;
  private final RecordValueMapper valueMapper;
  private final TransactionContext context;
  private final MeterRegistry meterRegistry;

  public BusinessRepositoryProcessService(
      final EventLog logStream,
      final BusinessRepository repository,
      final RaftPartitionSource partitionSource,
      final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    applier = repository.getAppliers();
    valueMapper = DefaultRecordValueMapper.getInstance();
    this.logStream = logStream;
    this.repository = repository;
    context = repository.getContext();
    this.partitionSource = partitionSource;
    partitionId = partitionSource.getPartitionId();
  }

  @Override
  protected void onActorStarting() {
    logStreamReader = logStream.newBatchReader();
    initRepositoryPosition();
    logStreamReader.seekToNextBatch(processPosition);
  }

  private void initRepositoryPosition() {
    repositoryPosition = repository.processedPositionRepository();
    repositoryKey = repository.keyGeneratorRepository();
    processPosition = repositoryPosition.getLastSuccessfulProcessedRecordPosition();
  }

  @Override
  protected void onActorStarted() {
    logStream.registerRecordAvailableListener(this);
    actor.submit(this::processNextReplayEvent);
  }

  @Override
  public void close() {
    logStream.removeRecordAvailableListener(this);
    if (currentTransaction != null) {
      try {
        currentTransaction.rollback();
        currentTransaction = null;
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onRecordAvailable() {
    actor.submit(this::processNextReplayEvent);
  }

  void processNextReplayEvent() {
    if (processing || !logStreamReader.hasNext()) {
      return;
    }
    try {
      processing = true;
      while (logStreamReader.hasNext()) {
        final BatchEntryReader.Batch batch = logStreamReader.next();
        processReplayEvent(batch);
      }
    } finally {
      processing = false;
      actor.submit(this::processNextReplayEvent);
    }
  }

  void processReplayEvent(final BatchEntryReader.Batch batch) {
    try {
      currentTransaction = context.getCurrentTransaction();
      currentTransaction.run(
          () -> {
            while (batch.hasNext()) {
              final LoggedEntry loggedEvent = batch.next();
              metadata.reset();
              loggedEvent.readMetadata(metadata);
              final ValueType valueType = metadata.getValueType();
              final ValueLifeCycle lifeCycle = metadata.getLifeCycle();
              final RecordType recordType = metadata.getRecordType();
              final long key = loggedEvent.getKey();
              if (recordType == RecordType.EVENT) {
                final UnifiedRecordValue recordValue = valueMapper.getCacheValue(lifeCycle);
                recordValue.reset();
                loggedEvent.readValue(recordValue);
                applier.applyState(key, valueType, lifeCycle, recordValue);
              }
              processPosition = loggedEvent.getPosition();
              repositoryKey.setKeyIfHigher(key);
            }
            repositoryPosition.markAsProcessed(processPosition);
          });
      currentTransaction.commit();
      currentTransaction = null;
    } catch (final Exception e) {
      LOG.error("Failed to process log records", e);
    }
  }
}
