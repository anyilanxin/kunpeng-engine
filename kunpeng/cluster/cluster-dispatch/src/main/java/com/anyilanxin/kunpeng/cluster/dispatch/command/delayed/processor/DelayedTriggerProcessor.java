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
package com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.AbstractDelayedProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class DelayedTriggerProcessor extends AbstractDelayedProcessor {
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  protected final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;
  private final ImmutableRepositoryBusiness repositoryBusiness;

  public DelayedTriggerProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
    repositoryBusiness = repository.repositoryBusiness();
  }

  @Override
  public void processRecord(final LogRecord<DelayedRecord> record) {
    final DelayedRecord value = record.getValue();
    final long requestId = record.getRequestId();
    switch (value.getDelayedType()) {
      case ADMIN_PLAN -> processAdminPlan(requestId, value);
      case ADMIN_EXECUTION -> processAdminPlanExecution(requestId, value);
      case BUSINESS_PLAN -> processBusinessPlan(requestId, value);
      case BUSINESS_EXECUTION -> processBusinessPlanExecution(requestId, value);
    }
    writer.addEvent(value.getDelayedId(), DelayedLifeCycle.TRIGGERED, record.getRequestId(), value);
  }

  @Override
  public DelayedLifeCycle valueLifeCycle() {
    return DelayedLifeCycle.TRIGGER;
  }

  /** 管理计划级触发：成员不足延迟到期后重新进入计划制定 */
  private void processAdminPlan(final long requestId, final DelayedRecord record) {
    final AdminDispatchPlanRecord dispatchPlan = repositoryAdmin.getDispatchPlan();
    // 管理面计划为单槽存储，槽内已被新计划占据说明延迟任务已过期
    if (dispatchPlan == null || dispatchPlan.getDispatchPlanId() != record.getDispatchPlanId()) {
      LOGGER.warn("延迟任务触发时管理面调度计划不存在或已过期，跳过。plan id:{}", record.getDispatchPlanId());
      return;
    }
    writer.addCommand(
        dispatchPlan.getDispatchPlanId(),
        AdminDispatchPlanLifeCycle.PLAN_CREATING,
        requestId,
        dispatchPlan);
  }

  /** 管理执行明细级触发：已到达终态说明 ack 已回流，仅淘汰过期看门狗；否则重新下发 EXECUTING 重试 */
  private void processAdminPlanExecution(final long requestId, final DelayedRecord record) {
    final AdminDispatchPlanExecutionRecord execution =
        repositoryAdmin.getDispatchPlanExecution(record.getDispatchPlanExecutionId());
    if (execution == null) {
      LOGGER.warn(
          "延迟任务触发时管理面执行明细不存在，跳过。plan id:{}，execution id:{}",
          record.getDispatchPlanId(),
          record.getDispatchPlanExecutionId());
      return;
    }
    if (execution.getDispatchExecutionState() == DispatchExecutionState.SUCCEED
        || execution.getDispatchExecutionState() == DispatchExecutionState.FAILED) {
      LOGGER.info(
          "管理面执行明细已到达终态，跳过过期看门狗。plan id:{}，execution id:{}，state:{}",
          record.getDispatchPlanId(),
          record.getDispatchPlanExecutionId(),
          execution.getDispatchExecutionState());
      return;
    }
    LOGGER.info(
        "延迟任务触发，重新下发管理面执行。plan id:{}，execution id:{}，execution type:{}",
        record.getDispatchPlanId(),
        record.getDispatchPlanExecutionId(),
        execution.getExecutionType());
    writer.addCommand(
        execution.getDispatchPlanExecutionId(),
        AdminDispatchPlanExecutionLifeCycle.EXECUTING,
        requestId,
        execution);
  }

  private void processBusinessPlan(final long requestId, final DelayedRecord record) {
    final long dispatchPlanId = record.getDispatchPlanId();
    final BusinessDispatchPlanRecord dispatchPlan =
        repositoryBusiness.getDispatchPlan(dispatchPlanId);
    writer.addCommand(
        dispatchPlan.getDispatchPlanId(),
        BusinessDispatchPlanLifeCycle.PLAN_CREATING,
        requestId,
        dispatchPlan);
  }

  /** 执行明细级触发：已到达终态说明 ack 已回流，仅淘汰过期看门狗；否则重新下发 EXECUTING 重试 */
  private void processBusinessPlanExecution(final long requestId, final DelayedRecord record) {
    final BusinessDispatchPlanExecutionRecord execution =
        repositoryBusiness.getDispatchPlanExecution(
            record.getDispatchPlanId(), record.getDispatchPlanExecutionId());
    if (execution == null) {
      LOGGER.warn(
          "延迟任务触发时执行明细不存在，跳过。plan id:{}，execution id:{}",
          record.getDispatchPlanId(),
          record.getDispatchPlanExecutionId());
      return;
    }
    if (execution.getDispatchExecutionState() == DispatchExecutionState.SUCCEED
        || execution.getDispatchExecutionState() == DispatchExecutionState.FAILED) {
      LOGGER.info(
          "执行明细已到达终态，跳过过期看门狗。plan id:{}，execution id:{}，state:{}",
          record.getDispatchPlanId(),
          record.getDispatchPlanExecutionId(),
          execution.getDispatchExecutionState());
      return;
    }
    LOGGER.info(
        "延迟任务触发，重新下发执行。plan id:{}，execution id:{}，execution type:{}",
        record.getDispatchPlanId(),
        record.getDispatchPlanExecutionId(),
        execution.getExecutionType());
    writer.addCommand(
        execution.getDispatchPlanExecutionId(),
        BusinessDispatchPlanExecutionLifeCycle.EXECUTING,
        requestId,
        execution);
  }
}
