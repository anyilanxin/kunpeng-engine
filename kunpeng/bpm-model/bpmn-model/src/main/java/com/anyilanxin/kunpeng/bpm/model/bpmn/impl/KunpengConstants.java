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
package com.anyilanxin.kunpeng.bpm.model.bpmn.impl;

public interface KunpengConstants {

  String ATTRIBUTE_RETRIES = "retries";
  String ATTRIBUTE_TYPE = "type";
  String ATTRIBUTE_EVENT_TYPE = "eventType";

  String ATTRIBUTE_KEY = "key";
  String ATTRIBUTE_NAME = "name";
  String ATTRIBUTE_VALUE = "value";

  String ATTRIBUTE_SOURCE = "source";
  String ATTRIBUTE_TARGET = "target";

  String ATTRIBUTE_CORRELATION_KEY = "correlationKey";
  String ATTRIBUTE_MESSAGE_TIME_TO_LIVE = "timeToLive";

  String ATTRIBUTE_COLLECTION = "collection";
  String ATTRIBUTE_ELEMENT_VARIABLE = "elementVariable";

  String ATTRIBUTE_INPUT_COLLECTION = "inputCollection";
  String ATTRIBUTE_INPUT_ELEMENT = "inputElement";
  String ATTRIBUTE_OUTPUT_COLLECTION = "outputCollection";
  String ATTRIBUTE_OUTPUT_ELEMENT = "outputElement";

  String ATTRIBUTE_PROCESS_ID = "processId";
  String ATTRIBUTE_PROPAGATE_ALL_CHILD_VARIABLES = "propagateAllChildVariables";
  String ATTRIBUTE_PROPAGATE_ALL_PARENT_VARIABLES = "propagateAllParentVariables";

  String ATTRIBUTE_FORM_KEY = "formKey";
  String ATTRIBUTE_FORM_ID = "formId";
  String ATTRIBUTE_EXTERNAL_REFERENCE = "externalReference";

  String ATTRIBUTE_ASSIGNEE = "assignee";
  String ATTRIBUTE_CANDIDATE_GROUPS = "candidateGroups";
  String ATTRIBUTE_CANDIDATE_USERS = "candidateUsers";

  String ATTRIBUTE_DUE_DATE = "dueDate";
  String ATTRIBUTE_FOLLOW_UP_DATE = "followUpDate";

  String ATTRIBUTE_DECISION_ID = "decisionId";

  String ATTRIBUTE_EXPRESSION = "expression";

  String ATTRIBUTE_RESULT_VARIABLE = "resultVariable";

  String ATTRIBUTE_BINDING_TYPE = "bindingType";
  String ATTRIBUTE_VERSION_TAG = "versionTag";

  String ATTRIBUTE_PRIORITY = "priority";

  String ATTRIBUTE_ACTIVE_ELEMENTS_COLLECTION = "activeElementsCollection";

  String ATTRIBUTE_RESOURCE_ID = "resourceId";
  String ATTRIBUTE_RESOURCE_TYPE = "resourceType";
  String ATTRIBUTE_LINK_NAME = "linkName";

  String ELEMENT_DYNAMIC_ADDITIONS = "dynamicAdditions";
  String ELEMENT_ADDITION = "addition";
  String ELEMENT_INPUT = "input";
  String ELEMENT_IO_MAPPING = "ioMapping";
  String ELEMENT_OUTPUT = "output";

  String ELEMENT_SCRIPT = "script";
  String ELEMENT_SUBSCRIPTION = "subscription";
  String ELEMENT_PUBLISH_MESSAGE = "publishMessage";

  String ELEMENT_TASK_DEFINITION = "taskDefinition";

  String ELEMENT_FORM_DEFINITION = "formDefinition";
  String ELEMENT_USER_TASK_FORM = "userTaskForm";

  String ELEMENT_ASSIGNMENT_DEFINITION = "assignmentDefinition";

  String ELEMENT_SCHEDULE_DEFINITION = "taskSchedule";

  String ELEMENT_LOOP_CHARACTERISTICS = "loopCharacteristics";

  String ELEMENT_CALLED_ELEMENT = "calledElement";

  String ELEMENT_CALLED_DECISION = "calledDecision";

  String ELEMENT_PROPERTIES = "properties";

  String ELEMENT_PROPERTY = "property";

  String ELEMENT_USER_TASK = "userTask";

  String ELEMENT_EXECUTION_LISTENERS = "executionListeners";
  String ELEMENT_EXECUTION_LISTENER = "executionListener";
  String ELEMENT_TASK_LISTENERS = "taskListeners";
  String ELEMENT_TASK_LISTENER = "taskListener";

  String ELEMENT_VERSION_TAG = "versionTag";

  /** Form key format used for camunda-forms format */
  String USER_TASK_FORM_KEY_KUNPENG_FORMS_FORMAT = "camunda-forms";

  /** Form key location used for forms embedded in the same BPMN file, i.e. zeebeUserTaskForm */
  String USER_TASK_FORM_KEY_BPMN_LOCATION = "bpmn";

  String ELEMENT_PRIORITY_DEFINITION = "priorityDefinition";

  String ELEMENT_AD_HOC = "adHoc";

  String ELEMENT_LINKED_RESOURCE = "linkedResource";
  String ELEMENT_LINKED_RESOURCES = "linkedResources";

  /**
   * The postfix of an ID of an ad-hoc sub-process inner instance (pattern:
   * AD_HOC_SUB_PROCESS_ID#innerInstance)
   */
  String AD_HOC_SUB_PROCESS_INNER_INSTANCE_ID_POSTFIX = "#innerInstance";
}
