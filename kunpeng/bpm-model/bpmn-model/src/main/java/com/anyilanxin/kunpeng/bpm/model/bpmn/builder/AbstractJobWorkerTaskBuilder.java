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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Task;

/**
 * A builder for tasks that are based on jobs and should be processed by job workers. For example,
 * service tasks.
 */
public abstract class AbstractJobWorkerTaskBuilder<
        B extends AbstractJobWorkerTaskBuilder<B, T>, T extends Task>
    extends AbstractTaskBuilder<B, T> implements KunpengJobWorkerElementBuilder<B> {

  private final KunpengJobWorkerPropertiesBuilder<B> jobWorkerPropertiesBuilder;

  protected AbstractJobWorkerTaskBuilder(
      final BpmnModelInstance modelInstance, final T element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
    // delegates to the element builder but keeping this class for backward compatibility
    jobWorkerPropertiesBuilder = new KunpengJobWorkerPropertiesBuilderImpl<>(myself);
  }

  @Override
  public B kunpengJobType(final String type) {
    return jobWorkerPropertiesBuilder.kunpengJobType(type);
  }

  @Override
  public B kunpengJobTypeExpression(final String expression) {
    return jobWorkerPropertiesBuilder.kunpengJobTypeExpression(expression);
  }

  @Override
  public B kunpengJobRetries(final String retries) {
    return jobWorkerPropertiesBuilder.kunpengJobRetries(retries);
  }

  @Override
  public B kunpengJobRetriesExpression(final String expression) {
    return jobWorkerPropertiesBuilder.kunpengJobRetriesExpression(expression);
  }
}
