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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.AbstractBusinessDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessDispatchFailProcessor extends AbstractBusinessDispatchProcessor {
  protected final LogEventWriter writer;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public BusinessDispatchFailProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    LOGGER.info("\n\n------->调度计划失败，停止推进<-------\n{}\n", value);
    // 计划标记失败并清理（applier 复用 dispatchPlanComplete）；未执行明细保持 WAIT，等待后续人工重试
    writer.addEvent(
        value.getDispatchPlanId(),
        BusinessDispatchPlanLifeCycle.FAILED,
        record.getRequestId(),
        value);
  }

  @Override
  public BusinessDispatchPlanLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanLifeCycle.FAILING;
  }
}
