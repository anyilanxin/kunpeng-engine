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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.INCIDENT;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.IncidentDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 故障（Incident）：值不携带状态，按生命周期派生——创建即 {@code OPEN}，解决/删除时更新为终态。 创建时间取记录时间戳，只在插入时写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class IncidentRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(IncidentLifeCycle.CREATED, IncidentLifeCycle.RESOLVED, IncidentLifeCycle.DELETED);

  @Override
  public ValueType valueType() {
    return ValueType.INCIDENT;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final IncidentRecordValue value = (IncidentRecordValue) record.getValue();
    final var lifecycle = (IncidentLifeCycle) record.getValueState();
    if (lifecycle == IncidentLifeCycle.CREATED) {
      final var model = new IncidentDbModel();
      model.setIncidentId(value.getIncidentId());
      model.setIncidentType(nameOf(value.getIncidentType()));
      model.setIncidentMessage(value.getIncidentMessage());
      model.setProcessInstanceId(value.getProcessInstanceId());
      model.setActivityInstanceId(value.getActivityInstanceId());
      model.setProcessDefinitionId(value.getProcessDefinitionId());
      model.setDefinitionKey(value.getProcessDefinitionKey());
      model.setActivityKey(value.getActivityDefinitionKey());
      model.setTaskId(value.getTaskId() > 0 ? value.getTaskId() : null);
      model.setJobId(value.getJobId() > 0 ? value.getJobId() : null);
      model.setRelatedValueType(nameOf(value.getIncidentRecordValueType()));
      final var relatedLifecycle = value.getIncidentRecordLifeCycle();
      model.setRelatedLifecycle(relatedLifecycle == null ? null : relatedLifecycle.name());
      model.setState("OPEN");
      model.setCreatedTime(RecordModelHandler.at(record.getTimestamp()));
      model.setResourceId(record.getResourceId());
      buffer.offer(RowChange.insert(INCIDENT, List.of(value.getIncidentId()), model));
      return;
    }
    final var model = new IncidentDbModel();
    model.setIncidentId(value.getIncidentId());
    model.setState(deriveState(lifecycle));
    model.setResolvedTime(
        lifecycle == IncidentLifeCycle.RESOLVED
            ? RecordModelHandler.at(record.getTimestamp())
            : null);
    buffer.offer(RowChange.update(INCIDENT, List.of(value.getIncidentId()), model));
  }

  private static String deriveState(final IncidentLifeCycle lifecycle) {
    return switch (lifecycle) {
      case RESOLVED -> "RESOLVED";
      case DELETED -> "DELETED";
      default -> "OPEN";
    };
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
