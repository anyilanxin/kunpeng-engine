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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledDecision;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobProperties;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 业务规则任务转换器：装配任务定义扩展（任务型）或被调用决策扩展（决策型），二者按声明取其一。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BusinessRuleTaskTransformer implements ElementTransformer<BusinessRuleTask> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<BusinessRuleTask> getType() {
    return BusinessRuleTask.class;
  }

  /**
   * 先装配任务定义（任务型），再装配被调用决策（决策型）。
   *
   * @param element 业务规则任务模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final BusinessRuleTask element, final BpmnTransformContext context) {
    final BpmnBusinessRuleTask task =
        context.getCurrentProcess().getElementById(element.getId(), BpmnBusinessRuleTask.class);

    final KunpengTaskDefinition taskDefinition =
        element.getSingleExtensionElement(KunpengTaskDefinition.class);
    if (taskDefinition != null) {
      BpmnJobProperties jobProperties = task.getJobProperties();
      if (jobProperties == null) {
        jobProperties = new BpmnJobProperties();
        task.setJobProperties(jobProperties);
      }
      JobWorkerTaskTransformer.applyTaskDefinition(jobProperties, taskDefinition, context);
    }

    final KunpengCalledDecision calledDecision =
        element.getSingleExtensionElement(KunpengCalledDecision.class);
    if (calledDecision != null) {
      task.setDecisionId(context.parseExpression(calledDecision.getDecisionId()));
      task.setResultVariable(calledDecision.getResultVariable());
      task.setBindingType(calledDecision.getBindingType());
      task.setVersionTag(calledDecision.getVersionTag());
    }
  }
}
