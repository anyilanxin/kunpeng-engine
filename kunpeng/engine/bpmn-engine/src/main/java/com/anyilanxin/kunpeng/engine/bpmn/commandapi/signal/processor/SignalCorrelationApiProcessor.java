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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.signal.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.signal.SignalCorrelationApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.signal.correlation.SignalCorrelationRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.signal.correlation.SignalCorrelationResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.signal.CommandApiSignalValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;

/**
 * 信号关联 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SignalCorrelationApiProcessor
    extends SignalCorrelationApiAbstractProcessor<SignalCorrelationRequestRecord> {
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final int sourceId;
  private final SignalDistributeCorrelateRecord correlateRecord;
  private final SignalCorrelationResponseRecord response = new SignalCorrelationResponseRecord();

  public SignalCorrelationApiProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    correlateRecord = new SignalDistributeCorrelateRecord();
  }

  @Override
  public void processRecord(final BusinessLogRecord<SignalCorrelationRequestRecord> record) {
    final SignalCorrelationRequestRecord value = record.getValue();
    correlateRecord.reset();

    correlateRecord
        .setSignalName(value.getSignalNameBuffer())
        .setDistributeSignalSubscriptionId(writer.nextCurrentSourceKey())
        .setVariables(value.getVariablesBuffer())
        .setTenantId(value.getTenantIdBuffer());

    writer.addCommand(
        correlateRecord.getDistributeSignalSubscriptionId(),
        SignalDistributeCorrelateLifeCycle.CREATE,
        record.getRequestId(),
        correlateRecord);

    writer.adResponse(
        CommandApiSignalValueLifeCycle.CORRELATION_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiSignalValueLifeCycle valueLifeCycle() {
    return CommandApiSignalValueLifeCycle.CORRELATION_REQUEST;
  }
}
