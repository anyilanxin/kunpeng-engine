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
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.AbstractAdminDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.planner.AdminDispatchPlanMaker;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 管理面调度计划制定：期望副本数超出当前成员池时注册计划级延迟任务等待成员加入， 否则立即制定计划并按需进入执行。
 *
 * @author zxuanhong
 * @since 2026.9.0
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
  public void processRecord(final AdminLogRecord<AdminDispatchPlanRecord> record) {
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

      value.setDispatchState(DispatchPlanState.PLAN_DELAYED);
      writer.addEvent(
          value.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.PLAN_DELAYED,
          record.getRequestId(),
          value);
      delayChecker.schedule(delayedRecord.getDueDate());
      writer.adErrorResponse(record.getRequestId(), -1, "集群成员不足，管理面调度进入延迟等待，成员满足后自动生成计划");
      return;
    }
    // 满足条件，进行计划制定
    final AdminDispatchPlanRecord plan = planMaker.createPlan(value, memberIds);
    plan.setDispatchState(DispatchPlanState.PLAN_SUCCEED);
    writer.addEvent(
        plan.getDispatchPlanId(),
        AdminDispatchPlanLifeCycle.PLAN_CREATED,
        record.getRequestId(),
        plan);
    // 计划生成即响应：请求方拿到生成的计划与集群元数据，不再挂到客户端超时
    final AdminChangeResponseRecord response = new AdminChangeResponseRecord();
    copyInto(plan, response.getDispatchPlan());
    final AdminClusterMetaRecord adminClusterMeta =
        writer.getRepository().adminRepository().getClusterMeta();
    if (adminClusterMeta != null) {
      copyInto(adminClusterMeta, response.getClusterMeta());
    }
    writer.adResponse(
        AdminDispatchApiValueLifeCycle.CHANGE_REPLICATION_RESPONSE,
        record.getRequestId(),
        response);
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
