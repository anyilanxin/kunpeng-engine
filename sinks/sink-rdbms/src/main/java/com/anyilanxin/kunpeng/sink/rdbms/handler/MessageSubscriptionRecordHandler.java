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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.MESSAGE_SUBSCRIPTION;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.MessageSubscriptionDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 消息订阅：创建保存全量行，关联与取消只更新状态列（值本身不携带状态，按生命周期派生）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class MessageSubscriptionRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(
          MessageSubscriptionLifeCycle.CREATED,
          MessageSubscriptionLifeCycle.CORRELATED,
          MessageSubscriptionLifeCycle.CANCELED);

  @Override
  public ValueType valueType() {
    return ValueType.MESSAGE_SUBSCRIPTION;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final MessageSubscriptionRecordValue value = (MessageSubscriptionRecordValue) record.getValue();
    if (record.getValueState() == MessageSubscriptionLifeCycle.CREATED) {
      final var model = new MessageSubscriptionDbModel();
      model.setMessageSubscriptionId(value.getMessageSubscriptionId());
      model.setMessageName(value.getMessageName());
      model.setMessageType(nameOf(value.getMessageType()));
      model.setCorrelationKey(value.getCorrelationKey());
      model.setProcessInstanceId(value.getProcessInstanceId());
      model.setActivityInstanceId(value.getActivityInstanceId());
      model.setCorrelationActivityInstanceId(
          value.getCorrelationActivityInstanceId() > 0
              ? value.getCorrelationActivityInstanceId()
              : null);
      model.setCorrelationProcessInstanceId(
          value.getCorrelationProcessInstanceId() > 0
              ? value.getCorrelationProcessInstanceId()
              : null);
      model.setProcessDefinitionId(value.getProcessDefinitionId());
      model.setDefinitionKey(value.getProcessDefinitionKey());
      model.setActivityKey(value.getActivityDefinitionKey());
      model.setState(record.getValueState().name());
      model.setResourceId(record.getResourceId());
      buffer.offer(
          RowChange.insert(MESSAGE_SUBSCRIPTION, List.of(value.getMessageSubscriptionId()), model));
      return;
    }
    final var model = new MessageSubscriptionDbModel();
    model.setMessageSubscriptionId(value.getMessageSubscriptionId());
    model.setState(record.getValueState().name());
    buffer.offer(
        RowChange.update(MESSAGE_SUBSCRIPTION, List.of(value.getMessageSubscriptionId()), model));
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
