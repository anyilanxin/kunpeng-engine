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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengScript;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobProperties;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnScriptTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 脚本任务转换器：装配任务定义扩展（任务型脚本任务）与内联脚本扩展。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ScriptTaskTransformer implements ElementTransformer<ScriptTask> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<ScriptTask> getType() {
    return ScriptTask.class;
  }

  /**
   * 装配任务定义与内联脚本（表达式与结果变量名）。
   *
   * @param element 脚本任务模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final ScriptTask element, final BpmnTransformContext context) {
    final BpmnScriptTask task =
        context.getCurrentProcess().getElementById(element.getId(), BpmnScriptTask.class);

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

    final KunpengScript script = element.getSingleExtensionElement(KunpengScript.class);
    if (script != null) {
      task.setExpression(context.parseExpression(script.getExpression()));
      task.setResultVariable(script.getResultVariable());
    }
  }
}
