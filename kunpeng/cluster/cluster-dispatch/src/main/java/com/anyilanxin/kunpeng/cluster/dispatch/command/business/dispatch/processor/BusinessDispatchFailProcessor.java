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
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import org.slf4j.Logger;

/**
 * 业务面调度计划失败命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessDispatchFailProcessor extends AbstractBusinessDispatchProcessor {
  protected final LogEventWriter writer;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public BusinessDispatchFailProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecord(final AdminLogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    LOGGER.info("\n\n------->调度计划失败，补偿数据手术后停止推进<-------\n{}\n", value);
    // 失败即解冻：补发 RESUME/re-JOIN 恢复已冻结分区与已摘除 follower（03 号文档 §9）；
    // 未执行明细保持 WAIT，等待后续人工重试（重试重新冻结即可）
    final BusinessDispatchPlanRecord dispatchPlan =
        writer.getRepository().businessRepository().getDispatchPlan(value.getDispatchPlanId());
    if (dispatchPlan != null) {
      SurgeryCompensator.compensate(writer, dispatchPlan);
    }
    value.setDispatchState(DispatchPlanState.FAILED);
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
