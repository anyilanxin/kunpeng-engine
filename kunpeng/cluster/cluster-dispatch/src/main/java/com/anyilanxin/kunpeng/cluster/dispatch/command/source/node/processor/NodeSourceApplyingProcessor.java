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
package com.anyilanxin.kunpeng.cluster.dispatch.command.source.node.processor;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.node.AbstractNodeSourceProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;

/**
 * @author zxuanhong
 * @since
 */
public class NodeSourceApplyingProcessor extends AbstractNodeSourceProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositorySource repositorySource;
  private final ClusterDispatchClient dispatchClient;

  public NodeSourceApplyingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    dispatchClient = writer.getDispatchClient();
    final AdminImmutableRepository repository = writer.getRepository();
    repositorySource = repository.repositorySource();
  }

  @Override
  public void processRecord(final LogRecord<NodeSourceRecord> record) {
    final NodeSourceRecord value = record.getValue();
    final NodeSourceRecord nodeSource = repositorySource.getNodeSource(value.getMemberId());
    if (nodeSource == null) {
      final NodeSourceMetaRecord nodeSourceMeta = repositorySource.getNodeSourceMeta();
      final int maxNodeSourceId = nodeSourceMeta.getMaxNodeSourceId();
      value.setSourceId(maxNodeSourceId + 1);
      nodeSourceMeta.setMaxNodeSourceId(maxNodeSourceId + 1);
      writer.addEvent(-1, NodeSourceMetaLifeCycle.UPDATED, record.getRequestId(), nodeSourceMeta);
      writer.addEvent(-1, NodeSourceLifeCycle.APPLIED, record.getRequestId(), value);
      dispatchClient.send(MemberId.from(value.getMemberId()), value.getSourceId());
    }
  }

  @Override
  public NodeSourceLifeCycle valueLifeCycle() {
    return NodeSourceLifeCycle.APPLYING;
  }
}
