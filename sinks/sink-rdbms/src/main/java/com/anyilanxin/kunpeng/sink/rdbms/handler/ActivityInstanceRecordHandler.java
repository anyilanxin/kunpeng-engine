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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.ACTIVITY_INSTANCE;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.ActivityInstanceDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 活动实例（流程节点执行轨迹）：激活类事件保存全量行（值自带 {@code start_time}），流动/完成/终止类事件只更新可变列。
 *
 * <p>连线（SEQUENCE_FLOW）实例的首事件是 TAKING 而非 ACTIVATING（见引擎 SequenceFlowBehavior）， TAKING 也走保存建行； 连线的
 * {@code start_activity_instance_id} 即流出节点实例，是其起点语义的承载。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ActivityInstanceRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> START_EVENTS =
      Set.of(
          ActivityInstanceLifeCycle.ACTIVATING,
          ActivityInstanceLifeCycle.ACTIVATING_AFTER,
          ActivityInstanceLifeCycle.ACTIVATED,
          ActivityInstanceLifeCycle.TAKING);

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(
          ActivityInstanceLifeCycle.ACTIVATING,
          ActivityInstanceLifeCycle.ACTIVATING_AFTER,
          ActivityInstanceLifeCycle.ACTIVATED,
          ActivityInstanceLifeCycle.TAKING,
          ActivityInstanceLifeCycle.TAKEN,
          ActivityInstanceLifeCycle.COMPLETING,
          ActivityInstanceLifeCycle.COMPLETING_AFTER,
          ActivityInstanceLifeCycle.COMPLETED,
          ActivityInstanceLifeCycle.TERMINATING,
          ActivityInstanceLifeCycle.TERMINATING_AFTER,
          ActivityInstanceLifeCycle.TERMINATED);

  @Override
  public ValueType valueType() {
    return ValueType.ACTIVITY;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final ActivityInstanceRecordValue value = (ActivityInstanceRecordValue) record.getValue();
    if (START_EVENTS.contains(record.getValueState())) {
      buffer.offer(
          RowChange.insert(
              ACTIVITY_INSTANCE, List.of(value.getActivityInstanceId()), full(value, record)));
      return;
    }
    final var model = new ActivityInstanceDbModel();
    model.setActivityInstanceId(value.getActivityInstanceId());
    model.setTaskId(positive(value.getTaskId()));
    model.setAssignee(value.getAssignee());
    model.setState(nameOf(value.getState()));
    model.setIncidentId(positive(value.getIncidentId()));
    model.setEndTime(RecordModelHandler.at(value.getEndTime()));
    model.setDuration(positive(value.getDuration()));
    model.setRevision(value.getRev());
    buffer.offer(
        RowChange.update(ACTIVITY_INSTANCE, List.of(value.getActivityInstanceId()), model));
  }

  private static ActivityInstanceDbModel full(
      final ActivityInstanceRecordValue value, final BusinessEventRecord<?> record) {
    final var model = new ActivityInstanceDbModel();
    model.setActivityInstanceId(value.getActivityInstanceId());
    model.setProcessInstanceId(value.getProcessInstanceId());
    model.setRootProcessInstanceId(positive(value.getRootProcessInstanceId()));
    model.setParentActivityInstanceId(positive(value.getParentActivityInstanceId()));
    model.setCallProcessInstanceId(positive(value.getCallProcessInstanceId()));
    model.setProcessDefinitionId(value.getProcessDefinitionId());
    model.setDefinitionKey(value.getProcessDefinitionKey());
    model.setActivityKey(value.getActivityDefinitionKey());
    model.setActivityName(value.getActivityDefinitionName());
    model.setActivityType(nameOf(value.getActivityDefinitionType()));
    model.setTaskId(positive(value.getTaskId()));
    model.setAssignee(value.getAssignee());
    model.setStartActivityDefinitionKey(blankToNull(value.getStartActivityDefinitionKey()));
    model.setStartActivityInstanceId(positive(value.getStartActivityInstanceId()));
    model.setState(nameOf(value.getState()));
    model.setIncidentId(positive(value.getIncidentId()));
    model.setSequenceCounter(value.getSequenceCounter());
    model.setStartTime(RecordModelHandler.at(value.getStartTime()));
    model.setEndTime(RecordModelHandler.at(value.getEndTime()));
    model.setDuration(positive(value.getDuration()));
    model.setRevision(value.getRev());
    model.setResourceId(record.getResourceId());
    return model;
  }

  private static Long positive(final long id) {
    return id > 0 ? id : null;
  }

  private static String blankToNull(final String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
