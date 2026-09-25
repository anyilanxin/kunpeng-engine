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
package com.anyilanxin.kunpeng.engine.bpmn.command.distribute.parallel.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.parallel.AbstractDistributeParallelProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 并行分发完成命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelCompleteProcessor extends AbstractDistributeParallelProcessor {
  private static final Logger LOG =
      LoggerFactory.getLogger(DistributeParallelCompleteProcessor.class);
  final LogEventWriter writer;

  public DistributeParallelCompleteProcessor(final LogEventWriter writer) {
    this.writer = writer;
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeParallelRecord> record) {
    LOG.debug("distributeCompleteProcessor {}", record.getValue().getDistributeId());
    final DistributeParallelRecord value = record.getValue();
    writer.addEvent(
        value.getDistributeId(),
        DistributeParallelLifeCycle.DISTRIBUTE_COMPLETED,
        record.getRequestId(),
        value);
  }

  @Override
  public DistributeParallelLifeCycle valueLifeCycle() {
    return DistributeParallelLifeCycle.DISTRIBUTE_COMPLETE;
  }
}
