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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobProperties;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobWorkerTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 任务型活动转换器（服务任务、发送任务）：装配任务定义扩展（任务类型与重试次数）。
 *
 * <p>任务定义的解析逻辑抽为静态方法，供脚本任务、业务规则任务与任务型中间抛出事件复用。
 *
 * @param <T> 目标模型元素类型
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobWorkerTaskTransformer<T extends FlowElement>
    implements ElementTransformer<T> {

  /** 本实例处理的目标模型元素类型 */
  private final Class<T> type;

  /**
   * 以目标元素类型构造转换器。
   *
   * @param type 目标模型元素类型
   */
  public JobWorkerTaskTransformer(final Class<T> type) {
    this.type = type;
  }

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<T> getType() {
    return type;
  }

  /**
   * 装配任务定义扩展。
   *
   * @param element 任务型模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final T element, final BpmnTransformContext context) {
    final BpmnJobWorkerTask task =
        context.getCurrentProcess().getElementById(element.getId(), BpmnJobWorkerTask.class);
    final KunpengTaskDefinition taskDefinition =
        element.getSingleExtensionElement(KunpengTaskDefinition.class);
    if (taskDefinition == null) {
      return;
    }
    BpmnJobProperties jobProperties = task.getJobProperties();
    if (jobProperties == null) {
      jobProperties = new BpmnJobProperties();
      task.setJobProperties(jobProperties);
    }
    applyTaskDefinition(jobProperties, taskDefinition, context);
  }

  /**
   * 把任务定义解析到任务属性（任务类型与重试次数表达式，经上下文表达式缓存去重）。
   *
   * @param jobProperties 待填充的任务属性
   * @param taskDefinition 任务定义扩展
   * @param context 转换上下文
   */
  static void applyTaskDefinition(
      final BpmnJobProperties jobProperties,
      final KunpengTaskDefinition taskDefinition,
      final BpmnTransformContext context) {
    jobProperties.setType(context.parseExpression(taskDefinition.getType()));
    jobProperties.setRetries(context.parseExpression(taskDefinition.getRetries()));
  }
}
