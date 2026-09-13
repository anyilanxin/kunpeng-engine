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

import com.anyilanxin.kunpeng.cluster.dispatch.command.CommandProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.ApiCommandProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zxuanhong
 * @since
 */
public class ClusterDispatchEngine {

  private final LogEventProcessors processors;

  private final ProcessingCollectSupplier collectSupplier;
  private final LogEventWriter writer;
  private final List<SchedulerCheckerAware> schedulerCheckerAwares = new ArrayList<>();

  public ClusterDispatchEngine(
      final LogEventProcessors processors,
      final AdminRepository repository,
      final PartitionSourceMetadata partitionSourceMetadata,
      final MeterRegistry meterRegistry,
      final LogEventWriter logEventWriter,
      final ProcessingCollectSupplier collectSupplier) {
    this.processors = processors;
    this.collectSupplier = collectSupplier;
    writer = logEventWriter;
    init();
  }

  private void init() {
    ApiCommandProcessorRegister.registerRepository(processors, writer);
    CommandProcessorRegister.registerRepository(processors, writer);
  }

  List<SchedulerCheckerAware> lifecycleAwares() {
    return schedulerCheckerAwares;
  }

  public void processEvent(final LogRecord record, final BatchProcessingCollect processingCollect) {
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
