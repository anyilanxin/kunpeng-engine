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

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.business.AbstractBusinessApiProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.BusinessDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessChangeCancelApiProcessor
    extends AbstractBusinessApiProcessor<BusinessChangeCancelRequestRecord> {
  private final LogEventWriter writer;
  private final ImmutableRepositoryBusiness repositoryBusiness;
  private final BusinessDispatchPlanRecord planRecord;

  public BusinessChangeCancelApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
    planRecord = new BusinessDispatchPlanRecord();
  }

  @Override
  public void processRecord(final LogRecord<BusinessChangeCancelRequestRecord> record) {
    final BusinessClusterMetaRecord clusterMeta = repositoryBusiness.getClusterMeta();
    if (clusterMeta == null) {
      writer.adErrorResponse(record.getRequestId(), -1, "集群未初始化，无法调度");
      return;
    }
    planRecord.reset();
    writer.addCommand(
        -1, BusinessDispatchPlanLifeCycle.CANCELING, record.getRequestId(), planRecord);
  }

  @Override
  public BusinessDispatchApiValueLifeCycle valueLifeCycle() {
    return BusinessDispatchApiValueLifeCycle.CHANGE_CANCEL_REQUEST;
  }
}
