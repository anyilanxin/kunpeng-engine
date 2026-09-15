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

/** 事件相关流程元素的事件语义类型（消息、定时、错误等）。 */
public enum BpmnEventType {
  /** 未指定（初始态） */
  UNSPECIFIED,
  /** 无事件定义的普通事件 */
  NONE,
  /** 消息事件 */
  MESSAGE,
  /** 定时事件 */
  TIMER,
  /** 错误事件 */
  ERROR,
  /** 升级事件 */
  ESCALATION,
  /** 信号事件 */
  SIGNAL,
  /** 链接事件 */
  LINK,
  /** 补偿事件 */
  COMPENSATION
}
