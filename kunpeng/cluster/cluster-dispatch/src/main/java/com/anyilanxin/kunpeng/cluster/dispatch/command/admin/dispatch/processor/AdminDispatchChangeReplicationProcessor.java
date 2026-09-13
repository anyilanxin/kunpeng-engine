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

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.AbstractAdminDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.planner.AdminDispatchPlanMaker;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.configuration.broker.ManageRaftCfg;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class AdminDispatchChangeReplicationProcessor extends AbstractAdminDispatchProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;
  private final ManageRaftCfg adminRaft;
  private final ClusterMembershipService membershipService;
  private final DelayedDelayChecker delayChecker;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private final AdminDispatchPlanMaker planMaker;

  public AdminDispatchChangeReplicationProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
    adminRaft = writer.getAdminRaft();
    membershipService = writer.getMembershipService();
    delayChecker = writer.getDelayChecker();
    planMaker = new AdminDispatchPlanMaker(writer);
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchPlanRecord> record) {
    final AdminDispatchPlanRecord value = record.getValue();
    final AdminClusterMetaRecord clusterMeta = repositoryAdmin.getClusterMeta();
    LOGGER.info("\n---->管理端初始化<-----\n{}", value);
    value.setApplyPlan(true);
    value.setDispatchPlanType(AdminDispatchType.CHANGE_REPLICATION);
    value.setDispatchPlanId(writer.nextKey());
    value.setOldMeta(clusterMeta.getMeta());
    value.setOldReplicationFactor(clusterMeta.getReplicationFactor());
    writer.addCommand(
        value.getDispatchPlanId(),
        AdminDispatchPlanLifeCycle.PLAN_CREATING,
        record.getRequestId(),
        value);
  }

  @Override
  public AdminDispatchPlanLifeCycle valueLifeCycle() {
    return AdminDispatchPlanLifeCycle.CHANGE_REPLICATION;
  }
}
