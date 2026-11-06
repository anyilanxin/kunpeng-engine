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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.multiinstance;

import com.anyilanxin.kunpeng.protocol.business.EngineReservedVariables;

/**
 * 多实例常量定义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MultiInstanceConstant {
  String NR_OF_ACTIVE_INSTANCES = "nrOfActiveInstances";
  String NR_OF_INSTANCES = "nrOfInstances";
  String NR_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";
  String NR_OF_TERMINATED_INSTANCES = "nrOfTerminatedInstances";
  String LOOP_COUNTER = "loopCounter";

  /** 引擎保留前缀：body 作用域的集合快照变量（求值一次，创建窗口内子实例按游标取元素，客户端不可写/删；契约单源 {@link EngineReservedVariables}） */
  String INPUT_COLLECTION = EngineReservedVariables.MI_INPUT_COLLECTION;
}
