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
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.INCIDENT;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.MESSAGE_SUBSCRIPTION;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.PROCESS_INSTANCE;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.SIGNAL_SUBSCRIPTION;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.TIMER;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.USER_TASK;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.VARIABLE;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;

/**
 * 历史清理：按 {@code process_instance_id} 连带删除该实例的全部运行轨迹（含定时器与订阅，避免实例结束后残留孤儿行）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class HistoryCleanupRecordHandler implements RecordModelHandler {

  @Override
  public ValueType valueType() {
    return ValueType.HISTORY_CLEANUP;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return lifecycle == HistoryCleanupLifeCycle.TRIGGERED;
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final HistoryCleanupRecordValue value = (HistoryCleanupRecordValue) record.getValue();
    final var processInstanceId = value.getProcessInstanceId();
    buffer.offer(
        RowChange.deleteByColumn(PROCESS_INSTANCE, "process_instance_id", processInstanceId));
    buffer.offer(
        RowChange.deleteByColumn(ACTIVITY_INSTANCE, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(VARIABLE, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(USER_TASK, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(INCIDENT, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(TIMER, "process_instance_id", processInstanceId));
    buffer.offer(
        RowChange.deleteByColumn(MESSAGE_SUBSCRIPTION, "process_instance_id", processInstanceId));
    buffer.offer(
        RowChange.deleteByColumn(SIGNAL_SUBSCRIPTION, "process_instance_id", processInstanceId));
  }
}
