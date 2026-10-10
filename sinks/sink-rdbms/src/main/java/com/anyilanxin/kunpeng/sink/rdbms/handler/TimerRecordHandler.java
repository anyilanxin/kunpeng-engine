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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.TIMER;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerEventRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.sink.rdbms.model.TimerDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 定时器：创建保存全量行，触发/取消只更新到期、重复与状态列；终态保留行以便审计。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class TimerRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(TimerLifeCycle.CREATED, TimerLifeCycle.TRIGGERED, TimerLifeCycle.CANCELED);

  @Override
  public ValueType valueType() {
    return ValueType.TIMER;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final TimerEventRecordValue value = (TimerEventRecordValue) record.getValue();
    if (record.getValueState() == TimerLifeCycle.CREATED) {
      final var model = new TimerDbModel();
      model.setTimerId(value.getTimerId());
      model.setDueTime(RecordModelHandler.at(value.getDueDate()));
      model.setRepetitions(value.getRepetitions());
      model.setProcessInstanceId(value.getProcessInstanceId());
      model.setActivityInstanceId(value.getActivityInstanceId());
      model.setProcessDefinitionId(value.getProcessDefinitionId());
      model.setDefinitionKey(value.getProcessDefinitionKey());
      model.setActivityKey(value.getActivityDefinitionKey());
      model.setTimerElementType(nameOf(value.getTimerElementType()));
      model.setTimerType(value.getTimerType());
      model.setTimerContent(value.getTimerContent());
      model.setState(nameOf(value.getState()));
      model.setResourceId(record.getResourceId());
      buffer.offer(RowChange.insert(TIMER, List.of(value.getTimerId()), model));
      return;
    }
    final var model = new TimerDbModel();
    model.setTimerId(value.getTimerId());
    model.setDueTime(RecordModelHandler.at(value.getDueDate()));
    model.setRepetitions(value.getRepetitions());
    model.setState(nameOf(value.getState()));
    buffer.offer(RowChange.update(TIMER, List.of(value.getTimerId()), model));
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
