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
package com.anyilanxin.kunpeng.sink.rdbms.handler;

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.PROCESS_DEFINITION;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.ProcessDefinitionDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.JsonValues;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/**
 * 流程定义：只在 {@code CREATED} 时保存一行（含资源内容与启动事件），删除事件被忽略以保留历史。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ProcessDefinitionRecordHandler implements RecordModelHandler {

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_DEFINITION;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return lifecycle == ProcessDefinitionLifeCycle.CREATED;
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final ProcessDefinitionRecordValue value = (ProcessDefinitionRecordValue) record.getValue();
    final var model = new ProcessDefinitionDbModel();
    model.setProcessDefinitionId(value.getProcessDefinitionId());
    model.setDefinitionKey(value.getProcessDefinitionKey());
    model.setDefinitionName(value.getProcessDefinitionName());
    model.setVersion(value.getProcessDefinitionVersion());
    model.setVersionTag(value.getVersionTag());
    model.setHistoryTimeToLive(value.getHistoryTimeToLive());
    model.setDeploymentId(value.getDeploymentId());
    model.setResourceDefinitionId(value.getResourceDefinitionId());
    model.setResourceName(value.getResourceDefinitionName());
    model.setChecksum(
        value.getChecksum() == null ? null : HexFormat.of().formatHex(value.getChecksum()));
    model.setStarterEvents(JsonValues.toJson(value.getStarterEvents()));
    model.setCandidateStarterGroups(JsonValues.toJson(value.getCandidateStarterGroups()));
    model.setCandidateStarterUsers(JsonValues.toJson(value.getCandidateStarterUsers()));
    model.setResourceContent(
        value.getResource() == null
            ? null
            : new String(value.getResource(), StandardCharsets.UTF_8));
    model.setResourceId(record.getResourceId());
    buffer.offer(
        RowChange.insert(PROCESS_DEFINITION, List.of(value.getProcessDefinitionId()), model));
  }
}
