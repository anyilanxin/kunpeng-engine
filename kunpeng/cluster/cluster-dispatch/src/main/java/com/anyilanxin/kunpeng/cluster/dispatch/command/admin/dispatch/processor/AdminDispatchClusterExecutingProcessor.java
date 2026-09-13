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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.AbstractAdminDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class AdminDispatchClusterExecutingProcessor extends AbstractAdminDispatchProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public AdminDispatchClusterExecutingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchPlanRecord> record) {
    final AdminDispatchPlanRecord planRecord = record.getValue();
    final AdminDispatchPlanRecord dispatchPlan = repositoryAdmin.getDispatchPlan();
    if (dispatchPlan == null) {
      throw new RuntimeException("管理面调度计划不存在");
    }
    if (dispatchPlan.executionPlan().isEmpty()) {
      LOGGER.info("\n\n------->管理面调度计划执行内容为空，直接完成<-------\n");
      writer.addEvent(
          dispatchPlan.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.COMPLETED,
          record.getRequestId(),
          dispatchPlan);
    } else {
      LOGGER.info("\n\n------->管理面调度计划生成成功<-------\n{}\n", dispatchPlan);
      writer.addEvent(
          dispatchPlan.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.EXECUTED,
          record.getRequestId(),
          dispatchPlan);
      final AdminDispatchPlanExecutionRecord planExecutionRecord =
          dispatchPlan.executionPlan().get(0);
      writer.addCommand(
          planExecutionRecord.getDispatchPlanExecutionId(),
          AdminDispatchPlanExecutionLifeCycle.EXECUTING,
          record.getRequestId(),
          planExecutionRecord);
    }
  }

  @Override
  public AdminDispatchPlanLifeCycle valueLifeCycle() {
    return AdminDispatchPlanLifeCycle.EXECUTING;
  }
}
