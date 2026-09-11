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
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.TaskListenerBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListenerEventType;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ReflectUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

public class KunpengTaskListenersValidationTest {

    @Test
    @DisplayName("task listener with not defined `type` property")
    void testTaskListenerTypeNotDefined() {
        // given
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask(
                                "my_user_task",
                          ut -> ut.kunpengUserTask().kunpengTaskListener(TaskListenerBuilder::completing))
                        .endEvent()
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
          process, expect(KunpengTaskListener.class, "Attribute 'type' must be present and not empty"));
    }

    @Test
    @DisplayName("task listener with not defined `eventType` property")
    void testEventTypeNotDefined() {
        // given
        final BpmnModelInstance process =
                Bpmn.readModelFromStream(
                        ReflectUtil.getResourceAsStream(
                          "com/anyilanxin/kunpeng/bpm/model/bpmn/validation/KunpengTaskListenersValidationTest.testEventTypeNotDefined.bpmn"));

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
          expect(KunpengTaskListener.class, "Attribute 'eventType' must be present and not empty"));
    }

    @DisplayName("task listener with supported `eventType` property")
    @ParameterizedTest(name = "supported event type: ''{0}''")
    @EnumSource(value = KunpengTaskListenerEventType.class)
    void testEventTypeSupported(final KunpengTaskListenerEventType supportedEventType) {
        // given
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask(
                                "user_task",
                                ut ->
                                  ut.kunpengUserTask()
                                    .kunpengTaskListener(
                                                        l -> l.eventType(supportedEventType).type("supported_listener")))
                        .endEvent()
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }
}
