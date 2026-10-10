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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.VARIABLE;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.VariableDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.JsonValues;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 变量：一条记录可携带同 scope 下的多个变量，展开为多行，行主键是 {@code (scope_id, name)}。 创建保存全量行，更新只改值与版本。
 *
 * <p>{@code REMOVED} 事件不携带可定位的值明细（历史清理会连带删除实例变量），这里不响应。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class VariableRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(VariableLifeCycle.CREATED, VariableLifeCycle.UPDATED);

  @Override
  public ValueType valueType() {
    return ValueType.VARIABLE;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final VariableRecordValue value = (VariableRecordValue) record.getValue();
    final var variables = value.getVariables();
    if (variables == null || variables.isEmpty()) {
      return;
    }
    final var creating = record.getValueState() == VariableLifeCycle.CREATED;
    for (final var entry : variables.entrySet()) {
      final var model = new VariableDbModel();
      model.setScopeId(value.getScopId());
      model.setName(entry.getKey());
      model.setValueJson(JsonValues.toJson(entry.getValue()));
      model.setRevision(value.getRev());
      final List<Object> key = List.of(value.getScopId(), entry.getKey());
      if (creating) {
        model.setParentScopeId(value.getParentScopId() > 0 ? value.getParentScopId() : null);
        model.setProcessInstanceId(value.getProcessInstanceId());
        model.setProcessDefinitionId(value.getProcessDefinitionId());
        model.setResourceId(record.getResourceId());
        buffer.offer(RowChange.insert(VARIABLE, key, model));
      } else {
        buffer.offer(RowChange.update(VARIABLE, key, model));
      }
    }
  }
}
