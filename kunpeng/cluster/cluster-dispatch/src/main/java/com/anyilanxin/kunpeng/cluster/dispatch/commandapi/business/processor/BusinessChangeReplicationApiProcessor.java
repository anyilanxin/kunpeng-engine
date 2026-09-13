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
package com.anyilanxin.kunpeng.cluster.dispatch.commandapi.business.processor;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.business.AbstractBusinessApiProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeReplicationRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.BusinessDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import java.util.Set;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessChangeReplicationApiProcessor
    extends AbstractBusinessApiProcessor<BusinessChangeReplicationRequestRecord> {
  private final LogEventWriter writer;
  private final ClusterMembershipService membershipService;
  private final ImmutableRepositoryBusiness repositoryBusiness;
  private final BusinessDispatchPlanRecord planRecord;

  public BusinessChangeReplicationApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    membershipService = writer.getMembershipService();
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
    planRecord = new BusinessDispatchPlanRecord();
  }

  @Override
  public void processRecord(final LogRecord<BusinessChangeReplicationRequestRecord> record) {
    final BusinessChangeReplicationRequestRecord value = record.getValue();
    if (value.getExpectReplicationFactor() <= 0) {
      writer.adErrorResponse(record.getRequestId(), -1, "期望副本数量必须大于0");
      return;
    }
    final BusinessClusterMetaRecord clusterMeta = repositoryBusiness.getClusterMeta();
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
        .setDispatchPlanType(BusinessDispatchType.CHANGE_REPLICATION)
        .setApplyPlan(value.isApplyPlan());
    writer.addCommand(
        -1, BusinessDispatchPlanLifeCycle.CHANGE_REPLICATION, record.getRequestId(), planRecord);
  }

  @Override
  public BusinessDispatchApiValueLifeCycle valueLifeCycle() {
    return BusinessDispatchApiValueLifeCycle.CHANGE_REPLICATION_REQUEST;
  }
}
