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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.business.AbstractBusinessApiProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.query.BusinessDispatchQueryResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.BusinessDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessQueryApiProcessor
    extends AbstractBusinessApiProcessor<BusinessChangeResponseRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(BusinessQueryApiProcessor.class);
  private final LogEventWriter writer;
  private final BusinessDispatchQueryResponseRecord response;
  private final ImmutableRepositoryBusiness repositoryBusiness;

  public BusinessQueryApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    response = new BusinessDispatchQueryResponseRecord();
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
  }

  @Override
  public void processRecord(final LogRecord<BusinessChangeResponseRecord> record) {
    response.reset();
    final BusinessClusterMetaRecord clusterMeta = repositoryBusiness.getClusterMeta();
    if (clusterMeta != null) {
      copyInto(clusterMeta, response.getClusterMeta());
    }
    final BusinessDispatchPlanRecord dispatchPlan = repositoryBusiness.getDispatchPlan(-1);
    if (dispatchPlan != null) {
      copyInto(dispatchPlan, response.getDispatchPlan());
    }
    writer.adResponse(
        BusinessDispatchApiValueLifeCycle.DISPATCH_QUERY_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public BusinessDispatchApiValueLifeCycle valueLifeCycle() {
    return BusinessDispatchApiValueLifeCycle.DISPATCH_QUERY_REQUEST;
  }
}
