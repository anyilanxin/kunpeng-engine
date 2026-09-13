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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution.processor;

import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decode;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution.AbstractBusinessDispatchExecutionProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionLeaveSourceTransferRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessDispatchExecutionAcknowledgeProcessor
    extends AbstractBusinessDispatchExecutionProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryBusiness repositoryBusiness;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private final PartitionLeaveSourceTransferRecord transferRecord;

  public BusinessDispatchExecutionAcknowledgeProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
    transferRecord = new PartitionLeaveSourceTransferRecord();
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanExecutionRecord> record) {
    final BusinessDispatchPlanExecutionRecord value = record.getValue();
    // 幂等：明细已到达终态说明是重复 ack（成员重启重放调度后会再次 ack），直接跳过
    final BusinessDispatchPlanExecutionRecord stored =
        repositoryBusiness.getDispatchPlanExecution(
            value.getDispatchPlanId(), value.getDispatchPlanExecutionId());
    if (stored == null) {
      LOGGER.warn(
          "收到未知执行明细 ack，跳过。plan id:{}，execution id:{}",
          value.getDispatchPlanId(),
          value.getDispatchPlanExecutionId());
      return;
    }
    if (stored.getDispatchExecutionState() == DispatchExecutionState.SUCCEED
        || stored.getDispatchExecutionState() == DispatchExecutionState.FAILED) {
      LOGGER.info(
          "执行明细已到达终态，跳过重复 ack。plan id:{}，execution id:{}，state:{}",
          value.getDispatchPlanId(),
          value.getDispatchPlanExecutionId(),
          stored.getDispatchExecutionState());
      return;
    }
    value.setDispatchEndTime(writer.millis());
    if (value.getDispatchExecutionState() == DispatchExecutionState.FAILED) {
      // 成员执行失败不终止计划：注册延迟重试任务，到期由 DelayedTriggerProcessor 重新下发
      LOGGER.info("\n\n----->收到失败 ack，安排延迟重试<-----\n{}\n", value);
      scheduleExecutionRetry(record.getRequestId(), value);
      return;
    }
    LOGGER.info("\n\n----->收到 ack<-----\n{}\n", value);
    writer.addEvent(
        value.getDispatchPlanExecutionId(),
        BusinessDispatchPlanExecutionLifeCycle.SUCCEED,
        record.getRequestId(),
        value);
    if (value.getExecutionType() == PartitionExecutionType.LEAVE_SOURCE_TRANSFER) {
      // 修改元数据
      final byte[] planData = stored.getPlanData();
      transferRecord.reset();
      decode(planData, transferRecord);
      LOGGER.info("\n\n----->执行元数据迁移<-----\n{}\n", transferRecord);
      final PartitionSourceRecord sourceRecord = new PartitionSourceRecord();
      sourceRecord.setPartitionId(transferRecord.getPartitionId());
      sourceRecord.setPartitionGroup(transferRecord.getPartitionGroup());
      sourceRecord.setSourcePartitionGroup(transferRecord.getSourcePartitionGroup());
      sourceRecord.setSourcePartitionId(transferRecord.getSourcePartitionId());
      for (final IntegerValue agentSourceId : transferRecord.agentSourceIds()) {
        sourceRecord.agentSourceIds().add().setValue(agentSourceId.getValue());
      }
      writer.addCommand(
          -1, PartitionSourceLifeCycle.TRANSFERRING_ADD, record.getRequestId(), sourceRecord);
    }
    final BusinessDispatchPlanExecutionRecord nextDispatchPlanExecution =
        repositoryBusiness.getNextDispatchPlanExecution(value.getDispatchPlanId());
    if (nextDispatchPlanExecution == null) {
      LOGGER.info("\n\n------->调度执行完成<-------\n");
      final BusinessDispatchPlanRecord dispatchPlan =
          repositoryBusiness.getDispatchPlan(value.getDispatchPlanId());
      writer.addCommand(
          dispatchPlan.getDispatchPlanId(),
          BusinessDispatchPlanLifeCycle.COMPLETING,
          record.getRequestId(),
          dispatchPlan);
    } else {
      writer.addCommand(
          nextDispatchPlanExecution.getDispatchPlanExecutionId(),
          BusinessDispatchPlanExecutionLifeCycle.EXECUTING,
          record.getRequestId(),
          nextDispatchPlanExecution);
    }
  }

  @Override
  public BusinessDispatchPlanExecutionLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanExecutionLifeCycle.ACKNOWLEDGE;
  }
}
