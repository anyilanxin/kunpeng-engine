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
package com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DistributeSerialBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.AbstractDistributeSerialProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.ImmutableDistributeSerialRepository;
import java.util.List;
import org.agrona.collections.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 串行分发启动命令处理器：向索引列族剩余的首个存活目标投递，ACK 摘除后由 Ack 处理器续发下一个（一次一个）。 目标已随缩容摘除的按已送达摘除后继续尝试下一个；剩余摘空则完成。首次投递与
 * checker 超时重投走同一逻辑， 天然幂等。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialDistributeStartProcessor extends AbstractDistributeSerialProcessor {
  private static final Logger LOG =
      LoggerFactory.getLogger(DistributeSerialDistributeStartProcessor.class);
  final LogEventWriter writer;
  private final DistributeSerialBehavior distributeSerialBehavior;
  private final ImmutableDistributeSerialRepository distribute;

  public DistributeSerialDistributeStartProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    distributeSerialBehavior = behavior.distributeSerialBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    distribute = repository.distributeSerialRepository();
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeSerialRecord> record) {
    LOG.debug(
        "Distributing data to other partitions for distribute {}",
        record.getValue().getDistributeId());
    final DistributeSerialRecord value = record.getValue();
    final List<Integer> remainIndex = distribute.getDistributeRemainIndex(value.getDistributeId());
    final IntHashSet activityPartitionIds = writer.getActivityPartitionIds();
    for (final Integer partitionId : remainIndex) {
      value.setCurrentDistributeSourceId(partitionId);
      if (!activityPartitionIds.contains(partitionId)) {
        // 死目标（分区已随缩容摘除）无处送达：按已送达摘除。接盘其 resource 的幸存分区本就在
        // 目标集内，数据不缺
        writer.addEvent(
            value.getDistributeId(),
            DistributeSerialLifeCycle.DISTRIBUTE_ACKED,
            record.getRequestId(),
            value);
        continue;
      }
      distributeSerialBehavior.distributeSerial(value, partitionId);
      writer.addEvent(
          value.getDistributeId(),
          DistributeSerialLifeCycle.DISTRIBUTE_CONTINUED,
          record.getRequestId(),
          value);
      // 串行：一次只投一个，等 ACK 摘除后续发下一个
      return;
    }
    // 剩余目标全为死目标（或已摘空）：完成分发
    if (!distribute.haveDistributeAck(value.getDistributeId())) {
      writer.addCommand(
          value.getDistributeId(),
          DistributeSerialLifeCycle.DISTRIBUTE_COMPLETE,
          record.getRequestId(),
          value);
    }
  }

  @Override
  public DistributeSerialLifeCycle valueLifeCycle() {
    return DistributeSerialLifeCycle.DISTRIBUTE_START;
  }
}
