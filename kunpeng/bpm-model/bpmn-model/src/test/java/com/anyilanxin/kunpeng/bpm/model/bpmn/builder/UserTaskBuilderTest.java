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

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.*;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class UserTaskBuilderTest {

    @Test
    void testUserTaskAssigneeCanBeSet() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", task -> task.kunpengAssignee("user1"))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengAssignmentDefinition.class))
                .hasSize(1)
                .extracting(KunpengAssignmentDefinition::getAssignee)
                .containsExactly("user1");
    }

    @Test
    void testUserTaskCandidateGroupsCanBeSet() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", task -> task.kunpengCandidateGroups("role1"))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengAssignmentDefinition.class))
                .hasSize(1)
                .extracting(KunpengAssignmentDefinition::getCandidateGroups)
                .containsExactly("role1");
    }

    @Test
    void testUserTaskCandidateUsersCanBeSet() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", task -> task.kunpengCandidateUsers("user1"))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengAssignmentDefinition.class))
                .hasSize(1)
                .extracting(KunpengAssignmentDefinition::getCandidateUsers)
                .containsExactly("user1");
    }

    @Test
    void shouldSetDueDateOnUserTask() {
        final String dueDate = "2023-02-24T14:29:00Z";
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", task -> task.kunpengDueDate(dueDate))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengTaskSchedule.class))
                .hasSize(1)
                .extracting(KunpengTaskSchedule::getDueDate)
                .containsExactly(dueDate);
    }

    @Test
    void shouldSetFollowUpDateOnUserTask() {
        final String followUpDate = "2023-02-24T14:29:00Z";
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", task -> task.kunpengFollowUpDate(followUpDate))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengTaskSchedule.class))
                .hasSize(1)
                .extracting(KunpengTaskSchedule::getFollowUpDate)
                .containsExactly(followUpDate);
    }

    @Test
    void shouldSetAllExistingUserTaskProperties() {
        final String dueDate = "2023-02-24T14:29:00Z";
        final String followUpDate = "2023-02-24T14:29:00Z";
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask(
                                "userTask1",
                                b ->
                                        b.kunpengAssignee("user1")
                                                .kunpengCandidateGroups("role1")
                                                .kunpengCandidateUsers("user2"))
                        .kunpengDueDate(dueDate)
                        .kunpengFollowUpDate(followUpDate)
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengAssignmentDefinition.class))
                .hasSize(1)
                .extracting(
                        KunpengAssignmentDefinition::getAssignee,
                        KunpengAssignmentDefinition::getCandidateGroups,
                        KunpengAssignmentDefinition::getCandidateUsers)
                .containsExactly(tuple("user1", "role1", "user2"));
        assertThat(extensionElements.getChildElementsByType(KunpengTaskSchedule.class))
                .hasSize(1)
                .extracting(KunpengTaskSchedule::getDueDate, KunpengTaskSchedule::getFollowUpDate)
                .containsExactly(tuple(dueDate, followUpDate));
    }

    @Test
    void testUserTaskFormIdNotNull() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1")
                        .kunpengUserTaskForm("{}")
                        .endEvent()
                        .done();

        final Collection<KunpengUserTaskForm> kunpengUserTaskForms =
                instance.getModelElementsByType(KunpengUserTaskForm.class);

        assertThat(kunpengUserTaskForms).hasSize(1);
        final KunpengUserTaskForm kunpengUserTaskForm = kunpengUserTaskForms.iterator().next();
        assertThat(kunpengUserTaskForm.getId()).isNotEmpty();
    }

    @Test
    void shouldMarkAsKunpengUserTask() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1")
                        .kunpengUserTask()
                        .endEvent()
                        .done();

        final Collection<KunpengUserTask> kunpengUserTasks =
                instance.getModelElementsByType(KunpengUserTask.class);

        assertThat(kunpengUserTasks).hasSize(1);
    }

    @Test
    void shouldMarkAsKunpengUserTaskIfUsedMultipleTimes() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1")
                        .kunpengUserTask()
                        .kunpengUserTask()
                        .kunpengUserTask()
                        .endEvent()
                        .done();

        final Collection<KunpengUserTask> kunpengUserTasks =
                instance.getModelElementsByType(KunpengUserTask.class);

        assertThat(kunpengUserTasks).hasSize(1);
    }

    @Test
    void shouldNotMarkAsKunpengUserTaskByDefault() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1")
                        .endEvent()
                        .done();

        final Collection<KunpengUserTask> kunpengUserTasks =
                instance.getModelElementsByType(KunpengUserTask.class);

        assertThat(kunpengUserTasks).isEmpty();
    }

    @Test
    void shouldSetAllExistingUserTaskPropertiesForKunpengUserTask() {
        final String dueDate = "2023-02-24T14:29:00Z";
        final String followUpDate = "2023-02-24T14:29:00Z";
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask(
                                "userTask1",
                                b ->
                                        b.kunpengAssignee("user1")
                                                .kunpengCandidateGroups("role1")
                                                .kunpengCandidateUsers("user2"))
                        .kunpengDueDate(dueDate)
                        .kunpengFollowUpDate(followUpDate)
                        .kunpengUserTask()
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengAssignmentDefinition.class))
                .hasSize(1)
                .extracting(
                        KunpengAssignmentDefinition::getAssignee,
                        KunpengAssignmentDefinition::getCandidateGroups,
                        KunpengAssignmentDefinition::getCandidateUsers)
                .containsExactly(tuple("user1", "role1", "user2"));
        assertThat(extensionElements.getChildElementsByType(KunpengTaskSchedule.class))
                .hasSize(1)
                .extracting(KunpengTaskSchedule::getDueDate, KunpengTaskSchedule::getFollowUpDate)
                .containsExactly(tuple(dueDate, followUpDate));
    }

    @ParameterizedTest
    @EnumSource(KunpengBindingType.class)
    void shouldSetFormBindingType(final KunpengBindingType bindingType) {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1")
                        .kunpengFormBindingType(bindingType)
                        .endEvent()
                        .done();

        // then
        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengFormDefinition.class))
                .hasSize(1)
                .extracting(KunpengFormDefinition::getBindingType)
                .containsExactly(bindingType);
    }

    @Test
    void shouldSetFormVersionTag() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask")
                        .kunpengFormVersionTag("v1")
                        .endEvent()
                        .done();

        // then
        final ModelElementInstance userTask = instance.getModelElementById("userTask");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengFormDefinition.class))
                .hasSize(1)
                .extracting(KunpengFormDefinition::getVersionTag)
                .containsExactly("v1");
    }

    @Test
    void shouldSetPriorityOnKunpengUserTask() {
        final String priority = "20";
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", task -> task.kunpengUserTask().kunpengTaskPriority(priority))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengPriorityDefinition.class))
                .hasSize(1)
                .extracting(KunpengPriorityDefinition::getPriority)
                .containsExactly(priority);
    }

    @Test
    void shouldSetDefaultPriorityOnKunpengUserTask() {
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("userTask1", AbstractUserTaskBuilder::kunpengUserTask)
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = instance.getModelElementById("userTask1");
        final ExtensionElements extensionElements =
                (ExtensionElements) userTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengPriorityDefinition.class))
                .hasSize(1)
                .extracting(KunpengPriorityDefinition::getPriority)
                .containsExactly(KunpengPriorityDefinition.DEFAULT_LITERAL_PRIORITY);
    }
}
