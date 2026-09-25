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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.message.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.message.MessageCorrelationApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.correlation.MessageCorrelationRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.message.CommandApiMessageValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;

/**
 * 消息关联 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MessageCorrelationApiProcessor
    extends MessageCorrelationApiAbstractProcessor<MessageCorrelationRequestRecord> {
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final int sourceId;
  private final MessageDistributeCorrelateRecord correlateRecord;

  public MessageCorrelationApiProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    correlateRecord = new MessageDistributeCorrelateRecord();
  }

  @Override
  public void processRecord(final BusinessLogRecord<MessageCorrelationRequestRecord> record) {
    final MessageCorrelationRequestRecord value = record.getValue();
    correlateRecord.reset();

    correlateRecord
        .setCorrelationKey(value.getCorrelationKeyBuffer())
        .setDistributeMessageSubscriptionId(writer.nextCurrentSourceKey())
        .setMessageName(value.getMessageNameBuffer())
        .setVariables(value.getVariablesBuffer())
        .setTenantId(value.getTenantIdBuffer());

    writer.addCommand(
        correlateRecord.getDistributeMessageSubscriptionId(),
        MessageDistributeCorrelateLifeCycle.CREATE,
        record.getRequestId(),
        correlateRecord);

    // 添加异步请求
    final AsyncRequestRecord requestRecord =
        new AsyncRequestRecord()
            .setKey(correlateRecord.getDistributeMessageSubscriptionId())
            .setValueType(ValueType.MESSAGE_DISTRIBUTE_CORRELATE)
            .setValueLifeCycle(MessageDistributeCorrelateLifeCycle.CREATE)
            .setRequestId(record.getRequestId());

    writer.addEvent(
        requestRecord.getKey(),
        AsyncRequestLifeCycle.CREATED,
        record.getRequestId(),
        requestRecord);
  }

  @Override
  public CommandApiMessageValueLifeCycle valueLifeCycle() {
    return CommandApiMessageValueLifeCycle.CORRELATION_REQUEST;
  }
}
