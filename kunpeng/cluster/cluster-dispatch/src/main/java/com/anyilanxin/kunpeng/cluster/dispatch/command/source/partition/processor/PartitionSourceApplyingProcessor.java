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
package com.anyilanxin.kunpeng.cluster.dispatch.command.source.partition.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.partition.AbstractPartitionSourceProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;

/**
 * @author zxuanhong
 * @since
 */
public class PartitionSourceApplyingProcessor extends AbstractPartitionSourceProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositorySource repositorySource;
  private final ClusterDispatchClient dispatchClient;

  public PartitionSourceApplyingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    dispatchClient = writer.getDispatchClient();
    final AdminImmutableRepository repository = writer.getRepository();
    repositorySource = repository.repositorySource();
  }

  @Override
  public void processRecord(final LogRecord<PartitionSourceRecord> record) {
    final PartitionSourceRecord value = record.getValue();
    final PartitionSourceRecord partitionSource =
        repositorySource.getPartitionSource(value.getPartitionGroup(), value.getPartitionId());
    if (partitionSource == null) {
      final PartitionSourceMetaRecord partitionSourceMeta =
          repositorySource.getPartitionSourceMeta();
      final int maxPartitionSourceId = partitionSourceMeta.getMaxPartitionSourceId();
      value.setSourceId(maxPartitionSourceId + 1);
      partitionSourceMeta.setMaxPartitionSourceId(maxPartitionSourceId + 1);
      writer.addEvent(
          -1, PartitionSourceMetaLifeCycle.UPDATED, record.getRequestId(), partitionSourceMeta);
      writer.addEvent(-1, PartitionSourceLifeCycle.APPLIED, record.getRequestId(), value);
    }
  }

  @Override
  public PartitionSourceLifeCycle valueLifeCycle() {
    return PartitionSourceLifeCycle.APPLYING;
  }
}
