/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyilanxin.kunpeng.bpm.model.bpmn.builder;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Script;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengScript;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractScriptTaskBuilder<B extends AbstractScriptTaskBuilder<B>>
    extends AbstractJobWorkerTaskBuilder<B, ScriptTask> {

  protected AbstractScriptTaskBuilder(
      final BpmnModelInstance modelInstance, final ScriptTask element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the script format of the build script task.
   *
   * @param scriptFormat the script format to set
   * @return the builder object
   */
  public B scriptFormat(final String scriptFormat) {
    element.setScriptFormat(scriptFormat);
    return myself;
  }

  /**
   * Sets the script of the build script task.
   *
   * @param script the script to set
   * @return the builder object
   */
  public B script(final Script script) {
    element.setScript(script);
    return myself;
  }

  public B scriptText(final String scriptText) {
    final Script script = createChild(Script.class);
    script.setTextContent(scriptText);
    return myself;
  }

  /**
   * Sets feel script text of the script task that is called
   *
   * @param expression the feel expression for the script task
   * @return the builder object
   */
  public B kunpengExpression(final String expression) {
    final KunpengScript kunpengScript = getCreateSingleExtensionElement(KunpengScript.class);
    kunpengScript.setExpression(asKunpengExpression(expression));
    return myself;
  }

  /**
   * Sets the name of the result variable.
   *
   * @param resultVariable the name of the result variable
   * @return the builder object
   */
  public B kunpengResultVariable(final String resultVariable) {
    final KunpengScript kunpengScript = getCreateSingleExtensionElement(KunpengScript.class);
    kunpengScript.setResultVariable(resultVariable);
    return myself;
  }
}
