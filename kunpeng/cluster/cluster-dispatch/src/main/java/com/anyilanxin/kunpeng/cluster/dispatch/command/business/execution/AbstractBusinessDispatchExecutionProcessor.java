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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution;

import static com.anyilanxin.kunpeng.protocol.admin.AdminConstant.DISPATCH_PLAN_TIME;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;

/**
 * @author zxuanhong
 * @since
 */
public abstract class AbstractBusinessDispatchExecutionProcessor
    implements LogEventProcessorSingleState<BusinessDispatchPlanExecutionRecord> {
  protected final LogEventWriter writer;

  /** 延迟任务记录缓冲，仅在引擎单线程内写入，可安全复用同一实例 */
  protected final DelayedRecord delayedRecord = new DelayedRecord();

  protected final DelayedDelayChecker delayChecker;

  public AbstractBusinessDispatchExecutionProcessor(final LogEventWriter writer) {
    this.writer = writer;
    delayChecker = writer.getDelayChecker();
  }

  @Override
  public AdminValueType valueType() {
    return AdminValueType.BUSINESS_DISPATCH_EXECUTION;
  }

  @Override
  public abstract BusinessDispatchPlanExecutionLifeCycle valueLifeCycle();

  /**
   * 创建执行明细级延迟任务：发送调度时注册作 ack 看门狗，收到失败 ack 时注册作重试。到期仍未收到 ack 响应由 {@code DelayedTriggerProcessor}
   * 重新下发 EXECUTING。
   */
  protected void scheduleExecutionRetry(
      final long requestId, final BusinessDispatchPlanExecutionRecord execution) {
    delayedRecord.reset();
    delayedRecord
        .setDelayedId(writer.nextKey())
        .setDelayedType(DelayedType.BUSINESS_EXECUTION)
        .setPartitionType(PartitionType.BUSINESS)
        .setDispatchPlanId(execution.getDispatchPlanId())
        .setDispatchPlanExecutionId(execution.getDispatchPlanExecutionId())
        .setDueDate(writer.millis() + DISPATCH_PLAN_TIME);
    writer.addEvent(
        delayedRecord.getDelayedId(), DelayedLifeCycle.CREATED, requestId, delayedRecord);
    delayChecker.schedule(delayedRecord.getDueDate());
  }
}
