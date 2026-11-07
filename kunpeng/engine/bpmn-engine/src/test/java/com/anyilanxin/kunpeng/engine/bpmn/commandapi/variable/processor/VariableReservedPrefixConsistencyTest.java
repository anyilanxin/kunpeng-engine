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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.variable.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove.VariableRemoveRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 引擎保留变量前缀一致性测试：客户端路径（变量更新/删除 API、行为漏斗）写入/删除保留前缀一律拒绝， 引擎内部写入（快照/载荷）放行——防止打穿批处理游标取元素的快照。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class VariableReservedPrefixConsistencyTest {
  private static final long REQUEST_ID = 300L;
  private static final long INSTANCE_ID = 100L;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repository;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
  }

  private ActivityInstanceRecord activityInstance() {
    return new ActivityInstanceRecord()
        .setActivityInstanceId(INSTANCE_ID)
        .setProcessInstanceId(9L)
        .setRootProcessInstanceId(9L)
        .setProcessDefinitionId(5L)
        .setProcessDefinitionKey("p1");
  }

  private <T extends com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue> BusinessLogRecord<T>
      logRecord(final T value) {
    final BusinessLogRecord<T> record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(1L);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    return record;
  }

  @Test
  void clientUpdateWithReservedNameIsRejected() {
    final VariableUpdateRequestRecord value =
        new VariableUpdateRequestRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setVariables(Map.of(VariableBehavior.ENGINE_RESERVED_PREFIX + "x", 1, "ok", 2));
    new VariableUpdateApiProcessor(writer).processRecord(logRecord(value));

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(-1), anyString());
    verify(writer, never()).addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any());
  }

  @Test
  void clientUpdateWithoutReservedNamePasses() {
    final VariableUpdateRequestRecord value =
        new VariableUpdateRequestRecord().setActivityInstanceId(INSTANCE_ID).setVariables(Map.of("ok", 2));
    new VariableUpdateApiProcessor(writer).processRecord(logRecord(value));

    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    verify(writer)
        .addEvent(eq(INSTANCE_ID), eq(VariableLifeCycle.UPDATE), eq(REQUEST_ID), any());
  }

  @Test
  void behaviorFunnelRejectsReservedName() {
    final VariableBehavior behavior = new VariableBehavior(writer);
    final ActivityContent content = new ActivityContent(activityInstance(), REQUEST_ID);
    assertThrows(
        IllegalArgumentException.class,
        () -> behavior.variableUpdate(content, Map.of(AddSignBehavior.ADD_SIGN_PAYLOAD, 1)));
  }

  @Test
  void internalWriteAllowsReservedName() {
    final VariableBehavior behavior = new VariableBehavior(writer);
    final ActivityContent content = new ActivityContent(activityInstance(), REQUEST_ID);
    assertDoesNotThrow(
        () -> behavior.variableUpdateInternal(content, Map.of(AddSignBehavior.ADD_SIGN_PAYLOAD, 1)));
    verify(writer).addEvent(anyLong(), any(ValueLifeCycle.class), eq(REQUEST_ID), any());
  }

  @Test
  void removeNamedReservedKeyIsRejected() {
    final VariableRemoveRequestRecord value =
        new VariableRemoveRequestRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setRemoveVariable(java.util.List.of("plain", VariableBehavior.ENGINE_RESERVED_PREFIX + "y"));
    new VariableRemoveApiProcessor(writer).processRecord(logRecord(value));

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(-1), anyString());
    verify(writer, never()).addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any());
  }

  @Test
  void removeAllWithReservedInScopeIsRejected() {
    when(repository.variableRepository().getRecord(INSTANCE_ID))
        .thenReturn(
            Optional.of(
                new VariableRecord()
                    .setScopId(INSTANCE_ID)
                    .setVariables(Map.<String, Object>of(AddSignBehavior.ADD_SIGN_PAYLOAD, 1, "plain", 2))));
    final VariableRemoveRequestRecord value =
        new VariableRemoveRequestRecord().setActivityInstanceId(INSTANCE_ID).setRemoveAll(true);
    new VariableRemoveApiProcessor(writer).processRecord(logRecord(value));

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(-1), anyString());
    verify(writer, never()).addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any());
  }

  @Test
  void removeAllWithoutReservedPasses() {
    when(repository.variableRepository().getRecord(INSTANCE_ID))
        .thenReturn(
            Optional.of(
                new VariableRecord().setScopId(INSTANCE_ID).setVariables(Map.<String, Object>of("plain", 2))));
    final VariableRemoveRequestRecord value =
        new VariableRemoveRequestRecord().setActivityInstanceId(INSTANCE_ID).setRemoveAll(true);
    new VariableRemoveApiProcessor(writer).processRecord(logRecord(value));

    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    verify(writer)
        .addEvent(eq(INSTANCE_ID), eq(VariableLifeCycle.REMOVE), eq(REQUEST_ID), any());
  }
}
