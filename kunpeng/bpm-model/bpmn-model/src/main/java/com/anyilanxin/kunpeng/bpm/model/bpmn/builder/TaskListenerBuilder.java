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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListenerEventType;

public class TaskListenerBuilder {

  private final KunpengTaskListener element;
  private final AbstractBaseElementBuilder<?, ?> elementBuilder;

  protected TaskListenerBuilder(
      final KunpengTaskListener element, final AbstractBaseElementBuilder<?, ?> elementBuilder) {
    this.element = element;
    this.elementBuilder = elementBuilder;
  }

  public TaskListenerBuilder eventType(final KunpengTaskListenerEventType eventType) {
    element.setEventType(eventType);
    return this;
  }

  public TaskListenerBuilder creating() {
    return eventType(KunpengTaskListenerEventType.creating);
  }

  public TaskListenerBuilder updating() {
    return eventType(KunpengTaskListenerEventType.updating);
  }

  public TaskListenerBuilder assigning() {
    return eventType(KunpengTaskListenerEventType.assigning);
  }

  public TaskListenerBuilder completing() {
    return eventType(KunpengTaskListenerEventType.completing);
  }

  public TaskListenerBuilder canceling() {
    return eventType(KunpengTaskListenerEventType.canceling);
  }

  public TaskListenerBuilder type(final String type) {
    element.setType(type);
    return this;
  }

  public TaskListenerBuilder typeExpression(final String typeExpression) {
    return type(elementBuilder.asKunpengExpression(typeExpression));
  }

  public TaskListenerBuilder retries(final String retries) {
    element.setRetries(retries);
    return this;
  }

  public TaskListenerBuilder retriesExpression(final String retriesExpression) {
    return retries(elementBuilder.asKunpengExpression(retriesExpression));
  }
}
