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
package com.anyilanxin.kunpeng.bpm.parse.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBranchingGateway;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCallActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnIntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobWorkerTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnScriptTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BPMN 模型转换测试：覆盖任务装配、连线、分支网关、事件载荷、多实例重组与调用活动等主路径。
 */
class BpmnTransformTest {
  /** 基础流程：服务/脚本/用户任务、排他网关（条件+默认流）、流程级监听器。 */
  private static final String SIMPLE_BPMN = "bpmn/simple.bpmn";

  /** 进阶流程：边界事件（定时/错误）、多实例、信号捕获、消息抛出、调用活动与事件子流程。 */
  private static final String ADVANCED_BPMN = "bpmn/advanced.bpmn";

  @Test
  void shouldTransformSimpleProcess() {
    final BpmnProcess process = transformFirst(SIMPLE_BPMN);

    assertEquals("Process_simple", process.getId());
    assertTrue(process.isExecutable());
    assertEquals(BpmnElementType.PROCESS, process.getElementType());
    assertTrue(process.hasNoneStartEvent());
    final BpmnStartEvent startEvent = process.getNoneStartEvent();
    assertNotNull(startEvent);
    assertEquals(BpmnEventType.NONE, startEvent.getEventType());

    // 流程级 start/end 监听器均已装配
    assertEquals(1, process.getExecutionListeners(KunpengExecutionListenerEventType.start).size());
    assertEquals(1, process.getExecutionListeners(KunpengExecutionListenerEventType.end).size());

    // 服务任务：任务定义 + 动态扩展表达式
    final BpmnJobWorkerTask serviceTask =
        process.getElementById("Task_service", BpmnJobWorkerTask.class);
    assertNotNull(serviceTask.getJobProperties());
    assertEquals("service-job", serviceTask.getJobProperties().getType().getParsedText());
    assertEquals(1, serviceTask.getAdditions().size());
    assertTrue(serviceTask.getAdditions().containsKey("priority"));

    // 脚本任务：内联脚本与结果变量、start 监听器
    final BpmnScriptTask scriptTask =
        process.getElementById("Task_script", BpmnScriptTask.class);
    assertNotNull(scriptTask.getExpression());
    assertEquals("doubled", scriptTask.getResultVariable());
    assertEquals(
        1, scriptTask.getExecutionListeners(KunpengExecutionListenerEventType.start).size());

    // 用户任务：分配定义三项
    final BpmnUserTask userTask = process.getElementById("Task_user", BpmnUserTask.class);
    assertNotNull(userTask.getAssignee());
    assertNotNull(userTask.getCandidateGroups());
    assertNotNull(userTask.getCandidateUsers());

    // 排他网关：默认流与条件出边
    final BpmnBranchingGateway gateway =
        process.getElementById("Gateway_check", BpmnBranchingGateway.class);
    assertNotNull(gateway.getDefaultFlow());
    assertEquals("Flow_gateway_end", gateway.getDefaultFlow().getId());
    assertEquals(1, gateway.getOutgoingWithCondition().size());
    assertEquals(2, gateway.getOutgoing().size());

    // 连线双向连接
    final BpmnSequenceFlow flow =
        process.getElementById("Flow_start_service", BpmnSequenceFlow.class);
    assertSame(startEvent, flow.getSource());
    assertSame(process.getElementById("Task_service", BpmnFlowNode.class), flow.getTarget());
    final BpmnFlowNode startNode = process.getElementById("Start_1", BpmnFlowNode.class);
    final BpmnFlowNode serviceNode = process.getElementById("Task_service", BpmnFlowNode.class);
    assertTrue(startNode.getOutgoing().contains(flow));
    assertTrue(serviceNode.getIncoming().contains(flow));

    // 元素名称与文档缺失时保持 null
    assertNull(process.getElementById("Task_service", BpmnFlowNode.class).getDocumentation());
    assertEquals("服务任务", serviceTask.getName());
  }

