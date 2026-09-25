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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DistributeParallelBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.parallel.AbstractDistributeParallelProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 并行分发启动后命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelDistributeAfterStartProcessor
    extends AbstractDistributeParallelProcessor {
  final LogEventWriter writer;
  private final DistributeParallelBehavior distributeParallelBehavior;

  public DistributeParallelDistributeAfterStartProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    distributeParallelBehavior = behavior.distributeParallelBehavior();
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeParallelRecord> record) {
    final DistributeParallelRecord value = record.getValue();
    final List<Integer> activitySourceIds = new ArrayList<>(writer.getActivitySourceIds());
    Collections.shuffle(activitySourceIds);
    value.setDistributeAfterIndex(activitySourceIds.getFirst());
    value.setLifeCycle(DistributeParallelLifeCycle.DISTRIBUTE_AFTER_STARTED);
    writer.addEvent(
        value.getDistributeId(),
        DistributeParallelLifeCycle.DISTRIBUTE_AFTER_STARTED,
        record.getRequestId(),
        value);
    distributeParallelBehavior.distributeParallelAfter(value, value.getDistributeAfterIndex());
  }

  @Override
  public DistributeParallelLifeCycle valueLifeCycle() {
    return DistributeParallelLifeCycle.DISTRIBUTE_AFTER_START;
  }
}
