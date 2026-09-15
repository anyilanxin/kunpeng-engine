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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.element;

import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.Timer;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.function.Function;

/**
 * 捕获事件元素的运行时模型（中间捕获事件、开始事件、边界事件的共同基类）。
 *
 * <p>依据事件定义装配一种事件载荷：消息、定时、错误、升级、信号、链接或补偿；由 {@code isXxx()} 系列方法判定事件语义。
 */
public class BpmnCatchEventElement extends BpmnFlowNode {
  /** 消息载荷，非消息事件为 null */
  private BpmnMessage message;

  /** 定时载荷，非定时事件为 null */
  private TimerProperties timerProperties;

  /** 错误载荷，非错误事件为 null */
  private BpmnError error;

  /** 升级载荷，非升级事件为 null */
  private BpmnEscalation escalation;

  /** 信号载荷，非信号事件为 null */
  private BpmnSignal signal;

  /** 补偿载荷，非补偿事件为 null */
  private BpmnCompensation compensation;

  /** 是否为链接事件 */
  private boolean link;

  /** 是否中断其所在范围（边界事件/事件子流程开始事件语义） */
  private boolean interrupting = true;

  /** 是否与事件网关直接相连（事件网关后继事件在网关分支激活后才可触发） */
  private boolean connectedToEventBasedGateway;

  /**
   * 以元素 id 构造捕获事件元素。
   *
   * @param id 元素唯一标识
   */
  public BpmnCatchEventElement(final String id) {
    super(id);
  }

  /** 是否为定时事件。 */
  public boolean isTimer() {
    return timerProperties != null;
  }

  /** 是否为消息事件。 */
  public boolean isMessage() {
    return message != null;
  }

  /** 是否为错误事件。 */
  public boolean isError() {
    return error != null;
  }

  /** 是否为升级事件。 */
  public boolean isEscalation() {
    return escalation != null;
  }

  /** 是否为链接事件。 */
  public boolean isLink() {
    return link;
  }

  /** 是否为信号事件。 */
  public boolean isSignal() {
    return signal != null;
  }

  /** 是否为补偿事件。 */
  public boolean isCompensation() {
    return compensation != null;
  }

  /** 是否为无事件定义的普通事件。 */
  public boolean isNone() {
    return !isTimer()
        && !isMessage()
        && !isError()
        && !isLink()
        && !isEscalation()
        && !isSignal()
        && !isCompensation();
  }

  /** 获取消息载荷，非消息事件返回 null。 */
  public BpmnMessage getMessage() {
    return message;
  }

  /**
   * 设置消息载荷。
   *
   * @param message 消息载荷
   */
  public void setMessage(final BpmnMessage message) {
    this.message = message;
  }

  /** 获取定时载荷，非定时事件返回 null。 */
  public TimerProperties getTimerProperties() {
    return timerProperties;
  }

  /**
   * 设置定时载荷。
   *
   * @param timerProperties 定时载荷
   */
  public void setTimerProperties(final TimerProperties timerProperties) {
    this.timerProperties = timerProperties;
  }

  /** 获取错误载荷，非错误事件返回 null。 */
  public BpmnError getError() {
    return error;
  }

  /**
   * 设置错误载荷。
   *
   * @param error 错误载荷
   */
  public void setError(final BpmnError error) {
    this.error = error;
  }

  /** 获取升级载荷，非升级事件返回 null。 */
  public BpmnEscalation getEscalation() {
    return escalation;
  }

  /**
   * 设置升级载荷。
   *
   * @param escalation 升级载荷
   */
  public void setEscalation(final BpmnEscalation escalation) {
    this.escalation = escalation;
  }

  /** 获取信号载荷，非信号事件返回 null。 */
  public BpmnSignal getSignal() {
    return signal;
  }

  /**
   * 设置信号载荷。
   *
   * @param signal 信号载荷
   */
  public void setSignal(final BpmnSignal signal) {
    this.signal = signal;
  }

  /** 获取补偿载荷，非补偿事件返回 null。 */
  public BpmnCompensation getCompensation() {
    return compensation;
  }

  /**
   * 设置补偿载荷。
   *
   * @param compensation 补偿载荷
   */
  public void setCompensation(final BpmnCompensation compensation) {
    this.compensation = compensation;
  }

  /** 是否中断其所在范围。 */
  public boolean isInterrupting() {
    return interrupting;
  }

  /**
   * 设置是否中断其所在范围。
   *
   * @param interrupting 是否中断
   */
  public void setInterrupting(final boolean interrupting) {
    this.interrupting = interrupting;
  }

  /** 是否与事件网关直接相连。 */
  public boolean isConnectedToEventBasedGateway() {
    return connectedToEventBasedGateway;
  }

  /**
   * 标记是否与事件网关直接相连。
   *
   * @param connectedToEventBasedGateway 是否相连
   */
  public void setConnectedToEventBasedGateway(final boolean connectedToEventBasedGateway) {
    this.connectedToEventBasedGateway = connectedToEventBasedGateway;
  }

  /**
   * 标记为链接事件。
   *
   * @param link 是否链接事件
   */
  public void setLink(final boolean link) {
    this.link = link;
  }

  /** 定时载荷：定时类型、原始表达式文本与延迟到运行期的定时构建函数。 */
  public static final class TimerProperties {
    /** 定时语义类型 */
    public enum TimerType {
      /** 时间点（timeDate） */
      DATE,
      /** 时长（timeDuration） */
      DURATION,
      /** 周期（timeCycle，ISO 重复间隔或 cron） */
      CYCLE
    }

    /** 定时类型 */
    private TimerType timerType;

    /** 原始定时表达式文本（用于日志与回显） */
    private String timerContent;

    /** 依据变量上下文构建定时器的工厂（解析期编译、运行期求值） */
    private Function<ScriptContext, Either<String, Timer>> timerFactory;

    /** 获取定时类型。 */
    public TimerType getTimerType() {
      return timerType;
    }

    /**
     * 设置定时类型。
     *
     * @param timerType 定时类型
     * @return 自身（链式设置）
     */
    public TimerProperties setTimerType(final TimerType timerType) {
      this.timerType = timerType;
      return this;
    }

    /** 获取原始定时表达式文本。 */
    public String getTimerContent() {
      return timerContent;
    }

    /**
     * 设置原始定时表达式文本。
     *
     * @param timerContent 定时表达式文本
     * @return 自身（链式设置）
     */
    public TimerProperties setTimerContent(final String timerContent) {
      this.timerContent = timerContent;
      return this;
    }

    /** 获取定时构建工厂。 */
    public Function<ScriptContext, Either<String, Timer>> getTimerFactory() {
      return timerFactory;
    }

    /**
     * 设置定时构建工厂。
     *
     * @param timerFactory 定时构建工厂
     * @return 自身（链式设置）
     */
    public TimerProperties setTimerFactory(
        final Function<ScriptContext, Either<String, Timer>> timerFactory) {
      this.timerFactory = timerFactory;
      return this;
    }
  }
}
