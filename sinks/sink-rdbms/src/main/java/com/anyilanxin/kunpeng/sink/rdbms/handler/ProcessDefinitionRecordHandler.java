/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
