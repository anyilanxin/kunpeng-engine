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
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution.AbstractBusinessDispatchExecutionProcessor;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import org.slf4j.Logger;

/**
 * 业务面执行明细 ACK 超时命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessDispatchExecutionAcknowledgeTimeOutProcessor
    extends AbstractBusinessDispatchExecutionProcessor {
  protected final LogEventWriter writer;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public BusinessDispatchExecutionAcknowledgeTimeOutProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecord(final AdminLogRecord<BusinessDispatchPlanExecutionRecord> record) {}

  @Override
  public BusinessDispatchPlanExecutionLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanExecutionLifeCycle.ACKNOWLEDGE_TIMED_OUT;
  }
}
