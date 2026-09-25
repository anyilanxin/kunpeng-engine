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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;

/**
 * 用户任务 API 处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class UserTaskApiAbstractProcessor<T extends UnifiedRecordValue>
    implements LogEventProcessorSingleState<T> {
  private final ImmutableUserTaskRepository userTask;
  protected final LogEventWriter writer;
  protected final ImmutableBpmnResourceRepository bpmnResource;

  public UserTaskApiAbstractProcessor(
      final LogEventWriter writer, final ImmutableUserTaskRepository userTask) {
    this.userTask = userTask;
    final ImmutableBusinessRepository repository = writer.getRepository();
    this.writer = writer;
    bpmnResource = repository.bpmnResourceRepository();
  }

  @Override
  public ValueType valueType() {
    return ValueType.USER_TASK_API;
  }

  @Override
  public abstract CommandApiUserTaskValueLifeCycle valueLifeCycle();

  @Override
  public void processRecord(final BusinessLogRecord<T> record) {
    final long key = record.getKey();
    if (key > 0) {
      final UserTaskRecord userTaskRecord = userTask.getRecord(key);
      if (userTaskRecord == null) {
        writer.adErrorResponse(record.getRequestId(), -1, "用户任务不存在");
        return;
      }
      innerProcessRecord(record, userTaskRecord);
    } else {
      innerProcessRecord(record, null);
    }
  }

  public abstract void innerProcessRecord(
      BusinessLogRecord<T> record, final UserTaskRecord userTask);
}
