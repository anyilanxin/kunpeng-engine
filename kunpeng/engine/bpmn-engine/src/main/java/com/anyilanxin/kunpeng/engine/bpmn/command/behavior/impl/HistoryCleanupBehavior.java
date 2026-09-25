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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.ImmutableDelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;

/**
 * 历史清理行为：历史数据的过期清理语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class HistoryCleanupBehavior {
  private final LogEventWriter writer;
  private final ImmutableProcessInstanceRepository processInstance;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableBatchRepository batch;
  private final ImmutableDelayRepository delay;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final HistoryCleanupRecord cleanupRecord;
  private static final long DAY_MILLIS = 24 * 60 * 60 * 1000L;

  public HistoryCleanupBehavior(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    bpmnResource = repository.bpmnResourceRepository();
    batch = repository.batchRepository();
    delay = repository.delayRepository();
    cleanupRecord = new HistoryCleanupRecord();
  }

  public void addHistoryCleanup(final ProcessInstanceRecord record) {
    final ProcessDefinitionRecord definitionRecord =
        bpmnResource.get(record.getProcessDefinitionId());
    final int historyTtl = definitionRecord.getHistoryTimeToLive();
    if (historyTtl != -1) {
      cleanupRecord.reset();
      cleanupRecord
          .setHistoryCleanupId(writer.nextCurrentSourceKey(record.getProcessInstanceId()))
          .setProcessInstanceId(record.getProcessInstanceId())
          .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
          .setProcessDefinitionId(record.getProcessDefinitionId())
          .setDueDate(writer.millis() + historyTtl * DAY_MILLIS)
          .setStartTime(writer.millis())
          .setTenantId(record.getTenantIdBuffer());
      // 添加history ttl
      writer.addEvent(
          cleanupRecord.getHistoryCleanupId(), HistoryCleanupLifeCycle.CREATED, -1, cleanupRecord);
    }
  }
}
