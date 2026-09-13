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

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.AbstractBusinessDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessClusterMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import java.util.List;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessDispatchCompleteProcessor extends AbstractBusinessDispatchProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryBusiness repositoryBusiness;

  public BusinessDispatchCompleteProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    final BusinessClusterMetaRecord clusterMeta;
    if (value.isInitialize()) {
      clusterMeta = new BusinessClusterMetaRecord();
    } else {
      clusterMeta = repositoryBusiness.getClusterMeta();
    }
    clusterMeta.setReplicationFactor(value.getExpectReplicationFactor());
    clusterMeta.setCurrentPartitionCount(value.getExpectPartitionsCount());
    final List<PartitionInfoMetaRecord> lastMeta = clusterMeta.getMetaRecord();
    clusterMeta.lastMeta().reset();
    for (final PartitionInfoMetaRecord metaRecord : lastMeta) {
      clusterMeta.lastMeta().add().fromMetadata(metaRecord);
    }
    clusterMeta.meta().reset();
    final ArrayProperty<PartitionInfoMetaRecord> meta = value.meta();
    for (final PartitionInfoMetaRecord metaRecord : meta) {
      clusterMeta.meta().add().fromMetadata(metaRecord);
    }
    clusterMeta.setUpdateTime(writer.millis());
    clusterMeta.setVersion(clusterMeta.getVersion() + 1);
    final BusinessClusterMetaLifeCycle metaLifeCycle;
    if (value.isInitialize()) {
      metaLifeCycle = BusinessClusterMetaLifeCycle.CREATING;
    } else {
      metaLifeCycle = BusinessClusterMetaLifeCycle.UPDATING;
    }
    writer.addCommand(-1, metaLifeCycle, record.getRequestId(), clusterMeta);
    writer.addEvent(
        record.getKey(), BusinessDispatchPlanLifeCycle.COMPLETED, record.getRequestId(), value);
  }

  @Override
  public BusinessDispatchPlanLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanLifeCycle.COMPLETING;
  }
}
