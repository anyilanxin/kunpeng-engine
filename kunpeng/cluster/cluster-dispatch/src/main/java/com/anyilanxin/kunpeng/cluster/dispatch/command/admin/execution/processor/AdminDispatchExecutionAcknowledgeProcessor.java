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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.execution.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.execution.AbstractAdminDispatchExecutionProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import org.slf4j.Logger;

/**
 * 管理面执行明细 ack 回流：失败不终止计划而是注册延迟重试，成功推进下一条明细或完成计划。
 *
 * @author zxuanhong
 * @since
 */
public class AdminDispatchExecutionAcknowledgeProcessor
    extends AbstractAdminDispatchExecutionProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public AdminDispatchExecutionAcknowledgeProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchPlanExecutionRecord> record) {
    final AdminDispatchPlanExecutionRecord value = record.getValue();
    // 幂等：明细已到达终态说明是重复 ack（成员重启重放调度后会再次 ack），直接跳过
    final AdminDispatchPlanExecutionRecord stored =
        repositoryAdmin.getDispatchPlanExecution(value.getDispatchPlanExecutionId());
    if (stored == null) {
      LOGGER.warn("收到未知管理面执行明细 ack，跳过。execution id:{}", value.getDispatchPlanExecutionId());
      return;
    }
    if (stored.getDispatchExecutionState() == DispatchExecutionState.SUCCEED
        || stored.getDispatchExecutionState() == DispatchExecutionState.FAILED) {
      LOGGER.info(
          "管理面执行明细已到达终态，跳过重复 ack。plan id:{}，execution id:{}，state:{}",
          value.getDispatchPlanId(),
          value.getDispatchPlanExecutionId(),
          stored.getDispatchExecutionState());
      return;
    }
    value.setDispatchEndTime(writer.millis());
    if (value.getDispatchExecutionState() == DispatchExecutionState.FAILED) {
      // 成员执行失败不终止计划：注册延迟重试任务，到期由 DelayedTriggerProcessor 重新下发
      LOGGER.info("\n\n----->收到管理面失败 ack，安排延迟重试<-----\n{}\n", value);
      scheduleExecutionRetry(record.getRequestId(), value);
      return;
    }
    LOGGER.info("\n\n----->收到管理面 ack<-----\n{}\n", value);
    writer.addEvent(
        value.getDispatchPlanExecutionId(),
        AdminDispatchPlanExecutionLifeCycle.SUCCEED,
        record.getRequestId(),
        value);
    final AdminDispatchPlanExecutionRecord nextDispatchPlanExecution =
        repositoryAdmin.getNextDispatchPlanExecution();
    if (nextDispatchPlanExecution == null) {
      LOGGER.info("\n\n------->管理面调度执行完成<-------\n");
      final AdminDispatchPlanRecord dispatchPlan = repositoryAdmin.getDispatchPlan();
      writer.addCommand(
          dispatchPlan.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.COMPLETING,
          record.getRequestId(),
          dispatchPlan);
    } else {
      writer.addCommand(
          nextDispatchPlanExecution.getDispatchPlanExecutionId(),
          AdminDispatchPlanExecutionLifeCycle.EXECUTING,
          record.getRequestId(),
          nextDispatchPlanExecution);
    }
  }

  @Override
  public AdminDispatchPlanExecutionLifeCycle valueLifeCycle() {
    return AdminDispatchPlanExecutionLifeCycle.ACKNOWLEDGE;
  }
}
