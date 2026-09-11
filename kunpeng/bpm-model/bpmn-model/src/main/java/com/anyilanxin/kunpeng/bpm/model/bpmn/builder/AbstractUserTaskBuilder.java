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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants.USER_TASK_FORM_KEY_BPMN_LOCATION;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants.USER_TASK_FORM_KEY_KUNPENG_FORMS_FORMAT;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.UserTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.*;
import java.util.function.Consumer;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractUserTaskBuilder<B extends AbstractUserTaskBuilder<B>>
    extends AbstractTaskBuilder<B, UserTask> implements KunpengUserTaskPropertiesBuilder<B> {

  protected AbstractUserTaskBuilder(
      final BpmnModelInstance modelInstance, final UserTask element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the implementation of the build user task.
   *
   * @param implementation the implementation to set
   * @return the builder object
   */
  public B implementation(final String implementation) {
    element.setImplementation(implementation);
    return myself;
  }

  @Override
  public B kunpengFormKey(final String format, final String location, final String id) {
    return kunpengFormKey(String.format("%s:%s:%s", format, location, id));
  }

  @Override
  public B kunpengFormKey(final String formKey) {
    final KunpengFormDefinition formDefinition =
        getCreateSingleExtensionElement(KunpengFormDefinition.class);
    formDefinition.setFormKey(formKey);
    return myself;
  }

  @Override
  public B kunpengUserTaskForm(final String userTaskForm) {
    final KunpengUserTaskForm kunpengUserTaskForm = createKunpengUserTaskForm();
    kunpengUserTaskForm.setTextContent(userTaskForm);
    return kunpengFormKey(
        USER_TASK_FORM_KEY_KUNPENG_FORMS_FORMAT,
        USER_TASK_FORM_KEY_BPMN_LOCATION,
        kunpengUserTaskForm.getId());
  }

  @Override
  public B kunpengUserTaskForm(final String id, final String userTaskForm) {
    final KunpengUserTaskForm kunpengUserTaskForm = createKunpengUserTaskForm();
    kunpengUserTaskForm.setId(id);
    kunpengUserTaskForm.setTextContent(userTaskForm);
    return kunpengFormKey(
        USER_TASK_FORM_KEY_KUNPENG_FORMS_FORMAT, USER_TASK_FORM_KEY_BPMN_LOCATION, id);
  }

  @Override
  public B kunpengAssignee(final String assignee) {
    final KunpengAssignmentDefinition assignment =
        myself.getCreateSingleExtensionElement(KunpengAssignmentDefinition.class);
    assignment.setAssignee(assignee);
    return myself;
  }

  @Override
  public B kunpengAssigneeExpression(final String expression) {
    return kunpengAssignee(asKunpengExpression(expression));
  }

  @Override
  public B kunpengCandidateGroups(final String candidateGroups) {
    final KunpengAssignmentDefinition assignment =
        myself.getCreateSingleExtensionElement(KunpengAssignmentDefinition.class);
    assignment.setCandidateGroups(candidateGroups);
    return myself;
  }

  @Override
  public B kunpengCandidateGroupsExpression(final String expression) {
    return kunpengCandidateGroups(asKunpengExpression(expression));
  }

  @Override
  public B kunpengCandidateUsers(final String candidateUsers) {
    final KunpengAssignmentDefinition assignment =
        myself.getCreateSingleExtensionElement(KunpengAssignmentDefinition.class);
    assignment.setCandidateUsers(candidateUsers);
    return myself;
  }

  @Override
  public B kunpengCandidateUsersExpression(final String expression) {
    return kunpengCandidateUsers(asKunpengExpression(expression));
  }

  @Override
  public B kunpengDueDate(final String dueDate) {
    final KunpengTaskSchedule taskSchedule =
        myself.getCreateSingleExtensionElement(KunpengTaskSchedule.class);
    taskSchedule.setDueDate(dueDate);
    return myself;
  }

  @Override
  public B kunpengDueDateExpression(final String expression) {
    return kunpengDueDate(asKunpengExpression(expression));
  }

  @Override
  public B kunpengFollowUpDate(final String followUpDate) {
    final KunpengTaskSchedule taskSchedule =
        myself.getCreateSingleExtensionElement(KunpengTaskSchedule.class);
    taskSchedule.setFollowUpDate(followUpDate);
    return myself;
  }

  @Override
  public B kunpengFollowUpDateExpression(final String expression) {
    return kunpengFollowUpDate(asKunpengExpression(expression));
  }

  @Override
  public B kunpengFormId(final String formId) {
    final KunpengFormDefinition formDefinition =
        getCreateSingleExtensionElement(KunpengFormDefinition.class);
    formDefinition.setFormId(formId);
    return myself;
  }

  @Override
  public B kunpengUserTask() {
    getCreateSingleExtensionElement(KunpengUserTask.class);
    getCreateSingleExtensionElement(KunpengPriorityDefinition.class);
    return myself;
  }

  @Override
  public B kunpengExternalFormReference(final String externalFormReference) {
    final KunpengFormDefinition formDefinition =
        getCreateSingleExtensionElement(KunpengFormDefinition.class);
    formDefinition.setExternalReference(externalFormReference);
    return myself;
  }

  @Override
  public B kunpengExternalFormReferenceExpression(final String expression) {
    return kunpengExternalFormReference(asKunpengExpression(expression));
  }

  @Override
  public B kunpengFormBindingType(final KunpengBindingType bindingType) {
    final KunpengFormDefinition formDefinition =
        getCreateSingleExtensionElement(KunpengFormDefinition.class);
    formDefinition.setBindingType(bindingType);
    return myself;
  }

  @Override
  public B kunpengFormVersionTag(final String versionTag) {
    final KunpengFormDefinition formDefinition =
        getCreateSingleExtensionElement(KunpengFormDefinition.class);
    formDefinition.setVersionTag(versionTag);
    return myself;
  }

  @Override
  public B kunpengTaskPriority(final String priority) {
    final KunpengPriorityDefinition priorityDefinition =
        myself.getCreateSingleExtensionElement(KunpengPriorityDefinition.class);
    priorityDefinition.setPriority(priority);
    return myself;
  }

  @Override
  public B kunpengTaskPriorityExpression(final String expression) {
    return kunpengTaskPriority(asKunpengExpression(expression));
  }

  public B kunpengTaskListener(final Consumer<TaskListenerBuilder> taskListenerBuilderConsumer) {
    final KunpengTaskListener listener = createTaskListenerElement();
    listener.setRetries(KunpengTaskListener.DEFAULT_RETRIES);

    final TaskListenerBuilder builder = new TaskListenerBuilder(listener, myself);
    taskListenerBuilderConsumer.accept(builder);
    return myself;
  }

  private KunpengTaskListener createTaskListenerElement() {
    final KunpengTaskListeners taskListeners =
        myself.getCreateSingleExtensionElement(KunpengTaskListeners.class);
    return myself.createChild(taskListeners, KunpengTaskListener.class);
  }
}
