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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.configuration.broker.ManageRaftCfg;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 管理面计划单槽闸门测试（与业务面分开算）：执行中拒绝；未应用取消覆盖；终态不构成障碍；空槽直建。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class AdminDispatchPlanSingleSlotTest {
  private static final long REQUEST_ID = 300L;

  private LogEventWriter writer;
  private ImmutableAdminRepository repositoryAdmin;
  private AdminDispatchChangeReplicationProcessor processor;
  private AdminLogRecord<AdminDispatchPlanRecord> record;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final AdminImmutableRepository repository = mock(AdminImmutableRepository.class);
    repositoryAdmin = mock(ImmutableAdminRepository.class);
    when(writer.getRepository()).thenReturn(repository);
    when(repository.adminRepository()).thenReturn(repositoryAdmin);
    when(repository.keyRepository()).thenReturn(mock());
    when(repository.sourceRepository()).thenReturn(mock());
    when(writer.getClusterTopologyService()).thenReturn(mock());
    when(writer.getAdminRaft()).thenReturn(mock(ManageRaftCfg.class));
    when(writer.getMembershipService()).thenReturn(mock());
    when(writer.getDelayChecker()).thenReturn(mock());
    when(writer.nextKey()).thenReturn(1L);
    final AdminClusterMetaRecord clusterMeta = new AdminClusterMetaRecord();
    clusterMeta.setReplicationFactor(1);
    when(repositoryAdmin.getClusterMeta()).thenReturn(clusterMeta);
    processor = new AdminDispatchChangeReplicationProcessor(writer);
    record = mock(AdminLogRecord.class);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(new AdminDispatchPlanRecord());
  }

  private AdminDispatchPlanRecord plan(final DispatchPlanState state) {
    final AdminDispatchPlanRecord plan = new AdminDispatchPlanRecord();
    plan.setDispatchPlanId(10L);
    plan.setDispatchState(state);
    return plan;
  }

  @Test
  void executingAdminPlanRejectsNewDispatch() {
    when(repositoryAdmin.getDispatchPlan()).thenReturn(plan(DispatchPlanState.EXECUTED));

    processor.processRecord(record);

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(-1), any(String.class));
    verify(writer, never())
        .addCommand(anyLong(), any(AdminDispatchPlanLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void unappliedAdminPlanIsReplacedByNewDispatch() {
    when(repositoryAdmin.getDispatchPlan()).thenReturn(plan(DispatchPlanState.PLAN_SUCCEED));

    processor.processRecord(record);

    verify(writer, never()).adErrorResponse(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(String.class));
    verify(writer)
        .addCommand(eq(10L), eq(AdminDispatchPlanLifeCycle.CANCELING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(anyLong(), eq(AdminDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }

  @Test
  void terminalAdminPlanDoesNotBlockNewDispatch() {
    when(repositoryAdmin.getDispatchPlan()).thenReturn(plan(DispatchPlanState.CANCELED));

    processor.processRecord(record);

    verify(writer, never()).adErrorResponse(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(String.class));
    verify(writer, never())
        .addCommand(anyLong(), eq(AdminDispatchPlanLifeCycle.CANCELING), anyLong(), any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(anyLong(), eq(AdminDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }

  @Test
  void emptyAdminSlotCreatesPlanDirectly() {
    when(repositoryAdmin.getDispatchPlan()).thenReturn(null);

    processor.processRecord(record);

    verify(writer, never()).adErrorResponse(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(String.class));
    verify(writer)
        .addCommand(anyLong(), eq(AdminDispatchPlanLifeCycle.PLAN_CREATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }
}
