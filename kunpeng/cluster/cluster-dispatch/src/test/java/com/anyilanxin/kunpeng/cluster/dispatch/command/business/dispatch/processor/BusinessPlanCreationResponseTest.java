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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.BusinessDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableBusinessRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 业务面计划创建响应测试：计划生成即按计划类型回响应（携带计划与集群元数据，不再挂到客户端超时）；
 * 成员不足进入延迟等待时回明确错误响应。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BusinessPlanCreationResponseTest {
  private static final long REQUEST_ID = 300L;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repositoryBusiness;
  private ClusterMembershipService membershipService;
  private BusinessDispatchClusterPlanCreatingProcessor processor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final AdminImmutableRepository repository = mock(AdminImmutableRepository.class);
    repositoryBusiness = mock(ImmutableBusinessRepository.class);
    membershipService = mock(ClusterMembershipService.class);
    when(writer.getRepository()).thenReturn(repository);
    when(repository.businessRepository()).thenReturn(repositoryBusiness);
    when(repository.keyRepository()).thenReturn(mock());
    when(repository.sourceRepository()).thenReturn(mock());
    when(writer.getClusterTopologyService()).thenReturn(mock());
    when(writer.getMembershipService()).thenReturn(membershipService);
    when(writer.getDelayChecker()).thenReturn(mock());
    when(writer.nextKey()).thenReturn(1L);
    when(writer.millis()).thenReturn(0L);
    processor = new BusinessDispatchClusterPlanCreatingProcessor(writer);
  }

  @Test
  void generatedPlanRespondsWithTypeMatchedLifecycle() {
    final BusinessClusterMetaRecord clusterMeta = new BusinessClusterMetaRecord();
    clusterMeta.setCurrentPartitionCount(3);
    when(repositoryBusiness.getClusterMeta()).thenReturn(clusterMeta);
    final BusinessDispatchPlanRecord plan = new BusinessDispatchPlanRecord();
    plan.setDispatchPlanId(77L);
    plan.setDispatchPlanType(BusinessDispatchType.CHANGE_PARTITION);

    processor.respondPlanCreated(REQUEST_ID, plan);

    final ArgumentCaptor<BusinessChangeResponseRecord> captor =
        ArgumentCaptor.forClass(BusinessChangeResponseRecord.class);
    verify(writer)
        .adResponse(
            eq(BusinessDispatchApiValueLifeCycle.CHANGE_PARTITION_RESPONSE),
            eq(REQUEST_ID),
            captor.capture());
    assertEquals(77L, captor.getValue().getDispatchPlan().getDispatchPlanId());
    assertEquals(3, captor.getValue().getClusterMeta().getCurrentPartitionCount());
  }

  @Test
  void insufficientMembersRespondsWithDeferredError() {
    when(membershipService.getMemberIds(any())).thenReturn(Set.of());
    final AdminLogRecord<BusinessDispatchPlanRecord> record = mock(AdminLogRecord.class);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    final BusinessDispatchPlanRecord plan = new BusinessDispatchPlanRecord();
    plan.setExpectReplicationFactor(3);
    when(record.getValue()).thenReturn(plan);

    processor.processRecord(record);

    verify(writer)
        .addEvent(
            anyLong(),
            eq(com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle.PLAN_DELAYED),
            eq(REQUEST_ID),
            any());
    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(-1), any(String.class));
  }
}
