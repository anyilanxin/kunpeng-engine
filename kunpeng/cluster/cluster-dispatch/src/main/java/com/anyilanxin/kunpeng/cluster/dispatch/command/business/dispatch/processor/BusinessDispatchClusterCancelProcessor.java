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

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.AbstractBusinessDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.BusinessDispatchPlanMaker;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.configuration.broker.BusinessRaftCfg;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessDispatchClusterCancelProcessor extends AbstractBusinessDispatchProcessor {
  protected final LogEventWriter writer;
  private final BusinessRaftCfg businessRaft;
  private final ClusterMembershipService membershipService;
  private final ImmutableRepositoryBusiness repositoryBusiness;
  private final DelayedDelayChecker delayChecker;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private final BusinessDispatchPlanMaker planGenerator;

  public BusinessDispatchClusterCancelProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
    businessRaft = writer.getBusinessRaft();
    membershipService = writer.getMembershipService();
    delayChecker = writer.getDelayChecker();
    planGenerator = new BusinessDispatchPlanMaker(writer);
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {}

  @Override
  public BusinessDispatchPlanLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanLifeCycle.CANCELING;
  }
}
