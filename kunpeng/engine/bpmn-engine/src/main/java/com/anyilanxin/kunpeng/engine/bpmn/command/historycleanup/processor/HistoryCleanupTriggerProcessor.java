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
package com.anyilanxin.kunpeng.engine.bpmn.command.historycleanup.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.historycleanup.HistoryCleanupAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 历史清理触发处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class HistoryCleanupTriggerProcessor extends HistoryCleanupAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(HistoryCleanupTriggerProcessor.class);

  public HistoryCleanupTriggerProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void processRecord(final BusinessLogRecord<HistoryCleanupRecord> record) {
    LOG.debug("Triggering history cleanup {}", record.getKey());
    final HistoryCleanupRecord value = record.getValue();
    writer.addEvent(
        record.getKey(), HistoryCleanupLifeCycle.TRIGGERED, record.getRequestId(), value);
  }

  @Override
  public HistoryCleanupLifeCycle valueLifeCycle() {
    return HistoryCleanupLifeCycle.TRIGGER;
  }
}
