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

import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/** 中间抛出事件的运行时模型：支持无操作、消息抛出（任务型或发布型）、链接、升级、信号与补偿抛出。 */
public class BpmnIntermediateThrowEvent extends BpmnFlowNode {
  /** 任务属性（任务型消息抛出），未声明为 null */
  private BpmnJobProperties jobProperties;

  /** 消息发布属性（发布型消息抛出），未声明为 null */
  private MessagePublishProperties messagePublishProperties;

  /** 链接载荷，非链接抛出为 null */
  private BpmnLink link;

  /** 升级载荷，非升级抛出为 null */
  private BpmnEscalation escalation;

  /** 信号载荷，非信号抛出为 null */
  private BpmnSignal signal;

  /** 补偿载荷，非补偿抛出为 null */
  private BpmnCompensation compensation;

  /**
   * 以元素 id 构造中间抛出事件。
   *
   * @param id 元素唯一标识
   */
  public BpmnIntermediateThrowEvent(final String id) {
    super(id);
  }

  /** 是否为无操作抛出事件。 */
  public boolean isNoneThrowEvent() {
    return !isMessageThrowEvent()
        && !isLinkThrowEvent()
        && !isEscalationThrowEvent()
        && !isSignalThrowEvent()
        && !isCompensationEvent();
  }

  /** 是否为消息抛出事件（任务型或发布型）。 */
  public boolean isMessageThrowEvent() {
    return jobProperties != null || messagePublishProperties != null;
  }

  /** 是否为链接抛出事件。 */
  public boolean isLinkThrowEvent() {
    return link != null;
  }

  /** 是否为升级抛出事件。 */
  public boolean isEscalationThrowEvent() {
    return escalation != null;
  }

  /** 是否为信号抛出事件。 */
  public boolean isSignalThrowEvent() {
    return signal != null;
  }

  /** 是否为补偿抛出事件。 */
  public boolean isCompensationEvent() {
    return getEventType() == BpmnEventType.COMPENSATION;
  }

  /** 获取任务属性，未声明时返回 null。 */
  public BpmnJobProperties getJobProperties() {
    return jobProperties;
  }

  /**
   * 设置任务属性。
   *
   * @param jobProperties 任务属性
   */
  public void setJobProperties(final BpmnJobProperties jobProperties) {
    this.jobProperties = jobProperties;
  }

  /** 获取消息发布属性，未声明时返回 null。 */
  public MessagePublishProperties getMessagePublishProperties() {
    return messagePublishProperties;
  }

  /**
   * 设置消息发布属性。
   *
   * @param messagePublishProperties 消息发布属性
   */
  public void setMessagePublishProperties(final MessagePublishProperties messagePublishProperties) {
    this.messagePublishProperties = messagePublishProperties;
  }

  /** 获取链接载荷，非链接抛出返回 null。 */
  public BpmnLink getLink() {
    return link;
  }

  /**
   * 设置链接载荷。
   *
   * @param link 链接载荷
   */
  public void setLink(final BpmnLink link) {
    this.link = link;
  }

  /** 获取升级载荷，非升级抛出返回 null。 */
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

  /** 获取信号载荷，非信号抛出返回 null。 */
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

  /** 获取补偿载荷，非补偿抛出返回 null。 */
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

  /** 发布型消息抛出属性：消息名、关联键与存活时长的表达式。 */
  public static final class MessagePublishProperties {
    /** 消息名表达式 */
    private ScriptExpression messageNameExpression;

    /** 已解析的静态消息名（消息名表达式与变量上下文无关时提前求值），未解析为 null */
    private String messageName;

    /** 关联键表达式 */
    private ScriptExpression correlationKeyExpression;

    /** 消息存活时长表达式，未声明为 null */
    private ScriptExpression timeToLiveExpression;

    /** 获取消息名表达式。 */
    public ScriptExpression getMessageNameExpression() {
      return messageNameExpression;
    }

    /**
     * 设置消息名表达式。
     *
     * @param messageNameExpression 消息名表达式
     */
    public void setMessageNameExpression(final ScriptExpression messageNameExpression) {
      this.messageNameExpression = messageNameExpression;
    }

    /** 获取静态消息名，未解析时返回 null。 */
    public String getMessageName() {
      return messageName;
    }

    /**
     * 设置静态消息名。
     *
     * @param messageName 静态消息名
     */
    public void setMessageName(final String messageName) {
      this.messageName = messageName;
    }

    /** 获取关联键表达式。 */
    public ScriptExpression getCorrelationKeyExpression() {
      return correlationKeyExpression;
    }

    /**
     * 设置关联键表达式。
     *
     * @param correlationKeyExpression 关联键表达式
     */
    public void setCorrelationKeyExpression(final ScriptExpression correlationKeyExpression) {
      this.correlationKeyExpression = correlationKeyExpression;
    }

    /** 获取消息存活时长表达式，未声明时返回 null。 */
    public ScriptExpression getTimeToLiveExpression() {
      return timeToLiveExpression;
    }

    /**
     * 设置消息存活时长表达式。
     *
     * @param timeToLiveExpression 存活时长表达式
     */
    public void setTimeToLiveExpression(final ScriptExpression timeToLiveExpression) {
      this.timeToLiveExpression = timeToLiveExpression;
    }
  }
}
