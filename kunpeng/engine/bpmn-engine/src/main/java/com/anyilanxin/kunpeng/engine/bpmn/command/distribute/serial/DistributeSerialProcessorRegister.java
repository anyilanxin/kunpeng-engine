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
package com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.processor.DistributeSerialAckProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.processor.DistributeSerialCompleteProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.processor.DistributeSerialCreateProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.processor.DistributeSerialDistributeStartProcessor;

/**
 * 串行分发处理器注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DistributeSerialProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    processors
        .onCommand(new DistributeSerialAckProcessor(writer))
        .onCommand(new DistributeSerialCompleteProcessor(writer))
        .onCommand(new DistributeSerialCreateProcessor(writer))
        .onCommand(new DistributeSerialDistributeStartProcessor(writer));
  }
}