  @Test
  void shouldTransformEventsAndMultiInstance() {
    final BpmnProcess process = transformFirst(ADVANCED_BPMN);

    // 支付任务：ioMapping 输入输出均已编译
    final BpmnJobWorkerTask payment =
        process.getElementById("Task_payment", BpmnJobWorkerTask.class);
    assertTrue(payment.hasInputMappings());
    assertTrue(payment.hasOutputMappings());
    assertTrue(payment.hasAttachedEvents());

    // 边界事件：定时（中断）与错误（非中断）
    final BpmnBoundaryEvent timeoutBoundary =
        process.getElementById("Boundary_timeout", BpmnBoundaryEvent.class);
    assertTrue(timeoutBoundary.isTimer());
    assertTrue(timeoutBoundary.isInterrupting());
    assertEquals(BpmnEventType.TIMER, timeoutBoundary.getEventType());
    assertNotNull(timeoutBoundary.getTimerProperties());
    assertEquals(
        BpmnCatchEventElement.TimerProperties.TimerType.DURATION,
        timeoutBoundary.getTimerProperties().getTimerType());
    assertEquals("PT10S", timeoutBoundary.getTimerProperties().getTimerContent());

    final BpmnBoundaryEvent errorBoundary =
        process.getElementById("Boundary_payment_error", BpmnBoundaryEvent.class);
    assertTrue(errorBoundary.isError());
    assertFalse(errorBoundary.isInterrupting());
    // QL 引擎 isStatic 恒为 false，错误码不预解析，以表达式承载（运行期求值）
    assertEquals("PAY-500", errorBoundary.getError().getErrorCodeExpression().getSourceText());

    // 多实例：注册表以活动体替换内部活动，特征与连线均已迁移
    final BpmnMultiInstanceBody multiInstanceBody =
        process.getElementById("Task_ship", BpmnMultiInstanceBody.class);
    assertEquals(BpmnElementType.MULTI_INSTANCE_BODY, multiInstanceBody.getElementType());
    assertNotNull(multiInstanceBody.getInnerActivity());
    assertFalse(multiInstanceBody.getLoopCharacteristics().isSequential());
    assertNotNull(multiInstanceBody.getLoopCharacteristics().getCompletionCondition());
    assertEquals("order", multiInstanceBody.getLoopCharacteristics().getElementVariable());
    assertEquals("Flow_payment_mi", multiInstanceBody.getIncoming().get(0).getId());
    assertEquals("Flow_ship_catch", multiInstanceBody.getOutgoing().get(0).getId());
    assertSame(multiInstanceBody, multiInstanceBody.getIncoming().get(0).getTarget());
    assertSame(multiInstanceBody, multiInstanceBody.getOutgoing().get(0).getSource());

    // 中间捕获事件（信号）与中间抛出事件（发布型消息）：名称以表达式承载，运行期求值
    final BpmnCatchEventElement signalCatch =
        process.getElementById("Catch_signal", BpmnCatchEventElement.class);
    assertTrue(signalCatch.isSignal());
    assertEquals("wait-signal", signalCatch.getSignal().getSignalNameExpression().getSourceText());
    final BpmnIntermediateThrowEvent messageThrow =
        process.getElementById("Throw_message", BpmnIntermediateThrowEvent.class);
    assertTrue(messageThrow.isMessageThrowEvent());
    assertNotNull(messageThrow.getMessagePublishProperties());
    assertEquals(
        "ship-message",
        messageThrow.getMessagePublishProperties().getMessageNameExpression().getSourceText());

    // 调用活动：被调用流程表达式与绑定属性
    final BpmnCallActivity callActivity =
        process.getElementById("Call_sub", BpmnCallActivity.class);
    assertEquals("child-process", callActivity.getCalledElementProcessId().getParsedText());
    assertTrue(callActivity.isPropagateAllParentVariables());
    assertFalse(callActivity.isPropagateAllChildVariables());
    assertEquals(0, callActivity.getLexicographicIndex());

    // 事件子流程：类型改标并挂载，开始事件回填子流程 id
    final BpmnContainer eventSubprocess =
        process.getElementById("Sub_escalation", BpmnContainer.class);
    assertEquals(BpmnElementType.EVENT_SUB_PROCESS, eventSubprocess.getElementType());
    final BpmnStartEvent escalationStart =
        process.getElementById("Start_escalation", BpmnStartEvent.class);
    assertTrue(escalationStart.isEscalation());
    assertEquals("Sub_escalation", escalationStart.getEventSubProcessId());
    assertEquals(
        "URGENT-1", escalationStart.getEscalation().getEscalationCodeExpression().getSourceText());
  }

  @Test
  void shouldReuseExpressionForIdenticalSources() {
    // 相同源文本（两个任务的 retries=3）经表达式缓存去重，仅解析一次并复用同一实例
    final List<BpmnProcess> processes = transform(ADVANCED_BPMN);
    final BpmnProcess process = processes.getFirst();
    final BpmnJobWorkerTask payment =
        process.getElementById("Task_payment", BpmnJobWorkerTask.class);
    final BpmnJobWorkerTask ship =
        process.getElementById("Task_ship", BpmnJobWorkerTask.class);
    assertSame(payment.getJobProperties().getRetries(), ship.getJobProperties().getRetries());
  }

  @Test
  void shouldValidateSimpleModelWithoutErrors() {
    final String result;
    try (final InputStream stream =
        getClass().getClassLoader().getResourceAsStream(SIMPLE_BPMN)) {
      final BpmnModelInstance modelInstance = Bpmn.readModelFromStream(stream);
      result = new BpmnValidator(32).validate(modelInstance);
    } catch (final Exception e) {
      throw new IllegalStateException("Validation failed: " + SIMPLE_BPMN, e);
    }
    assertNull(result, "Expected no validation errors: " + result);
  }

  private BpmnProcess transformFirst(final String resource) {
    return transform(resource).getFirst();
  }

  private List<BpmnProcess> transform(final String resource) {
    final BpmnTransformer transformer =
        BpmnFactory.createTransformer(BpmnFactory.createExpressionLanguage(null));
    try (final InputStream stream =
        getClass().getClassLoader().getResourceAsStream(resource)) {
      return transformer.transformDefinitions(stream.readAllBytes());
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to transform " + resource, e);
    }
  }
}
