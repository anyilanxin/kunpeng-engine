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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.update.UserTaskUpdateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;

/**
 * 用户任务更新 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskUpdateProcessor
    extends UserTaskApiAbstractProcessor<UserTaskUpdateRequestRecord> {
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final int sourceId;
  private final VariableBehavior variableBehavior;

  public UserTaskUpdateProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    variableBehavior = writer.behavior().variableBehavior();
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<UserTaskUpdateRequestRecord> record, final UserTaskRecord userTask) {}

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.UPDATE_REQUEST;
  }
}
