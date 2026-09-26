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
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.engine.bpmn.command.CommandProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.ApiCommandProcessorRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;

/**
 * BPMN 执行引擎：流程实例调度与命令处理的核心入口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnEngine {

  private final LogEventProcessors processors;
  private final RaftPartitionSource partitionSource;
  private final ProcessingCollectSupplier collectSupplier;
  private final LogEventWriter writer;
  private final BeanFactory beanFactory;
  private final BusinessRepository repository;
  private final InterPartitionCommandSender commandSender;
  private final MeterRegistry meterRegistry;
  private final List<SchedulerCheckerAware> schedulerCheckerAwares = new ArrayList<>();

  public BpmnEngine(
      final LogEventProcessors processors,
      final BusinessRepository repository,
      final RaftPartitionSource partitionSource,
      final BeanFactory beanFactory,
      final InterPartitionCommandSender commandSender,
      final MeterRegistry meterRegistry,
      final LogEventWriter logEventWriter,
      final ProcessingCollectSupplier collectSupplier) {
    this.meterRegistry = meterRegistry;
    this.processors = processors;
    this.repository = repository;
    this.partitionSource = partitionSource;
    this.commandSender = commandSender;
    this.beanFactory = beanFactory;
    writer = logEventWriter;
    this.collectSupplier = collectSupplier;
    init();
  }

  private void init() {
    ApiCommandProcessorRegister.registerRepository(processors, writer);
    CommandProcessorRegister.registerRepository(processors, writer);
  }

  List<SchedulerCheckerAware> lifecycleAwares() {
    return schedulerCheckerAwares;
  }

  public void processEvent(
      final BusinessLogRecord record, final BatchProcessingCollect processingCollect) {
    final LogEventProcessor<?> processor = processors.getProcessor(record);
    collectSupplier.setCollect(processingCollect);
    if (processor == null) {
      writer.adErrorResponse(
          record.getRequestId(),
          -1,
          "processor is null,recordType="
              + record.getRecordType()
              + ",valueType="
              + record.getValueType()
              + ",valueState="
              + record.getValueState());
      return;
    }
    processor.processRecord(record);
  }
}
