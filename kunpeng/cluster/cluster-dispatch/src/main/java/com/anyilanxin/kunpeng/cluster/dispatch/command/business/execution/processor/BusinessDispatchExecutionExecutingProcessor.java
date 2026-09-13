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

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution.AbstractBusinessDispatchExecutionProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessDispatchExecutionExecutingProcessor
    extends AbstractBusinessDispatchExecutionProcessor {
  protected final LogEventWriter writer;
  private final ClusterDispatchClient dispatchClient;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public BusinessDispatchExecutionExecutingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    dispatchClient = writer.getDispatchClient();
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanExecutionRecord> record) {
    final BusinessDispatchPlanExecutionRecord value = record.getValue();
    final long requestId = record.getRequestId();
    value.setDispatchStartTime(writer.millis());
    writer.addEvent(
        value.getDispatchPlanExecutionId(),
        BusinessDispatchPlanExecutionLifeCycle.EXECUTED,
        record.getRequestId(),
        value);
    LOGGER.info(
        "\n\n------->当前执行信息。order:{},execution id:{}，execution type:{}<-------\n",
        value.getExecutionOrder(),
        value.getDispatchPlanExecutionId(),
        value.getExecutionType());
    // 发出调度消息的同时注册 ack 看门狗：到期无响应由延迟任务重新下发
    scheduleExecutionRetry(requestId, value);
    dispatchClient.send(value, value.getPlanData());
  }

  @Override
  public BusinessDispatchPlanExecutionLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanExecutionLifeCycle.EXECUTING;
  }
}
