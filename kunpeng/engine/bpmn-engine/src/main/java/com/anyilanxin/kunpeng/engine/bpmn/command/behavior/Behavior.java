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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior;

import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;

/**
 * BPMN 元素行为接口：流程元素执行语义的抽象。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface Behavior {

  SequenceFlowBehavior sequenceFlowBehavior();

  ActivityInstanceBehavior activityInstanceBehavior();

  ProcessDefinitionBehavior processDefinitionBehavior();

  VariableBehavior variableBehavior();

  DelayBehavior delayBehavior();

  DistributeParallelBehavior distributeParallelBehavior();

  DistributeSerialBehavior distributeSerialBehavior();

  BpmnJobDeliveryBehavior activationBehavior();

  IncidentBehavior incidentBehavior();

  InputOutputBehavior inputOutputBehavior();

  UserTaskBehavior userTaskBehavior();

  JobBehavior jobBehavior();

  BatchBehavior batchBehavior();

  CatchEventBehavior catchEvent();

  HistoryCleanupBehavior historyCleanup();
}
