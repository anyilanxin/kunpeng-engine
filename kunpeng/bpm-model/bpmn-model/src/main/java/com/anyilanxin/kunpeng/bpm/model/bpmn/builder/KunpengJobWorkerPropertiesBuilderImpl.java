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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;

public class KunpengJobWorkerPropertiesBuilderImpl<B extends AbstractBaseElementBuilder<?, ?>>
    implements KunpengJobWorkerPropertiesBuilder<B> {

  private final B elementBuilder;

  protected KunpengJobWorkerPropertiesBuilderImpl(final B elementBuilder) {
    this.elementBuilder = elementBuilder;
  }

  @Override
  public B kunpengJobType(final String type) {
    final KunpengTaskDefinition taskDefinition =
        elementBuilder.getCreateSingleExtensionElement(KunpengTaskDefinition.class);
    taskDefinition.setType(type);
    return elementBuilder;
  }

  @Override
  public B kunpengJobTypeExpression(final String expression) {
    return kunpengJobType(elementBuilder.asKunpengExpression(expression));
  }

  @Override
  public B kunpengJobRetries(final String retries) {
    final KunpengTaskDefinition taskDefinition =
        elementBuilder.getCreateSingleExtensionElement(KunpengTaskDefinition.class);
    taskDefinition.setRetries(retries);
    return elementBuilder;
  }

  @Override
  public B kunpengJobRetriesExpression(final String expression) {
    return kunpengJobRetries(elementBuilder.asKunpengExpression(expression));
  }
}
