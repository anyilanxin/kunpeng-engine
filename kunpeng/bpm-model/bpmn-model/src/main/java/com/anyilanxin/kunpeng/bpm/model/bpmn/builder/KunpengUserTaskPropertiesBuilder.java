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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;

/** A fluent builder for kunpeng specific user task related properties. */
public interface KunpengUserTaskPropertiesBuilder<B extends KunpengUserTaskPropertiesBuilder<B>> {

  /**
   * Sets the form key with the format 'format:location:id' of the build user task.
   *
   * @param format the format of the reference form
   * @param location the location where the form is available
   * @param id the id of the form
   * @return the builder object
   */
  B kunpengFormKey(String format, String location, String id);

  /**
   * Sets the form key of the build user task.
   *
   * @param formKey the form key to set
   * @return the builder object
   */
  B kunpengFormKey(String formKey);

  /**
   * Creates a new user task form with the given context, assuming it is of the format camunda-forms
   * and embedded inside the diagram.
   *
   * @param userTaskForm the XML encoded user task form json in the camunda-forms format
   * @return the builder object
   */
  B kunpengUserTaskForm(String userTaskForm);

  /**
   * Creates a new user task form with the given context, assuming it is of the format camunda-forms
   * and embedded inside the diagram.
   *
   * @param id the unique identifier of the user task form element
   * @param userTaskForm the XML encoded user task form json in the camunda-forms format
   * @return the builder object
   */
  B kunpengUserTaskForm(String id, String userTaskForm);

  /**
   * Sets a static assignee for the user task
   *
   * @param assignee the assignee of the user task
   * @return the builder object
   */
  B kunpengAssignee(String assignee);

  /**
   * Sets a dynamic assignee for the user task that is retrieved from the given expression
   *
   * @param expression the expression for the assignee of the user task
   * @return the builder object
   */
  B kunpengAssigneeExpression(String expression);

  /**
   * Sets a static candidateGroups for the user task
   *
   * @param candidateGroups the candidateGroups of the user task
   * @return the builder object
   */
  B kunpengCandidateGroups(String candidateGroups);

  /**
   * Sets a dynamic candidateGroups for the user task that is retrieved from the given expression
   *
   * @param expression the expression for the candidateGroups of the user task
   * @return the builder object
   */
  B kunpengCandidateGroupsExpression(String expression);

  /**
   * Sets a static candidateUsers for the user task
   *
   * @param candidateUsers the candidateUsers of the user task
   * @return the builder object
   */
  B kunpengCandidateUsers(String candidateUsers);

  /**
   * Sets a dynamic candidateUsers for the user task that is retrieved from the given expression
   *
   * @param expression the expression for the candidateUsers of the user task
   * @return the builder object
   */
  B kunpengCandidateUsersExpression(String expression);

  /**
   * Sets a static dueDate for the user task
   *
   * @param dueDate the dueDate of the user task
   * @return the builder object
   */
  B kunpengDueDate(String dueDate);

  /**
   * Sets a dynamic dueDate for the user task that is retrieved from the given expression
   *
   * @param expression the expression for the dueDate of the user task
   * @return the builder object
   */
  B kunpengDueDateExpression(String expression);

  /**
   * Sets a static followUpDate for the user task
   *
   * @param followUpDate the followUpDate of the user task
   * @return the builder object
   */
  B kunpengFollowUpDate(String followUpDate);

  /**
   * Sets a dynamic followUpDate for the user task that is retrieved from the given expression
   *
   * @param expression the expression for the followUpDate of the user task
   * @return the builder object
   */
  B kunpengFollowUpDateExpression(String expression);

  /**
   * Sets the form id of the build user task.
   *
   * @param formId the form id to set
   * @return the builder object
   */
  B kunpengFormId(String formId);

  /**
   * Marks the user task as native Kunpeng user task.
   *
   * @return the builder object
   */
  B kunpengUserTask();

  /**
   * Sets a static external form reference for the user task.
   *
   * @param externalReference the external form reference of the user task
   * @return the builder object
   */
  B kunpengExternalFormReference(String externalReference);

  /**
   * Sets a dynamic external form reference for the user task that is retrieved from the given
   * expression.
   *
   * @param expression the expression for the external form reference of the user task
   * @return the builder object
   */
  B kunpengExternalFormReferenceExpression(String expression);

  /**
   * Sets the binding type for the user task's form.
   *
   * @param bindingType the binding type to set
   * @return the builder object
   */
  B kunpengFormBindingType(final KunpengBindingType bindingType);

  /**
   * Sets the version tag for the user task's form.
   *
   * @param versionTag the version tag to set
   * @return the builder object
   */
  B kunpengFormVersionTag(final String versionTag);

  /**
   * Sets a static priority for the user task.
   *
   * @param priority the priority value to set
   * @return the builder object
   */
  B kunpengTaskPriority(final String priority);

  /**
   * Sets a dynamic priority for the user task retrieved from the given expression.
   *
   * @param expression the expression for the priority to set
   * @return the builder object
   */
  B kunpengTaskPriorityExpression(final String expression);
}
