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

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor.*;

/**
 * 用户任务 API 处理器注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class UserTaskApiProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    processors
        .onCommand(new UserTaskAssigneeProcessor(writer))
        .onCommand(new UserTaskClaimProcessor(writer))
        .onCommand(new UserTaskCompleteProcessor(writer))
        .onCommand(new UserTaskCreateProcessor(writer))
        .onCommand(new UserTaskDelegateProcessor(writer))
        .onCommand(new UserTaskDeleteProcessor(writer))
        .onCommand(new UserTaskCancelProcessor(writer))
        .onCommand(new UserTaskOwnProcessor(writer))
        .onCommand(new UserTaskUpdateProcessor(writer));
  }
}
