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

  /**
   * 命令批次内变量快照缓存：scopeId → 变量集。 首次触碰从仓储加载，本行为内的更新写后回填；其他写入方经 {@link LogEventWriter#addEvent}
   * 的变量记录失效钩子摘除对应项。批次开始时整体清空（引擎 actor 单线程，无并发访问）。
   */
  private final Map<Long, Map<String, Object>> batchVariableCache = new HashMap<>();

  public VariableBehavior(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    variable = repository.variableRepository();
    activityInstance = repository.instanceRepository();
  }

  /** 批次开始清空缓存：跨批次仓储可能有本批次之外的变更，不复用旧快照 */
  public void resetBatchCache() {
    batchVariableCache.clear();
  }

  /** 其他写入方落了变量记录后摘除对应快照，下一次读取回仓储加载最新值 */
  public void invalidateScope(final long scopeId) {
    batchVariableCache.remove(scopeId);
  }

  /** 读取作用域变量（缓存穿透加载，返回缓存所有的映射实例，仅本类内部使用） */
  private Map<String, Object> scopeVariables(final long scopeId) {
    return batchVariableCache.computeIfAbsent(
        scopeId,
        id -> {
          final Map<String, Object> loaded = new HashMap<>();
          variable.getRecord(id).ifPresent(record -> loaded.putAll(record.getVariables()));
          return loaded;
        });
  }

  public Map<String, Object> getDirectVariable(final ActivityContent activityContext) {
    return new HashMap<>(scopeVariables(activityContext.getActivityInstanceId()));
  }

  public ScriptContext scriptContext(final UserTaskRecord userTaskRecord) {
    return scriptContext(
        userTaskRecord.getProcessInstanceId(), userTaskRecord.getActivityInstanceId());
  }

  public Map<String, Object> getVariable(
      final long processInstanceId, final long activityInstanceId) {
    final Map<String, Object> variableMap = new HashMap<>(scopeVariables(activityInstanceId));

    long currentActivityInstanceId = activityInstanceId;
    long currentParentActivityInstanceId;
    do {
      final ActivityInstanceRecord record = activityInstance.getRecord(currentActivityInstanceId);
      if (record != null) {
        currentParentActivityInstanceId = record.getParentActivityInstanceId();
        currentActivityInstanceId = currentParentActivityInstanceId;
        // 子作用域优先：父作用域变量只补齐子作用域未定义的键
        scopeVariables(currentParentActivityInstanceId).forEach(variableMap::putIfAbsent);
      } else {
        scopeVariables(processInstanceId).forEach(variableMap::putIfAbsent);
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
    updateScopeVariables(
        content.getActivityInstanceId(),
        content.getParentActivityInstanceId(),
        content.getProcessDefinitionId(),
        content.getProcessInstanceId(),
        content.getRequestId(),
        variables);
  }

  public void variableUpdate(
      final long processInstanceId,
      final long processDefinitionId,
      final long requestId,
      final Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) {
      return;
    }
    updateScopeVariables(
        processInstanceId,
        processInstanceId,
        processDefinitionId,
        processInstanceId,
        requestId,
        variables);
  }

  public void variableUpdate(final ActivityContent content, final DirectBuffer variables) {
    if (variables == null) {
      return;
    }
    updateScopeVariables(
        content.getActivityInstanceId(),
        content.getParentActivityInstanceId(),
        content.getProcessDefinitionId(),
        content.getProcessInstanceId(),
        content.getRequestId(),
        convertToMap(variables));
  }

  /** 变量更新统一路径：合并（旧值 ← 新值）后落事件，写后回填缓存。 事件写入会经失效钩子摘除旧快照，随后以合并结果重建。 */
  private void updateScopeVariables(
      final long scopeId,
      final long parentScopeId,
      final long processDefinitionId,
      final long processInstanceId,
      final long requestId,
      final Map<String, Object> variables) {
    final Map<String, Object> merged = scopeVariables(scopeId);
    // 有旧变量即视为更新；空变量集时补一次仓储存在性判定（空记录也算已存在，避免重复 CREATED）
    final boolean existed = !merged.isEmpty() || variable.getRecord(scopeId).isPresent();
    merged.putAll(variables);

    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setParentScopId(parentScopeId);
    variableRecord.setScopId(scopeId);
    variableRecord.setProcessDefinitionId(processDefinitionId);
    variableRecord.setProcessInstanceId(processInstanceId);
    variableRecord.setVariables(merged);

    writer.addEvent(
        scopeId,
        existed ? VariableLifeCycle.UPDATED : VariableLifeCycle.CREATED,
        requestId,
        variableRecord);
    batchVariableCache.put(scopeId, merged);
  }

  public void variableCreate(
      final long requestId, final ActivityContent content, final Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) {
      return;
    }
    createScopeVariables(
        requestId,
        content.getActivityInstanceId(),
        content.getParentActivityInstanceId(),
        content.getProcessDefinitionId(),
        content.getProcessInstanceId(),
        variables);
  }

  public void variableCreate(
      final long requestId,
      final ActivityInstanceRecord instanceRecord,
      final DirectBuffer variables) {
    if (variables == null) {
      return;
    }
    createScopeVariables(
        requestId,
        instanceRecord.getActivityInstanceId(),
        instanceRecord.getParentActivityInstanceId(),
        instanceRecord.getProcessDefinitionId(),
        instanceRecord.getProcessInstanceId(),
        convertToMap(variables));
  }

  public void variableCreate(
      final long requestId,
      final ProcessInstanceRecord instanceRecord,
      final DirectBuffer variables) {
    if (variables == null) {
      return;
    }
    createScopeVariables(
        requestId,
        instanceRecord.getProcessInstanceId(),
        instanceRecord.getProcessInstanceId(),
        instanceRecord.getProcessDefinitionId(),
        instanceRecord.getProcessInstanceId(),
        convertToMap(variables));
  }

  /** 变量创建统一路径：落事件后回填缓存（写路径的失效钩子先摘除，再以新值重建） */
  private void createScopeVariables(
      final long requestId,
      final long scopeId,
      final long parentScopeId,
      final long processDefinitionId,
      final long processInstanceId,
      final Map<String, Object> variables) {
    final VariableRecord variableRecord = new VariableRecord();
    variableRecord.setVariables(variables);
    variableRecord.setParentScopId(parentScopeId);
    variableRecord.setScopId(scopeId);
    variableRecord.setProcessDefinitionId(processDefinitionId);
    variableRecord.setProcessInstanceId(processInstanceId);

    writer.addEvent(
        variableRecord.getScopId(), VariableLifeCycle.CREATED, requestId, variableRecord);
    batchVariableCache.put(scopeId, new HashMap<>(variables));
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
