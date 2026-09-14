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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLoopCharacteristics;
import org.junit.runners.Parameterized.Parameters;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;
import static java.util.Arrays.asList;

public class KunpengMultiInstanceLoopCharacteristicsValidationTest
  extends AbstractKunpengValidationTest {

    @Parameters(name = "{index}: {1}")
    public static Object[][] parameters() {
        return new Object[][]{
                // neither loop cardinality nor collection is set
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                          .serviceTask("task", t -> t.kunpengJobType("test").multiInstance())
                                .done(),
                        asList(
                                expect(
                                        MultiInstanceLoopCharacteristics.class,
                                        "Must have Loop cardinality or Collection"))
                },
                // element variable is set but the collection is empty
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .serviceTask(
                                        "task",
                                        t ->
                                          t.kunpengJobType("test")
                                            .multiInstance(b -> b.elementVariable("item")))
                                .done(),
                        asList(
                                expect(
                                        MultiInstanceLoopCharacteristics.class,
                                        "Must have Loop cardinality or Collection"),
                                expect(
                                  KunpengLoopCharacteristics.class,
                                        "Attribute 'inputCollection' must be present and not empty"))
                },
                // collection is set only
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .serviceTask(
                                        "task",
                                        t ->
                                          t.kunpengJobType("test")
                                            .multiInstance(b -> b.collection("items")))
                                .done(),
                        valid()
                },
                // collection and element variable are set
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .serviceTask(
                                        "task",
                                        t ->
                                          t.kunpengJobType("test")
                                            .multiInstance(
                                                                b ->
                                                                  b.collection("items")
                                                                    .elementVariable("item")))
                                .done(),
                        valid()
                },
        };
    }
}
