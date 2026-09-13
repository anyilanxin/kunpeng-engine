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
package com.anyilanxin.kunpeng.cluster.dispatch;

import com.anyilanxin.kunpeng.broker.client.admin.BrokerResponseWriter;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.UnwrittenRecord;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryAppliers;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import java.util.*;

/**
 * @author zxuanhong
 * @since
 */
public class BatchProcessingCollect implements CloseableSilently {
  private final Deque<LogRecord<?>> toProcess = new ArrayDeque<>();
  private final List<AppendEntry> toWrite = new ArrayList<>();
  private final CommandApiHandle commandApiHandle;
  private Optional<BrokerResponseWriter<UnifiedRecordValue>> responseWriter = Optional.empty();
  private final AdminRepositoryAppliers applier;
  private final int partitionId;
  private int processSize = 0;
  private int stateSize = 0;
  private final RecordValueMapper valueMapper = DefaultRecordValueMapper.getInstance();

  private final List<SideEffectProducer> sideEffectProducers = new ArrayList<>();

  public BatchProcessingCollect(
      final CommandApiHandle commandApiHandle,
      final AdminRepositoryAppliers applier,
      final int partitionId) {
    this.commandApiHandle = commandApiHandle;
    this.applier = applier;
    this.partitionId = partitionId;
  }

  public void reset() {
    toProcess.clear();
    toWrite.clear();
    processSize = 0;
    stateSize = 0;
    responseWriter = Optional.empty();
    sideEffectProducers.clear();
  }

  public BatchProcessingCollect addInitCommand(final LogRecord<?> logRecord) {
    reset();
    processSize++;
    toProcess.addLast(logRecord);
    return this;
  }

  public BatchProcessingCollect addCommand(
      final long key, final UnifiedRecordValue recordValue, final AdminRecordMetadata metadata) {
    final UnwrittenRecord logRecord = new UnwrittenRecord(key, partitionId, recordValue, metadata);
    toProcess.addLast(logRecord);
    processSize++;
    return this;
  }

  public BatchProcessingCollect addCommandWrite(
      final UnifiedRecordValue recordValue, final AdminRecordMetadata metadata) {
    final AppendEntry logAppendEntry = RecordAppendEntryFactory.of(metadata, recordValue);
    toWrite.add(logAppendEntry);
    processSize++;
    return this;
  }

  public BatchProcessingCollect addState(
      final long key, final UnifiedRecordValue recordValue, final AdminRecordMetadata metadata) {
    applier.applyState(key, metadata.getValueType(), metadata.getLifeCycle(), recordValue);
    stateSize++;
    return this;
  }

  public BatchProcessingCollect addEvent(
      final long key, final UnifiedRecordValue recordValue, final AdminRecordMetadata metadata) {
    final AppendEntry logAppendEntry = RecordAppendEntryFactory.of(key, metadata, recordValue);
    toWrite.add(logAppendEntry);
    processSize++;
    return this;
  }

  public BatchProcessingCollect adResponse(
      final CommandApiValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    final BrokerResponseWriter<UnifiedRecordValue> responseWriter =
        commandApiHandle.newResponse(lifeCycle, requestId);
    responseWriter.setResponse(valueMapper.copyValue(lifeCycle, recordValue));
    this.responseWriter = Optional.of(responseWriter);
    return this;
  }

  public BatchProcessingCollect adErrorResponse(
      final long requestId, final int code, final String message) {
    final BrokerResponseWriter<UnifiedRecordValue> responseWriter =
        commandApiHandle.newResponse(null, requestId);
    responseWriter.setError(code, message);
    this.responseWriter = Optional.of(responseWriter);
    return this;
  }

  public BatchProcessingCollect addSideEffect(final SideEffectProducer producer) {
    sideEffectProducers.add(producer);
    return this;
  }

  public void sendResponse() {
    responseWriter.ifPresent(commandApiHandle::sendResponse);
  }

  public boolean hasNext() {
    return !toProcess.isEmpty();
  }

  public LogRecord<?> next() {
    final LogRecord<?> logRecord = toProcess.removeFirst();
    final AdminRecordMetadata copyMetadata = logRecord.getMetadata().copy();
    final UnifiedRecordValue value = logRecord.getValue();

    final UnifiedRecordValue copyRecordValue =
        valueMapper.copyValue(copyMetadata.getLifeCycle(), value);

    final AppendEntry logAppendEntry =
        RecordAppendEntryFactory.of(logRecord.getKey(), copyMetadata, copyRecordValue);
    toWrite.add(AppendEntry.skipped(logAppendEntry));
    return logRecord;
  }

  public List<AppendEntry> waitWrite() {
    if (!toProcess.isEmpty()) {
      for (final LogRecord<?> logRecord : toProcess) {
        final AppendEntry logAppendEntry =
            RecordAppendEntryFactory.of(
                logRecord.getKey(), logRecord.getMetadata(), logRecord.getValue());
        toWrite.add(logAppendEntry);
      }
    }
    return toWrite;
  }

  public List<LogRecord<?>> waitProcess() {
    return new ArrayList<>(toProcess);
  }

  public List<SideEffectProducer> sideEffect() {
    return new ArrayList<>(sideEffectProducers);
  }

  public int processSize() {
    return processSize;
  }

  public int stateSize() {
    return stateSize;
  }

  @Override
  public void close() {
    reset();
  }
}
