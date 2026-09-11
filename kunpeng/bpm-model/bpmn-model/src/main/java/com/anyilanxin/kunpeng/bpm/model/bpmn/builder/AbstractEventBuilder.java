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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Event;
import java.util.function.Consumer;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractEventBuilder<B extends AbstractEventBuilder<B, E>, E extends Event>
    extends AbstractFlowNodeBuilder<B, E> implements KunpengExecutionListenersBuilder<B> {

  private final KunpengExecutionListenersBuilder<B> kunpengExecutionListenersBuilder;

  protected AbstractEventBuilder(
      final BpmnModelInstance modelInstance, final E element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
    kunpengExecutionListenersBuilder = new KunpengExecutionListenersBuilderImpl<>(myself);
  }

  @Override
  public B kunpengStartExecutionListener(final String type, final String retries) {
    return kunpengExecutionListenersBuilder.kunpengStartExecutionListener(type, retries);
  }

  @Override
  public B kunpengStartExecutionListener(final String type) {
    return kunpengExecutionListenersBuilder.kunpengStartExecutionListener(type);
  }

  @Override
  public B kunpengEndExecutionListener(final String type, final String retries) {
    return kunpengExecutionListenersBuilder.kunpengEndExecutionListener(type, retries);
  }

  @Override
  public B kunpengEndExecutionListener(final String type) {
    return kunpengExecutionListenersBuilder.kunpengEndExecutionListener(type);
  }

  @Override
  public B kunpengExecutionListener(
      final Consumer<ExecutionListenerBuilder> executionListenerBuilderConsumer) {
    return kunpengExecutionListenersBuilder.kunpengExecutionListener(
        executionListenerBuilderConsumer);
  }
}
