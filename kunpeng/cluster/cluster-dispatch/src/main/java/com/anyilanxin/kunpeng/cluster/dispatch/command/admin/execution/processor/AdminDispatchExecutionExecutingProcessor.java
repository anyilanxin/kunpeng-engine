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
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.execution.AbstractAdminDispatchExecutionProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionLifeCycle;
import org.slf4j.Logger;

/**
 * 管理面执行明细下发：管理分区不产生引导与 source 治理，负载即编码后的调度动作记录， 按执行类型路由到对应成员处理端（topic 由分区类型 + 执行类型推导）。
 *
 * @author zxuanhong
 * @since
 */
public class AdminDispatchExecutionExecutingProcessor
    extends AbstractAdminDispatchExecutionProcessor {
  protected final LogEventWriter writer;
  private final ClusterDispatchClient dispatchClient;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public AdminDispatchExecutionExecutingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    dispatchClient = writer.getDispatchClient();
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchPlanExecutionRecord> record) {
    final AdminDispatchPlanExecutionRecord value = record.getValue();
    final long requestId = record.getRequestId();
    value.setDispatchStartTime(writer.millis());
    writer.addEvent(
        value.getDispatchPlanExecutionId(),
        AdminDispatchPlanExecutionLifeCycle.EXECUTED,
        record.getRequestId(),
        value);
    LOGGER.info(
        "\n\n------->管理面当前执行信息。order:{}，execution id:{}，execution type:{}<-------\n",
        value.getExecutionOrder(),
        value.getDispatchPlanExecutionId(),
        value.getExecutionType());
    // 发出调度消息的同时注册 ack 看门狗：到期无响应由延迟任务重新下发
    scheduleExecutionRetry(requestId, value);
    dispatchClient.send(value, value.getPlanData());
  }

  @Override
  public AdminDispatchPlanExecutionLifeCycle valueLifeCycle() {
    return AdminDispatchPlanExecutionLifeCycle.EXECUTING;
  }
}
