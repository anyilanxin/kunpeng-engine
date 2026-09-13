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

import static com.anyilanxin.kunpeng.protocol.admin.AdminConstant.DISPATCH_PLAN_TIME;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.AbstractAdminDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.planner.AdminDispatchPlanMaker;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 管理面调度计划制定：期望副本数超出当前成员池时注册计划级延迟任务等待成员加入， 否则立即制定计划并按需进入执行。
 *
 * @author zxuanhong
 * @since
 */
public class AdminDispatchClusterCreatingProcessor extends AbstractAdminDispatchProcessor {
  protected final LogEventWriter writer;
  private final DelayedRecord delayedRecord = new DelayedRecord();
  private final ClusterMembershipService membershipService;
  private final DelayedDelayChecker delayChecker;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private final AdminDispatchPlanMaker planMaker;

  public AdminDispatchClusterCreatingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    membershipService = writer.getMembershipService();
    delayChecker = writer.getDelayChecker();
    planMaker = new AdminDispatchPlanMaker(writer);
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchPlanRecord> record) {
    final AdminDispatchPlanRecord value = record.getValue();
    final Set<MemberId> memberIds = membershipService.getMemberIds(ZoneType.BROKER.getType());
    if (value.getExpectReplicationFactor() > memberIds.size()) {
      LOGGER.info(
          "\n----当前管理端需要集群成员不足，需要进行延迟调度-----期望成员数:{},实际成员数：{}\n",
          value.getExpectReplicationFactor(),
          memberIds.size());
      delayedRecord.reset();
      delayedRecord
          .setDelayedId(writer.nextKey())
          .setDelayedType(DelayedType.ADMIN_PLAN)
          .setDispatchPlanId(value.getDispatchPlanId())
          .setDueDate(writer.millis() + DISPATCH_PLAN_TIME)
          .setPartitionType(PartitionType.ADMIN);

      writer.addEvent(
          delayedRecord.getDelayedId(),
          DelayedLifeCycle.CREATED,
          record.getRequestId(),
          delayedRecord);

      writer.addEvent(
          value.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.PLAN_DELAYED,
          record.getRequestId(),
          value);
      delayChecker.schedule(delayedRecord.getDueDate());
      return;
    }
    // 满足条件，进行计划制定
    final AdminDispatchPlanRecord plan = planMaker.createPlan(value, memberIds);
    writer.addEvent(
        plan.getDispatchPlanId(),
        AdminDispatchPlanLifeCycle.PLAN_CREATED,
        record.getRequestId(),
        plan);
    if (value.isApplyPlan()) {
      writer.addCommand(
          plan.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.EXECUTING,
          record.getRequestId(),
          plan);
    }
  }

  @Override
  public AdminDispatchPlanLifeCycle valueLifeCycle() {
    return AdminDispatchPlanLifeCycle.PLAN_CREATING;
  }
}
