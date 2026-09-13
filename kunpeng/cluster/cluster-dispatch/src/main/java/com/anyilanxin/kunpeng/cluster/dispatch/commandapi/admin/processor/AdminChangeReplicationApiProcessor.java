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
package com.anyilanxin.kunpeng.cluster.dispatch.commandapi.admin.processor;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.admin.AbstractAdminApiProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeReplicationRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import java.util.Set;

/**
 * @author zxuanhong
 * @since
 */
public class AdminChangeReplicationApiProcessor
    extends AbstractAdminApiProcessor<AdminChangeReplicationRequestRecord> {
  private final LogEventWriter writer;
  private final ClusterMembershipService membershipService;
  private final ImmutableRepositoryAdmin repositoryAdmin;
  private final AdminDispatchPlanRecord planRecord;

  public AdminChangeReplicationApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    membershipService = writer.getMembershipService();
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
    planRecord = new AdminDispatchPlanRecord();
  }

  @Override
  public void processRecord(final LogRecord<AdminChangeReplicationRequestRecord> record) {
    final AdminChangeReplicationRequestRecord value = record.getValue();
    if (value.getExpectReplicationFactor() <= 0) {
      writer.adErrorResponse(record.getRequestId(), -1, "期望副本数量必须大于0");
      return;
    }
    final AdminClusterMetaRecord clusterMeta = repositoryAdmin.getClusterMeta();
    if (clusterMeta == null) {
      writer.adErrorResponse(record.getRequestId(), -1, "集群未初始化，无法调度");
      return;
    }
    final int expectReplicationFactor = value.getExpectReplicationFactor();
    final Set<Member> members = membershipService.getMembers(ZoneType.BROKER.getType());
    if (members.size() < expectReplicationFactor) {
      writer.adErrorResponse(
          record.getRequestId(),
          -1,
          "集群节点数量不足，实际:" + members.size() + ",需要最低节点数量:" + expectReplicationFactor);
      return;
    }
    planRecord.reset();
    planRecord
        .setExpectReplicationFactor(expectReplicationFactor)
        .setDispatchPlanType(AdminDispatchType.CHANGE_REPLICATION)
        .setApplyPlan(value.isApplyPlan());
    writer.addCommand(
        -1, AdminDispatchPlanLifeCycle.CHANGE_REPLICATION, record.getRequestId(), planRecord);
  }

  @Override
  public AdminDispatchApiValueLifeCycle valueLifeCycle() {
    return AdminDispatchApiValueLifeCycle.CHANGE_REPLICATION_REQUEST;
  }
}
