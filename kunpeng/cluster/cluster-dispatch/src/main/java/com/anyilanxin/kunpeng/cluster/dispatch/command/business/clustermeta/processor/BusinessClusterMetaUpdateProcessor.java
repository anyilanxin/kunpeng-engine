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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.clustermeta.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.clustermeta.AbstractBusinessClusterMetaProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessClusterMetaLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessClusterMetaUpdateProcessor extends AbstractBusinessClusterMetaProcessor {
  protected final LogEventWriter writer;

  public BusinessClusterMetaUpdateProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecord(final LogRecord<BusinessClusterMetaRecord> record) {
    final BusinessClusterMetaRecord value = record.getValue();
    writer.addEvent(
        record.getKey(), BusinessClusterMetaLifeCycle.UPDATED, record.getRequestId(), value);
  }

  @Override
  public BusinessClusterMetaLifeCycle valueLifeCycle() {
    return BusinessClusterMetaLifeCycle.UPDATING;
  }
}
