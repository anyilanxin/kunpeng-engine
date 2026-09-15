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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnError;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEscalation;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnLink;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMessage;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSignal;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BPMN 转换上下文：承载转换过程中的全部可寻址状态——流程注册表、事件载荷注册表、当前流程、表达式引擎与表达式缓存。
 *
 * <p>表达式缓存按源文本去重（{@link ScriptEngine#parse(String)} 每次调用都会重新解析）， 同一模型内重复出现的表达式（如统一的
 * retries、相同的映射源）只解析一次。
 */
public final class BpmnTransformContext {
  /** 已转换流程注册表（流程 id -> 流程） */
  private final Map<String, BpmnProcess> processes = new HashMap<>(4);

  /** 已转换消息注册表（消息 id -> 消息） */
  private final Map<String, BpmnMessage> messages = new HashMap<>(8);

  /** 已转换信号注册表（信号 id -> 信号） */
  private final Map<String, BpmnSignal> signals = new HashMap<>(8);

  /** 已转换错误注册表（错误 id -> 错误） */
  private final Map<String, BpmnError> errors = new HashMap<>(8);

  /** 已转换升级注册表（升级 id -> 升级） */
  private final Map<String, BpmnEscalation> escalations = new HashMap<>(8);

  /** 已转换链接注册表（链接名 -> 链接） */
  private final Map<String, BpmnLink> links = new HashMap<>(8);

  /** 表达式缓存（源文本 -> 已解析表达式） */
  private final Map<String, ScriptExpression> expressionCache = new HashMap<>(32);

  /** 当前正在转换的流程（由 Process/ContextProcess 转换器维护） */
  private BpmnProcess currentProcess;

  /** 表达式编译引擎 */
  private ScriptEngine expressionLanguage;

  /** 获取当前正在转换的流程。 */
  public BpmnProcess getCurrentProcess() {
    return currentProcess;
  }

  /**
   * 设置当前正在转换的流程。
   *
   * @param currentProcess 当前流程
   */
  public void setCurrentProcess(final BpmnProcess currentProcess) {
    this.currentProcess = currentProcess;
  }

  /**
   * 注册已转换的流程。
   *
   * @param process 已转换的流程
   */
  public void addProcess(final BpmnProcess process) {
    processes.put(process.getId(), process);
  }

  /**
   * 按流程 id 获取已转换的流程。
   *
   * @param id 流程 id
   * @return 对应流程，不存在时返回 null
   */
  public BpmnProcess getProcess(final String id) {
    return processes.get(id);
  }

  /** 获取全部已转换的流程。 */
  public List<BpmnProcess> getProcesses() {
    return new ArrayList<>(processes.values());
  }

  /**
   * 注册已转换的消息。
   *
   * @param message 已转换的消息
   */
  public void addMessage(final BpmnMessage message) {
    messages.put(message.getId(), message);
  }

  /**
   * 按消息 id 获取已转换的消息。
   *
   * @param id 消息 id
   * @return 对应消息，不存在时返回 null
   */
  public BpmnMessage getMessage(final String id) {
    return messages.get(id);
  }

  /**
   * 注册已转换的信号。
   *
   * @param signal 已转换的信号
   */
  public void addSignal(final BpmnSignal signal) {
    signals.put(signal.getId(), signal);
  }

  /**
   * 按信号 id 获取已转换的信号。
   *
   * @param id 信号 id
   * @return 对应信号，不存在时返回 null
   */
  public BpmnSignal getSignal(final String id) {
    return signals.get(id);
  }

  /**
   * 注册已转换的错误。
   *
   * @param error 已转换的错误
   */
  public void addError(final BpmnError error) {
    errors.put(error.getId(), error);
  }

  /**
   * 按错误 id 获取已转换的错误。
   *
   * @param id 错误 id
   * @return 对应错误，不存在时返回 null
   */
  public BpmnError getError(final String id) {
    return errors.get(id);
  }

  /**
   * 注册已转换的升级。
   *
   * @param escalation 已转换的升级
   */
  public void addEscalation(final BpmnEscalation escalation) {
    escalations.put(escalation.getId(), escalation);
  }

  /**
   * 按升级 id 获取已转换的升级。
   *
   * @param id 升级 id
   * @return 对应升级，不存在时返回 null
   */
  public BpmnEscalation getEscalation(final String id) {
    return escalations.get(id);
  }

  /**
   * 注册已转换的链接（以链接名为键，供链接抛出事件按名反查捕获事件）。
   *
   * @param link 已转换的链接
   */
  public void addLink(final BpmnLink link) {
    links.put(link.getName(), link);
  }

  /**
   * 按链接名获取已转换的链接。
   *
   * @param name 链接名
   * @return 对应链接，不存在时返回 null
   */
  public BpmnLink getLink(final String name) {
    return links.get(name);
  }

  /** 获取表达式编译引擎。 */
  public ScriptEngine getExpressionLanguage() {
    return expressionLanguage;
  }

  /**
   * 设置表达式编译引擎。
   *
   * @param expressionLanguage 表达式编译引擎
   */
  public void setExpressionLanguage(final ScriptEngine expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  /**
   * 解析表达式（带缓存）：同一源文本在本次转换内只解析一次。
   *
   * @param source 表达式源文本
   * @return 已解析的表达式
   */
  public ScriptExpression parseExpression(final String source) {
    return expressionCache.computeIfAbsent(source, expressionLanguage::parse);
  }
}
