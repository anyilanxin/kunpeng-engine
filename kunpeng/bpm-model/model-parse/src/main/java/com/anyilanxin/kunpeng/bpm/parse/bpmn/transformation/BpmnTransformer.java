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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SendTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.traversal.ModelWalker;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCallActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.BoundaryEventTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.BusinessRuleTaskTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.CallActivityTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.CatchEventTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.CurrentProcessSwitcher;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.EndEventTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.ErrorTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.EscalationTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.ExclusiveGatewayTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.FlowElementCreationTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.FlowNodeTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.IntermediateCatchEventTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.IntermediateThrowEventTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.JobWorkerTaskTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.MessageTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.MultiInstanceActivityTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.ProcessTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.ScriptTaskTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.SequenceFlowTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.SignalTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.StartEventTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.SubProcessTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer.UserTaskTransformer;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * BPMN 模型转换入口：将 BPMN XML 模型分五个阶段（每阶段一次全量深度优先遍历）转换为可执行流程集合。
 *
 * <p>阶段顺序：① 实例化全部元素（流程/消息/信号/错误/升级与流程元素骨架）→ ② 转换属性并交叉链接（连线、事件定义、任务属性） → ③ 依据上下文调整（排他网关默认流、子流程挂载）→ ④
 * 依据容器调整（中间抛出事件）→ ⑤ 依据容器元素调整（多实例活动体重组）。
 *
 * <p>五个阶段共用同一个 {@link ModelWalker}，每个阶段各持一组转换器；调用活动的字典序索引在全部遍历完成后统一计算一次（避免逐元素全量排序）。
 */
public final class BpmnTransformer {
  /** 阶段一：实例化流程与全局事件载荷 */
  private final ElementTransformVisitor step1Visitor;

  /** 阶段二：转换属性、连接元素 */
  private final ElementTransformVisitor step2Visitor;

  /** 阶段三：依据元素上下文调整 */
  private final ElementTransformVisitor step3Visitor;

  /** 阶段四：依据容器元素调整 */
  private final ElementTransformVisitor step4Visitor;

  /** 阶段五：依据容器元素重组（多实例） */
  private final ElementTransformVisitor step5Visitor;

  /** 表达式编译引擎（把 BPMN 表达式编译为可执行脚本） */
  private final ScriptEngine expressionLanguage;

  /**
   * 构造转换器，并按依赖顺序为五个阶段各注册一组元素转换器。
   *
   * @param expressionLanguage 用于编译 BPMN 表达式的脚本引擎
   */
  public BpmnTransformer(final ScriptEngine expressionLanguage) {
    this.expressionLanguage = expressionLanguage;

    step1Visitor = new ElementTransformVisitor();
    step1Visitor.registerHandler(new ErrorTransformer());
    step1Visitor.registerHandler(new EscalationTransformer());
    step1Visitor.registerHandler(new FlowElementCreationTransformer());
    step1Visitor.registerHandler(new MessageTransformer());
    step1Visitor.registerHandler(new SignalTransformer());
    step1Visitor.registerHandler(new ProcessTransformer());

    step2Visitor = new ElementTransformVisitor();
    step2Visitor.registerHandler(new BoundaryEventTransformer());
    step2Visitor.registerHandler(new BusinessRuleTaskTransformer());
    step2Visitor.registerHandler(new CallActivityTransformer());
    step2Visitor.registerHandler(new CatchEventTransformer());
    step2Visitor.registerHandler(new CurrentProcessSwitcher());
    step2Visitor.registerHandler(new EndEventTransformer());
    step2Visitor.registerHandler(new FlowNodeTransformer());
    step2Visitor.registerHandler(new JobWorkerTaskTransformer<>(ServiceTask.class));
    step2Visitor.registerHandler(new JobWorkerTaskTransformer<>(SendTask.class));
    step2Visitor.registerHandler(new SequenceFlowTransformer());
    step2Visitor.registerHandler(new StartEventTransformer());
    step2Visitor.registerHandler(new UserTaskTransformer());
    step2Visitor.registerHandler(new ScriptTaskTransformer());

    step3Visitor = new ElementTransformVisitor();
    step3Visitor.registerHandler(new CurrentProcessSwitcher());
    step3Visitor.registerHandler(new ExclusiveGatewayTransformer());
    step3Visitor.registerHandler(new IntermediateCatchEventTransformer());
    step3Visitor.registerHandler(new SubProcessTransformer());

    step4Visitor = new ElementTransformVisitor();
    step4Visitor.registerHandler(new CurrentProcessSwitcher());
    step4Visitor.registerHandler(new IntermediateThrowEventTransformer());

    step5Visitor = new ElementTransformVisitor();
    step5Visitor.registerHandler(new CurrentProcessSwitcher());
    step5Visitor.registerHandler(new MultiInstanceActivityTransformer());
  }

  /**
   * 从 BPMN XML 字节流读取模型并执行转换。
   *
   * @param bytes BPMN XML 字节流
   * @return 转换得到的可执行流程集合
   */
  public List<BpmnProcess> transformDefinitions(final byte[] bytes) {
    final BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromBytes(bytes);
    return transformDefinitions(bpmnModelInstance);
  }

  /**
   * 分五个阶段遍历模型完成转换，为全部调用活动统一编号字典序索引后返回流程集合。
   *
   * @param modelInstance 已解析的 BPMN 模型实例
   * @return 转换得到的可执行流程集合
   */
  public List<BpmnProcess> transformDefinitions(final BpmnModelInstance modelInstance) {
    final BpmnTransformContext context = new BpmnTransformContext();
    context.setExpressionLanguage(expressionLanguage);

    final ModelWalker walker = new ModelWalker(modelInstance);
    walk(walker, step1Visitor, context);
    walk(walker, step2Visitor, context);
    walk(walker, step3Visitor, context);
    walk(walker, step4Visitor, context);
    walk(walker, step5Visitor, context);

    final List<BpmnProcess> processes = context.getProcesses();
    assignCallActivityIndices(processes);
    return processes;
  }

  /** 以指定上下文执行一次全量遍历。 */
  private void walk(
      final ModelWalker walker,
      final ElementTransformVisitor visitor,
      final BpmnTransformContext context) {
    visitor.setContext(context);
    walker.walk(visitor);
  }

  /**
   * 为同一部署资源内全部调用活动的 id 按字典序统一编号。
   *
   * <p>一次性收集排序（O(n log n)），替代逐活动全量重算。
   *
   * @param processes 全部流程
   */
  private void assignCallActivityIndices(final List<BpmnProcess> processes) {
    final List<BpmnCallActivity> callActivities = new ArrayList<>(8);
    final List<String> ids = new ArrayList<>(8);
    for (final BpmnProcess process : processes) {
      for (final BpmnFlowElement element : process.getFlowElements()) {
        if (element instanceof final BpmnCallActivity callActivity) {
          callActivities.add(callActivity);
          ids.add(callActivity.getId());
        }
      }
    }
    if (callActivities.isEmpty()) {
      return;
    }
    Collections.sort(ids);
    for (final BpmnCallActivity callActivity : callActivities) {
      callActivity.setLexicographicIndex(
          Collections.binarySearch(ids, callActivity.getId(), Comparator.naturalOrder()));
    }
  }
}
