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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.admin.AbstractAdminApiProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query.AdminDispatchQueryRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query.AdminDispatchQueryResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zxuanhong
 * @since
 */
public class AdminQueryApiProcessor
    extends AbstractAdminApiProcessor<AdminDispatchQueryRequestRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(AdminQueryApiProcessor.class);
  private final AdminDispatchQueryResponseRecord response;
  private final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;

  public AdminQueryApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    response = new AdminDispatchQueryResponseRecord();
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchQueryRequestRecord> record) {
    response.reset();
    final AdminClusterMetaRecord clusterMeta = repositoryAdmin.getClusterMeta();
    if (clusterMeta != null) {
      copyInto(clusterMeta, response.getClusterMeta());
    }
    final AdminDispatchPlanRecord dispatchPlan = repositoryAdmin.getDispatchPlan();
    if (dispatchPlan != null) {
      copyInto(dispatchPlan, response.getDispatchPlan());
    }
    writer.adResponse(
        AdminDispatchApiValueLifeCycle.DISPATCH_QUERY_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public AdminDispatchApiValueLifeCycle valueLifeCycle() {
    return AdminDispatchApiValueLifeCycle.DISPATCH_QUERY_REQUEST;
  }
}
