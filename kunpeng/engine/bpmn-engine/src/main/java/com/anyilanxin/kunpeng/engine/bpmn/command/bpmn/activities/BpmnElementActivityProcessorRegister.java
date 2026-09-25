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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;

/**
 * BPMN 元素活动处理器注册器：登记全部元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnElementActivityProcessorRegister {

  private BpmnElementActivityProcessorRegister() {}

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    final BpmnElementProcessors elementProcessors = new BpmnElementProcessors(writer);
    processors
        .onCommand(new BpmnActivityElementProcessors(writer, elementProcessors))
        .onCommand(new BpmnListenerProcessor(writer, elementProcessors));
  }
}
