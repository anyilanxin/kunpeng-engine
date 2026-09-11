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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import java.util.function.Consumer;

public class KunpengExecutionListenersBuilderImpl<B extends AbstractBaseElementBuilder<?, ?>>
    implements KunpengExecutionListenersBuilder<B> {

  private final B elementBuilder;

  public KunpengExecutionListenersBuilderImpl(final B elementBuilder) {
    this.elementBuilder = elementBuilder;
  }

  @Override
  public B kunpengStartExecutionListener(final String type, final String retries) {
    final KunpengExecutionListener listener = createKunpengExecutionListener();
    listener.setEventType(KunpengExecutionListenerEventType.start);
    listener.setType(type);
    listener.setRetries(retries);

    return elementBuilder;
  }

  @Override
  public B kunpengStartExecutionListener(final String type) {
    return kunpengStartExecutionListener(type, KunpengExecutionListener.DEFAULT_RETRIES);
  }

  @Override
  public B kunpengEndExecutionListener(final String type, final String retries) {
    final KunpengExecutionListener listener = createKunpengExecutionListener();
    listener.setEventType(KunpengExecutionListenerEventType.end);
    listener.setType(type);
    listener.setRetries(retries);

    return elementBuilder;
  }

  @Override
  public B kunpengEndExecutionListener(final String type) {
    return kunpengEndExecutionListener(type, KunpengExecutionListener.DEFAULT_RETRIES);
  }

  @Override
  public B kunpengExecutionListener(
      final Consumer<ExecutionListenerBuilder> executionListenerBuilderConsumer) {
    final KunpengExecutionListener listener = createKunpengExecutionListener();
    final ExecutionListenerBuilder builder = new ExecutionListenerBuilder(listener, elementBuilder);

    executionListenerBuilderConsumer.accept(builder);

    return elementBuilder;
  }

  private KunpengExecutionListener createKunpengExecutionListener() {
    final KunpengExecutionListeners executionListeners =
        elementBuilder.getCreateSingleExtensionElement(KunpengExecutionListeners.class);
    return elementBuilder.createChild(executionListeners, KunpengExecutionListener.class);
  }
}
