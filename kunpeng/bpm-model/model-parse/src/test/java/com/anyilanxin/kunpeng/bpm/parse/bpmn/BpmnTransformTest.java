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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBranchingGateway;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCallActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.common.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnIntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobWorkerTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;
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
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class BpmnTransformTest {
  /** 基础流程：服务/脚本/用户任务、排他网关（条件+默认流）、流程级监听器。 */
  private static final String SIMPLE_BPMN = "bpmn/simple.bpmn";

  /** 进阶流程：边界事件（定时/错误）、多实例、信号捕获、消息抛出、调用活动与事件子流程。 */
  private static final String ADVANCED_BPMN = "bpmn/advanced.bpmn";

  /** 多实例集合（模型器产出形态）：kunpeng:loopCharacteristics 在 MI 元素扩展内、静态列表、无基数。 */
  private static final String MI_COLLECTION_BPMN = "bpmn/multi-instance-collection.bpmn";

  /** 加签流程：一个声明 kunpeng:addSign 的用户任务 + 一个未声明的用户任务。 */
  private static final String ADD_SIGN_BPMN = "bpmn/addsign.bpmn";

  /** 加签级联流程：cascade=true + maxDepth=2 的合法建模。 */
  private static final String ADD_SIGN_CASCADE_BPMN = "bpmn/addsign-cascade.bpmn";

  /** 非法加签建模：maxDepth=0（必须 >=1）。 */
  private static final String ADD_SIGN_INVALID_DEPTH_BPMN = "bpmn/addsign-invalid-depth.bpmn";

  /** 矛盾加签建模：未开启 cascade 却声明 maxDepth=3。 */

  /** 存量兼容：老 XML 残留 kunpeng:userTask 标记元素（模型已拔除）。 */
  private static final String ADD_SIGN_LEGACY_MARKER_BPMN = "bpmn/addsign-legacy-marker.bpmn";
  private static final String ADD_SIGN_CONFLICT_BPMN = "bpmn/addsign-conflict.bpmn";

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
    // 集合表达式从 MI 元素自身的扩展元素读取（与模型器/部署期校验同位置），活动层不再读取
    assertEquals("orders", multiInstanceBody.getLoopCharacteristics().getCollection().getSourceText());
    assertNotNull(multiInstanceBody.getLoopCharacteristics().getLoopCardinality());
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
  void shouldReadCollectionFromMultiInstanceElementExtension() {
    // 回归（并行 MI 空展开事故）：模型器把 kunpeng:loopCharacteristics 写在 MI 元素扩展内，
    // 转换器曾从活动层扩展读取导致 collection=null，运行期按"集合与基数均未声明"空展开完结
    final BpmnProcess process = transformFirst(MI_COLLECTION_BPMN);
    final BpmnMultiInstanceBody body =
        process.getElementById("Task_p38m2yr", BpmnMultiInstanceBody.class);
    assertFalse(body.getLoopCharacteristics().isSequential());
    assertEquals(
        "[1,2,3,4]", body.getLoopCharacteristics().getCollection().getSourceText());
    assertEquals("assignee", body.getLoopCharacteristics().getElementVariable());
    assertNull(body.getLoopCharacteristics().getLoopCardinality());
    assertNotNull(body.getLoopCharacteristics().getCompletionCondition());
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

  @Test
  void shouldSynthesizeAddSignVirtualElementOnlyForAllowedUserTask() {
    final BpmnProcess process = transformFirst(ADD_SIGN_BPMN);
    // 声明 kunpeng:addSign 的任务：标记允许且解析期派生 id + "-addsign" 虚拟用户任务
    final BpmnUserTask allowed = process.getElementById("Task_allowed", BpmnUserTask.class);
    assertTrue(allowed.isAddSignEnabled());
    final BpmnUserTask virtual =
        process.getElementById("Task_allowed-addsign", BpmnUserTask.class);
    assertTrue(virtual.isAddSignVirtual());
    assertFalse(virtual.isAddSignEnabled());
    // 未声明的任务：不允许加签、无虚拟节点（按类型取应抛不存在）
    final BpmnUserTask denied = process.getElementById("Task_denied", BpmnUserTask.class);
    assertFalse(denied.isAddSignEnabled());
    assertFalse(denied.isAddSignVirtual());
    assertThrows(
        BpmnParseException.class,
        () -> process.getElementById("Task_denied-addsign", BpmnUserTask.class));
  }

  @Test
  void shouldTolerateLegacyUserTaskMarkerElement() {
    // kunpeng:userTask 标记元素已从模型拔除（引擎只有一种用户任务）：存量 XML 残留该元素解析不报错、不产生任何运行时语义
    final BpmnProcess process = transformFirst(ADD_SIGN_LEGACY_MARKER_BPMN);
    final BpmnUserTask task = process.getElementById("Task_allowed", BpmnUserTask.class);
    assertTrue(task.isAddSignEnabled());
    assertFalse(task.isAddSignCascade());
    assertEquals(1, task.getAddSignMaxDepth());
  }

  @Test
  void shouldReadCascadeAddSignAttributes() {
    // cascade=true + maxDepth=2：合法建模，级联开关与层数上限都落到运行时元素
    final BpmnProcess process = transformFirst(ADD_SIGN_CASCADE_BPMN);
    final BpmnUserTask task = process.getElementById("Task_1", BpmnUserTask.class);
    assertTrue(task.isAddSignEnabled());
    assertTrue(task.isAddSignCascade());
    assertEquals(2, task.getAddSignMaxDepth());
  }

  @Test
  void shouldRejectAddSignDepthBelowOne() {
    // 解析期校验：maxDepth=0 非法（必须 >=1）
    final IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> transformFirst(ADD_SIGN_INVALID_DEPTH_BPMN));
    assertInstanceOf(BpmnParseException.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("maxDepth must be >= 1"));
  }

  @Test
  void shouldRejectAddSignDepthWithoutCascade() {
    // 解析期校验：未开启 cascade 时 maxDepth 只能为 1（不允许级联即只能一级加签）
    final IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> transformFirst(ADD_SIGN_CONFLICT_BPMN));
    assertInstanceOf(BpmnParseException.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("cascade is disabled"));
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
