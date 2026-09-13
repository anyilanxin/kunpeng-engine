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
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.partition.AbstractPartitionSourceProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;

/**
 * @author zxuanhong
 * @since
 */
public class PartitionSourceTransferringProcessor extends AbstractPartitionSourceProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositorySource repositorySource;

  public PartitionSourceTransferringProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositorySource = repository.repositorySource();
  }

  @Override
  public void processRecord(final LogRecord<PartitionSourceRecord> record) {
    final PartitionSourceRecord value = record.getValue();
    writer.addEvent(-1, PartitionSourceLifeCycle.TRANSFERRED_ADD, record.getRequestId(), value);
  }

  @Override
  public PartitionSourceLifeCycle valueLifeCycle() {
    return PartitionSourceLifeCycle.TRANSFERRING_ADD;
  }
}
