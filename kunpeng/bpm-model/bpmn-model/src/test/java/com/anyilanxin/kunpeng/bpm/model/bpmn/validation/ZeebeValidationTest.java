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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;
import static java.util.Collections.singletonList;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ProcessBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.junit.runners.Parameterized.Parameters;

public class ZeebeValidationTest extends AbstractZeebeValidationTest {

  @Parameters(name = "{index}: {1}")
  public static Object[][] parameters() {
    return new Object[][] {
      {
        Bpmn.createExecutableProcess("process").done(),
        singletonList(expect("process", "Must have at least one start event"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .intermediateCatchEvent()
            .endEvent()
            .done(),
        singletonList(
            expect(IntermediateCatchEvent.class, "Must have exactly one event definition"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .intermediateCatchEvent("catch", AbstractCatchEventBuilder::compensateEventDefinition)
            .endEvent()
            .done(),
        singletonList(
            expect(IntermediateCatchEvent.class, "Event definition must be one of: message, timer"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("task", b -> b.zeebeJobType("type"))
            .boundaryEvent("msg1")
            .message(m -> m.name("message").zeebeCorrelationKeyExpression("id"))
            .endEvent()
            .moveToActivity("task")
            .boundaryEvent("msg2")
            .message(m -> m.name("message").zeebeCorrelationKeyExpression("orderId"))
            .endEvent()
            .done(),
        singletonList(
            expect(
                "task",
                "Multiple message event definitions with the same name 'message' are not allowed."))
      },
      {
        eventSubprocWithNoneStart(),
        singletonList(
            expect(
                "subprocess",
                "Start events in event subprocesses must be one of: message, timer, error"))
      },
      {eventSubprocWithSignalStart(), valid()}
    };
  }

  private static BpmnModelInstance eventSubprocWithNoneStart() {
    final ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");
    processBuilder.startEvent().endEvent();
    return processBuilder.eventSubProcess("subprocess").startEvent("start_event").endEvent().done();
  }

  private static BpmnModelInstance eventSubprocWithSignalStart() {
    final ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");
    processBuilder.startEvent().endEvent();
    return processBuilder
        .eventSubProcess("subprocess")
        .startEvent("start_event")
        .signal("signal")
        .endEvent()
        .done();
  }
}
