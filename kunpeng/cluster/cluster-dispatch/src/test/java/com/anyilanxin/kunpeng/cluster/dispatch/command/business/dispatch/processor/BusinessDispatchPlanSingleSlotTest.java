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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.configuration.broker.BusinessRaftCfg;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableBusinessRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 业务面计划单槽闸门测试（业务面与管理面分开算）：执行中计划拒绝新调度；未应用计划（延迟等待/已生成未执行）
 * 取消覆盖由新计划接管；终态计划不构成障碍。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BusinessDispatchPlanSingleSlotTest {
  private static final long REQUEST_ID = 300L;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repositoryBusiness;
  private BusinessDispatchChangePartitionProcessor processor;
  private AdminLogRecord<BusinessDispatchPlanRecord> record;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final AdminImmutableRepository repository = mock(AdminImmutableRepository.class);
    repositoryBusiness = mock(ImmutableBusinessRepository.class);
    when(writer.getRepository()).thenReturn(repository);
    when(repository.businessRepository()).thenReturn(repositoryBusiness);
    when(repository.keyRepository()).thenReturn(mock());
    when(repository.sourceRepository()).thenReturn(mock());
    when(writer.getClusterTopologyService()).thenReturn(mock());
    when(writer.getBusinessRaft()).thenReturn(mock(BusinessRaftCfg.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
    when(writer.getMembershipService()).thenReturn(mock());
    when(writer.getDelayChecker()).thenReturn(mock());
    when(writer.nextKey()).thenReturn(1L);
    final BusinessClusterMetaRecord clusterMeta = new BusinessClusterMetaRecord();
    clusterMeta.setReplicationFactor(1);
    clusterMeta.setCurrentPartitionCount(1);
    when(repositoryBusiness.getClusterMeta()).thenReturn(clusterMeta);
    processor = new BusinessDispatchChangePartitionProcessor(writer);
    record = mock(AdminLogRecord.class);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(new BusinessDispatchPlanRecord());
  }

  private BusinessDispatchPlanRecord plan(final long id, final DispatchPlanState state) {
    final BusinessDispatchPlanRecord plan = new BusinessDispatchPlanRecord();
    plan.setDispatchPlanId(id);
    plan.setDispatchState(state);
    return plan;
  }

  @Test
  void executingPlanRejectsNewDispatch() {
    when(repositoryBusiness.getDispatchPlans())
        .thenReturn(List.of(plan(10L, DispatchPlanState.EXECUTED)));

    processor.processRecord(record);

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(-1), any(String.class));
    verify(writer, never())
        .addCommand(anyLong(), any(BusinessDispatchPlanLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void unappliedCreatedPlanIsReplacedByNewDispatch() {
    when(repositoryBusiness.getDispatchPlans())
        .thenReturn(List.of(plan(10L, DispatchPlanState.PLAN_SUCCEED)));

    processor.processRecord(record);

    verify(writer, never()).adErrorResponse(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(String.class));
    verify(writer)
        .addCommand(eq(10L), eq(BusinessDispatchPlanLifeCycle.CANCELING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(anyLong(), eq(BusinessDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }

  @Test
  void unappliedDelayedPlanIsReplacedByNewDispatch() {
    when(repositoryBusiness.getDispatchPlans())
        .thenReturn(List.of(plan(10L, DispatchPlanState.PLAN_DELAYED)));

    processor.processRecord(record);

    verify(writer)
        .addCommand(eq(10L), eq(BusinessDispatchPlanLifeCycle.CANCELING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(anyLong(), eq(BusinessDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }

  @Test
  void terminalPlansDoNotBlockNewDispatch() {
    when(repositoryBusiness.getDispatchPlans())
        .thenReturn(
            List.of(
                plan(10L, DispatchPlanState.SUCCEED),
                plan(11L, DispatchPlanState.FAILED),
                plan(12L, DispatchPlanState.CANCELED)));

    processor.processRecord(record);

    verify(writer, never()).adErrorResponse(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(String.class));
    verify(writer, never())
        .addCommand(anyLong(), eq(BusinessDispatchPlanLifeCycle.CANCELING), anyLong(), any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(anyLong(), eq(BusinessDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }

  @Test
  void emptySlotCreatesPlanDirectly() {
    when(repositoryBusiness.getDispatchPlans()).thenReturn(List.of());

    processor.processRecord(record);

    verify(writer, never()).adErrorResponse(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(String.class));
    verify(writer, times(1))
        .addCommand(anyLong(), eq(BusinessDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }
}
