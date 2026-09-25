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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.adhocsubprocess;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.CatchEventBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.InputOutputBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.BpmnContainerActivityElementProcessor;

/**
 * 临时子流程元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AdHocSubProcessElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnAdHocSubProcess> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final VariableBehavior variableBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final InputOutputBehavior inputOutputBehavior;

  public AdHocSubProcessElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
    catchEventBehavior = behavior.catchEvent();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.AD_HOC_SUB_PROCESS;
  }

  @Override
  public Class<BpmnAdHocSubProcess> getType() {
    return BpmnAdHocSubProcess.class;
  }

  @Override
  public void onActivating(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void onActivated(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void onCompleting(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void onCompleted(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void onTerminating(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void onTerminated(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    throw new RuntimeException("not implemented");
  }
}
