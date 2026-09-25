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

import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.variable.ImmutableVariableRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.agrona.DirectBuffer;

/**
 * 变量行为：流程变量的读写与传播语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableBehavior {
  private final LogEventWriter writer;
  private final ImmutableVariableRepository variable;
  private final ImmutableActivityInstanceRepository activityInstance;

  public VariableBehavior(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    variable = repository.variableRepository();
    activityInstance = repository.instanceRepository();
  }

  public Map<String, Object> getDirectVariable(final ActivityContent activityContext) {
    final long currentActivityInstanceId = activityContext.getActivityInstanceId();
    final Optional<VariableRecord> optional = variable.getRecord(currentActivityInstanceId);
    final Map<String, Object> variableMap = new HashMap<>();
    optional.ifPresent(record -> variableMap.putAll(record.getVariables()));
    return variableMap;
  }

  public ScriptContext scriptContext(final UserTaskRecord userTaskRecord) {
    return scriptContext(
        userTaskRecord.getProcessInstanceId(), userTaskRecord.getActivityInstanceId());
  }

  public Map<String, Object> getVariable(
      final long processInstanceId, final long activityInstanceId) {
    long currentActivityInstanceId = activityInstanceId;
    final Optional<VariableRecord> optional = variable.getRecord(currentActivityInstanceId);
    final Map<String, Object> variableMap = new HashMap<>();
    if (optional.isEmpty()) {
      if (processInstanceId == currentActivityInstanceId) {
        return new HashMap<>();
      }
    } else {
      variableMap.putAll(optional.get().getVariables());
    }

    long currentParentActivityInstanceId = -1;
    do {
      final ActivityInstanceRecord record = activityInstance.getRecord(currentActivityInstanceId);
      if (record != null) {
        currentParentActivityInstanceId = record.getParentActivityInstanceId();
        currentActivityInstanceId = currentParentActivityInstanceId;
        final Optional<VariableRecord> currentOptional =
            variable.getRecord(currentParentActivityInstanceId);
        if (currentOptional.isPresent()) {
          final Map<String, Object> currentVariable = currentOptional.get().getVariables();
          final Map<String, Object> newCurrentVariable = new HashMap<>(currentVariable);
          newCurrentVariable.putAll(variableMap);
          variableMap.clear();
          variableMap.putAll(newCurrentVariable);
        }
      } else {
        final Optional<VariableRecord> currentOptional = variable.getRecord(processInstanceId);
        if (currentOptional.isPresent()) {
          final Map<String, Object> currentVariable = currentOptional.get().getVariables();
          final Map<String, Object> newCurrentVariable = new HashMap<>(currentVariable);
          newCurrentVariable.putAll(variableMap);
          variableMap.clear();
          variableMap.putAll(newCurrentVariable);
        }
        break;
      }
    } while (processInstanceId != currentParentActivityInstanceId);
    return variableMap;
  }

  public ScriptContext scriptContext(final long processInstanceId, final long activityInstanceId) {
    return () -> getVariable(processInstanceId, activityInstanceId);
  }

  public ScriptContext scriptContext(final ActivityContent activityContext) {
    return scriptContext(
        activityContext.getProcessInstanceId(), activityContext.getActivityInstanceId());
  }

  public void variableUpdate(final ActivityContent content, final Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) {
      return;
    }

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setParentScopId(content.getParentActivityInstanceId());
    variableRecord.setScopId(content.getActivityInstanceId());
    variableRecord.setProcessDefinitionId(content.getProcessDefinitionId());
    variableRecord.setProcessInstanceId(content.getProcessInstanceId());

    final Optional<VariableRecord> optional = variable.getRecord(content.getActivityInstanceId());
    if (optional.isPresent()) {
      final VariableRecord oldRecord = optional.get();
      final Map<String, Object> newVariableMap = new HashMap<>();
      newVariableMap.putAll(oldRecord.getVariables());
      newVariableMap.putAll(variables);
      variableRecord.setVariables(newVariableMap);
      writer.addEvent(
          variableRecord.getScopId(),
          VariableLifeCycle.UPDATED,
          content.getRequestId(),
          variableRecord);
    } else {
      variableRecord.setVariables(variables);
      writer.addEvent(
          variableRecord.getScopId(),
          VariableLifeCycle.CREATED,
          content.getRequestId(),
          variableRecord);
    }
  }

  public void variableUpdate(
      final long processInstanceId,
      final long processDefinitionId,
      final long requestId,
      final Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) {
      return;
    }

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setParentScopId(processInstanceId);
    variableRecord.setScopId(processInstanceId);
    variableRecord.setProcessDefinitionId(processDefinitionId);
    variableRecord.setProcessInstanceId(processInstanceId);

    final Optional<VariableRecord> optional = variable.getRecord(processInstanceId);
    if (optional.isPresent()) {
      final VariableRecord oldRecord = optional.get();
      final Map<String, Object> newVariableMap = new HashMap<>();
      newVariableMap.putAll(oldRecord.getVariables());
      newVariableMap.putAll(variables);
      variableRecord.setVariables(newVariableMap);
      writer.addEvent(
          variableRecord.getScopId(), VariableLifeCycle.UPDATED, requestId, variableRecord);
    } else {
      variableRecord.setVariables(variables);
      writer.addEvent(
          variableRecord.getScopId(), VariableLifeCycle.CREATED, requestId, variableRecord);
    }
  }

  public void variableUpdate(final ActivityContent content, final DirectBuffer variables) {
    if (variables == null) {
      return;
    }

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setParentScopId(content.getParentActivityInstanceId());
    variableRecord.setScopId(content.getActivityInstanceId());
    variableRecord.setProcessDefinitionId(content.getProcessDefinitionId());
    variableRecord.setProcessInstanceId(content.getProcessInstanceId());

    final Optional<VariableRecord> optional = variable.getRecord(content.getActivityInstanceId());
    if (optional.isPresent()) {
      final VariableRecord oldRecord = optional.get();
      final Map<String, Object> newVariableMap = new HashMap<>();
      newVariableMap.putAll(oldRecord.getVariables());
      newVariableMap.putAll(convertToMap(variables));
      variableRecord.setVariables(newVariableMap);
      writer.addEvent(
          variableRecord.getScopId(),
          VariableLifeCycle.UPDATED,
          content.getRequestId(),
          variableRecord);
    } else {
      variableRecord.setVariables(convertToMap(variables));
      writer.addEvent(
          variableRecord.getScopId(),
          VariableLifeCycle.CREATED,
          content.getRequestId(),
          variableRecord);
    }
  }

  public void variableCreate(
      final long requestId, final ActivityContent content, final Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) {
      return;
    }

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setVariables(variables);
    variableRecord.setParentScopId(content.getParentActivityInstanceId());
    variableRecord.setScopId(content.getActivityInstanceId());
    variableRecord.setProcessDefinitionId(content.getProcessDefinitionId());
    variableRecord.setProcessInstanceId(content.getProcessInstanceId());

    writer.addEvent(
        variableRecord.getScopId(), VariableLifeCycle.CREATED, requestId, variableRecord);
  }

  public void variableCreate(
      final long requestId,
      final ActivityInstanceRecord instanceRecord,
      final DirectBuffer variables) {
    if (variables == null) {
      return;
    }

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setVariables(variables);
    variableRecord.setParentScopId(instanceRecord.getParentActivityInstanceId());
    variableRecord.setScopId(instanceRecord.getActivityInstanceId());
    variableRecord.setProcessDefinitionId(instanceRecord.getProcessDefinitionId());
    variableRecord.setProcessInstanceId(instanceRecord.getProcessInstanceId());

    writer.addEvent(
        variableRecord.getScopId(), VariableLifeCycle.CREATED, requestId, variableRecord);
  }

  public void variableCreate(
      final long requestId,
      final ProcessInstanceRecord instanceRecord,
      final DirectBuffer variables) {
    if (variables == null) {
      return;
    }

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setVariables(variables);
    variableRecord.setParentScopId(instanceRecord.getProcessInstanceId());
    variableRecord.setScopId(instanceRecord.getProcessInstanceId());
    variableRecord.setProcessDefinitionId(instanceRecord.getProcessDefinitionId());
    variableRecord.setProcessInstanceId(instanceRecord.getProcessInstanceId());

    writer.addEvent(
        variableRecord.getScopId(), VariableLifeCycle.CREATED, requestId, variableRecord);
  }

  public void variableHistory(final long requestId, final ProcessInstanceRecord instanceRecord) {
    final Optional<VariableRecord> optional =
        variable.getRecord(instanceRecord.getProcessInstanceId());
    optional.ifPresent(
        variableRecord ->
            writer.addEvent(
                variableRecord.getScopId(), VariableLifeCycle.HISTORY, requestId, variableRecord));
  }

  public void variableHistory(final ActivityContent content) {
    final Optional<VariableRecord> optional = variable.getRecord(content.getActivityInstanceId());
    optional.ifPresent(
        variableRecord ->
            writer.addEvent(
                variableRecord.getScopId(),
                VariableLifeCycle.HISTORY,
                content.getRequestId(),
                variableRecord));
  }
}
