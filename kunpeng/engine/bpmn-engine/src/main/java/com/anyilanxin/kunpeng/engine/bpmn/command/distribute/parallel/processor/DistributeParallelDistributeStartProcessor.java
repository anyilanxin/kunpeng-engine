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
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.ImmutableDistributeParallelRepository;
import java.util.List;
import org.agrona.collections.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 并行分发启动命令处理器：对索引列族剩余的全部目标分区同时扇出（不等 ACK），目标已随缩容摘除的按已送达摘除。 首次投递与 checker 超时重投走同一逻辑，天然幂等。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelDistributeStartProcessor
    extends AbstractDistributeParallelProcessor {
  private static final Logger LOG =
      LoggerFactory.getLogger(DistributeParallelDistributeStartProcessor.class);
  private final LogEventWriter writer;
  private final DistributeParallelBehavior distributeParallelBehavior;
  private final ImmutableDistributeParallelRepository distribute;

  public DistributeParallelDistributeStartProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    distributeParallelBehavior = behavior.distributeParallelBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    distribute = repository.distributeParallelRepository();
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeParallelRecord> record) {
    LOG.debug(
        "Distributing data to other partitions for distribute {}",
        record.getValue().getDistributeId());
    final DistributeParallelRecord value = record.getValue();
    final List<Integer> remainIndex = distribute.getDistributeRemainIndex(value.getDistributeId());
    final IntHashSet activityPartitionIds = writer.getActivityPartitionIds();
    for (final Integer partitionId : remainIndex) {
      value.setCurrentDistributeIndex(partitionId);
      if (activityPartitionIds.contains(partitionId)) {
        distributeParallelBehavior.distributeParallel(value, partitionId);
        writer.addEvent(
            value.getDistributeId(),
            DistributeParallelLifeCycle.DISTRIBUTE_CONTINUED,
            record.getRequestId(),
            value);
      } else {
        // 死目标（分区已随缩容摘除）无处送达：按已送达摘除。接盘其 resource 的幸存分区本就在
        // 目标集内，数据不缺
        writer.addEvent(
            value.getDistributeId(),
            DistributeParallelLifeCycle.DISTRIBUTE_ACKED,
            record.getRequestId(),
            value);
      }
    }
    if (!distribute.haveDistributeAck(value.getDistributeId())) {
      completeOrFollowUp(record.getRequestId(), value);
    }
  }

  private void completeOrFollowUp(final long requestId, final DistributeParallelRecord value) {
    if (value.isHaveFollowUp()) {
      writer.addCommand(
          value.getDistributeId(),
          DistributeParallelLifeCycle.DISTRIBUTE_AFTER_START,
          requestId,
          value);
    } else {
      writer.addCommand(
          value.getDistributeId(),
          DistributeParallelLifeCycle.DISTRIBUTE_COMPLETE,
          requestId,
          value);
    }
  }

  @Override
  public DistributeParallelLifeCycle valueLifeCycle() {
    return DistributeParallelLifeCycle.DISTRIBUTE_START;
  }
}
