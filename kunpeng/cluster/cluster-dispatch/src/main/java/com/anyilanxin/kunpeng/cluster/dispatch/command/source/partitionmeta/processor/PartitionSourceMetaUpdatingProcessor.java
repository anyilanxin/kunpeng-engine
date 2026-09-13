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
package com.anyilanxin.kunpeng.cluster.dispatch.command.source.partitionmeta.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.partitionmeta.AbstractPartitionSourceMetaProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
public class PartitionSourceMetaUpdatingProcessor extends AbstractPartitionSourceMetaProcessor {
  protected final LogEventWriter writer;

  public PartitionSourceMetaUpdatingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecord(final LogRecord<PartitionSourceMetaRecord> record) {
    final PartitionSourceMetaRecord value = record.getValue();
    writer.addEvent(-1, PartitionSourceMetaLifeCycle.UPDATED, record.getRequestId(), value);
  }

  @Override
  public PartitionSourceMetaLifeCycle valueLifeCycle() {
    return PartitionSourceMetaLifeCycle.UPDATING;
  }
}
