/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableBusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 延迟任务终态跳过测试：被新调度覆盖（取消）或已完结的计划，其遗留延迟任务迟至触发时不得复活计划。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class DelayedTriggerTerminalSkipTest {
  private LogEventWriter writer;
  private ImmutableBusinessRepository repositoryBusiness;
  private DelayedTriggerProcessor processor;
  private AdminLogRecord<DelayedRecord> record;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final AdminImmutableRepository repository = mock(AdminImmutableRepository.class);
    repositoryBusiness = mock(ImmutableBusinessRepository.class);
    when(writer.getRepository()).thenReturn(repository);
    when(repository.businessRepository()).thenReturn(repositoryBusiness);
    when(repository.adminRepository()).thenReturn(mock());
    processor = new DelayedTriggerProcessor(writer);
    record = mock(AdminLogRecord.class);
    when(record.getRequestId()).thenReturn(1L);
    final DelayedRecord delayed = new DelayedRecord();
    delayed
        .setDelayedId(1L)
        .setDelayedType(DelayedType.BUSINESS_PLAN)
        .setDispatchPlanId(10L)
        .setPartitionType(PartitionType.BUSINESS);
    when(record.getValue()).thenReturn(delayed);
  }

  private BusinessDispatchPlanRecord plan(final DispatchPlanState state) {
    final BusinessDispatchPlanRecord plan = new BusinessDispatchPlanRecord();
    plan.setDispatchPlanId(10L);
    plan.setDispatchState(state);
    return plan;
  }

  @Test
  void terminalPlanIsNotResurrectedByDelayedTrigger() {
    when(repositoryBusiness.getDispatchPlan(10L)).thenReturn(plan(DispatchPlanState.CANCELED));

    processor.processRecord(record);

    verify(writer, never())
        .addCommand(anyLong(), any(BusinessDispatchPlanLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
    verify(writer).addEvent(eq(1L), eq(DelayedLifeCycle.TRIGGERED), eq(1L), any());
  }

  @Test
  void unappliedPlanStillReEntersCreation() {
    when(repositoryBusiness.getDispatchPlan(10L)).thenReturn(plan(DispatchPlanState.PLAN_DELAYED));

    processor.processRecord(record);

    verify(writer)
        .addCommand(eq(10L), eq(BusinessDispatchPlanLifeCycle.PLAN_CREATING), eq(1L), any(UnifiedRecordValue.class));
  }
}
