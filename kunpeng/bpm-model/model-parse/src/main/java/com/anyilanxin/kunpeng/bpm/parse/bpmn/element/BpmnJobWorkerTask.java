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

/**
 * 任务型活动（服务任务、发送任务等）的运行时基类：携带任务类型与重试次数构成的任务属性。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnJobWorkerTask extends BpmnActivity {
  /** 任务属性，未声明任务定义时为 null */
  private BpmnJobProperties jobProperties;

  /**
   * 以元素 id 构造任务型活动。
   *
   * @param id 元素唯一标识
   */
  public BpmnJobWorkerTask(final String id) {
    super(id);
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
}
