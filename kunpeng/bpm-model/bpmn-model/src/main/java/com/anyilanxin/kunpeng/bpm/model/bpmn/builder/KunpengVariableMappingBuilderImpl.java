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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengInput;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengIoMapping;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengOutput;

public class KunpengVariableMappingBuilderImpl<B extends AbstractBaseElementBuilder<?, ?>>
    implements KunpengVariablesMappingBuilder<B> {

  private final B elementBuilder;

  public KunpengVariableMappingBuilderImpl(final B elementBuilder) {
    this.elementBuilder = elementBuilder;
  }

  @Override
  public B kunpengInputExpression(final String sourceExpression, final String target) {
    final String expression = elementBuilder.asKunpengExpression(sourceExpression);
    return kunpengInput(expression, target);
  }

  @Override
  public B kunpengOutputExpression(final String sourceExpression, final String target) {
    final String expression = elementBuilder.asKunpengExpression(sourceExpression);
    return kunpengOutput(expression, target);
  }

  @Override
  public B kunpengInput(final String source, final String target) {
    final KunpengIoMapping ioMapping =
        elementBuilder.getCreateSingleExtensionElement(KunpengIoMapping.class);
    final KunpengInput input = elementBuilder.createChild(ioMapping, KunpengInput.class);
    input.setSource(source);
    input.setTarget(target);

    return elementBuilder;
  }

  @Override
  public B kunpengOutput(final String source, final String target) {
    final KunpengIoMapping ioMapping =
        elementBuilder.getCreateSingleExtensionElement(KunpengIoMapping.class);
    final KunpengOutput input = elementBuilder.createChild(ioMapping, KunpengOutput.class);
    input.setSource(source);
    input.setTarget(target);

    return elementBuilder;
  }
}
