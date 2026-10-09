/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
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
package io.camunda.connector.runtime.core.inbound.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.ClientStatusException;
import io.camunda.connector.api.inbound.CorrelationFailureHandlingStrategy;
import io.camunda.connector.api.inbound.CorrelationResult.Failure;
import io.camunda.connector.api.inbound.CorrelationResult.Success;
import io.camunda.connector.runtime.core.TestObjectMapperSupplier;
import io.camunda.connector.runtime.core.inbound.InboundConnectorElement;
import io.camunda.connector.runtime.core.inbound.ProcessElementWithRuntimeData;
import io.camunda.connector.runtime.core.inbound.correlation.MessageCorrelationPoint.StandaloneMessageCorrelationPoint;
import io.camunda.connector.runtime.core.testutil.command.CorrelateMessageCommandDummy;
import io.camunda.connector.runtime.core.testutil.command.CreateCommandDummy;
import io.grpc.Status;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InboundCorrelationHandlerTest {

  private KunpengClient client;
  private InboundCorrelationHandler handler;

  @BeforeEach
  public void initMock() {
    client = mock(KunpengClient.class);
    handler = new InboundCorrelationHandler(client, TestObjectMapperSupplier.INSTANCE);
  }

  @Test
  void upstreamZeebeError_shouldThrow() {
    // given
    var point = new StandaloneMessageCorrelationPoint("test-msg", "=\"test\"");
    var element = mock(InboundConnectorElement.class);
    when(element.correlationPoint()).thenReturn(point);
    when(element.element())
        .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

    when(client.newMessageCorrelationCommand())
        .thenThrow(new ClientStatusException(Status.UNAVAILABLE, null));

    // when & then
    var error =
        assertDoesNotThrow(() -> handler.correlate(List.of(element), Collections.emptyMap()));
    assertThat(error).isInstanceOf(Failure.ZeebeClientStatus.class);
    assertThat(((Failure.ZeebeClientStatus) error).status()).isEqualTo("UNAVAILABLE");
  }

  @Test
  void multipleElements_singleMatch() {
    // given
    var startEventPoint = new StartEventCorrelationPoint("process1", 0, 0);
    var startEventElement = mock(InboundConnectorElement.class);
    when(startEventElement.correlationPoint()).thenReturn(startEventPoint);
    when(startEventElement.element())
        .thenReturn(
            new ProcessElementWithRuntimeData("process1", 0, 0, "startEventElementId", "default"));
    when(startEventElement.activationCondition()).thenReturn("=testKey==\"testValue1\"");
    var messageElement = mock(InboundConnectorElement.class);
    when(messageElement.activationCondition()).thenReturn("=testKey==\"testValue2\"");

    var dummyCommand = Mockito.spy(new CreateCommandDummy());
    when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

    // when
    var result =
        handler.correlate(
            List.of(startEventElement, messageElement), Map.of("testKey", "testValue1"));

    // then
    verify(client).newCreateProcessInstanceCommand();
    verifyNoMoreInteractions(client);

    verify(dummyCommand).processDefinitionKey("process1");
    verify(dummyCommand).version(0);
    verify(dummyCommand).send();

    assertThat(result).isInstanceOf(Success.ProcessInstanceCreated.class);
    var success = (Success.ProcessInstanceCreated) result;
    assertThat(success.activatedElement()).isEqualTo(startEventElement.element());
  }

  @Test
  void multipleElements_multipleMatches_errorRaised() {
    // given
    var startEventElement = mock(InboundConnectorElement.class);
    when(startEventElement.activationCondition()).thenReturn("=testKey==\"testValue\"");
    var messageElement = mock(InboundConnectorElement.class);
    when(messageElement.activationCondition()).thenReturn("=testKey==\"testValue\"");

    // when
    var result =
        handler.correlate(
            List.of(startEventElement, messageElement), Map.of("testKey", "testValue"));

    // then
    assertThat(result).isInstanceOf(Failure.InvalidInput.class);
    assertThat(((Failure.InvalidInput) result).message())
        .contains("Multiple connectors are activated");
  }

  @Nested
  class ZeebeClientMethodSelection {

    @Test
    void startEvent_shouldCallCorrectZeebeMethod() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      var dummyCommand = Mockito.spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      var result = handler.correlate(List.of(element), Collections.emptyMap());

      // then
      verify(client).newCreateProcessInstanceCommand();
      verifyNoMoreInteractions(client);

      verify(dummyCommand).processDefinitionKey(point.bpmnProcessId());
      verify(dummyCommand).version(point.version());
      verify(dummyCommand).send();

      assertThat(result).isInstanceOf(Success.ProcessInstanceCreated.class);
      var success = (Success.ProcessInstanceCreated) result;
      assertThat(success.activatedElement()).isEqualTo(element.element());
    }

    @Test
    void message_shouldCallCorrectZeebeMethod() {
      // given
      var correlationKeyValue = "someTestCorrelationKeyValue";
      var point = new StandaloneMessageCorrelationPoint("msg1", "=correlationKey");
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("correlationKey", correlationKeyValue);

      var dummyCommand = spy(new CorrelateMessageCommandDummy());
      when(client.newMessageCorrelationCommand()).thenReturn(dummyCommand);

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newMessageCorrelationCommand();
      verifyNoMoreInteractions(client);

      verify(dummyCommand).messageName(point.messageName());
      verify(dummyCommand).correlationKey(correlationKeyValue);
      verify(dummyCommand).send();

      assertThat(result).isInstanceOf(Success.MessageCorrelated.class);
      var success = (Success.MessageCorrelated) result;
      assertThat(success.activatedElement()).isEqualTo(element.element());
    }

    @Test
    void startMessageEvent_shouldCallCorrectZeebeMethod() {
      // given
      var point = new MessageStartEventCorrelationPoint("test", "", "process1", 1, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      var dummyCommand = Mockito.spy(new CorrelateMessageCommandDummy());
      when(client.newMessageCorrelationCommand()).thenReturn(dummyCommand);

      // when
      var result = handler.correlate(List.of(element), Collections.emptyMap());

      // then
      verify(client).newMessageCorrelationCommand();
      verifyNoMoreInteractions(client);

      verify(dummyCommand).messageName("test");
      verify(dummyCommand).send();

      assertThat(result).isInstanceOf(Success.MessageCorrelated.class);
      var success = (Success.MessageCorrelated) result;
      assertThat(success.activatedElement()).isEqualTo(element.element());
    }
  }

  @Nested
  class ActivationCondition {

    @Test
    void activationConditionFalse_strategyForwardErrorToUpstream() {
      // given
      var element = mock(InboundConnectorElement.class);
      when(element.activationCondition()).thenReturn("=testKey==\"otherValue\"");
      when(element.consumeUnmatchedEvents()).thenReturn(false);

      Map<String, Object> variables = Map.of("testKey", "testValue");

      // when & then
      var result = assertDoesNotThrow(() -> handler.correlate(List.of(element), variables));
      verifyNoMoreInteractions(client);
      assertThat(result).isInstanceOf(Failure.ActivationConditionNotMet.class);
      assertThat(((Failure.ActivationConditionNotMet) result).handlingStrategy())
          .isInstanceOf(CorrelationFailureHandlingStrategy.ForwardErrorToUpstream.class);
    }

    @Test
    void activationConditionFalse_strategyIgnore() {
      // given
      var element = mock(InboundConnectorElement.class);
      when(element.activationCondition()).thenReturn("=testKey==\"otherValue\"");
      when(element.consumeUnmatchedEvents()).thenReturn(true);

      Map<String, Object> variables = Map.of("testKey", "testValue");

      // when & then
      var result = assertDoesNotThrow(() -> handler.correlate(List.of(element), variables));
      verifyNoMoreInteractions(client);
      assertThat(result).isInstanceOf(Failure.ActivationConditionNotMet.class);
      assertThat(((Failure) result).handlingStrategy())
          .isInstanceOf(CorrelationFailureHandlingStrategy.Ignore.class);
    }

    @Test
    void activationConditionTrue_shouldCorrelate() {
      // given
      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.activationCondition()).thenReturn("=testKey==\"testValue\"");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue");

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newCreateProcessInstanceCommand();
      assertThat(result).isInstanceOf(Success.ProcessInstanceCreated.class);
    }

    @Test
    void activationConditionNull_shouldCorrelate() {
      // given
      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.activationCondition()).thenReturn(null);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue");

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newCreateProcessInstanceCommand();
      assertThat(result).isInstanceOf(Success.ProcessInstanceCreated.class);
    }

    @Test
    void activationConditionBlank_shouldCorrelate() {
      // given
      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.activationCondition()).thenReturn("  ");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue");

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newCreateProcessInstanceCommand();
      assertThat(result).isInstanceOf(Success.ProcessInstanceCreated.class);
    }

    @Test
    void messageStartEvent_activationConditionTrue_shouldCorrelate() {
      // given
      var dummyCommand = Mockito.spy(new CorrelateMessageCommandDummy());
      when(client.newMessageCorrelationCommand()).thenReturn(dummyCommand);

      var point = new MessageStartEventCorrelationPoint("testMsg", "=myVar", "process1", 1, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.activationCondition()).thenReturn("=myOtherMap.myOtherKey==\"myOtherValue\"");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables =
          Map.of("myVar", "myValue", "myOtherMap", Map.of("myOtherKey", "myOtherValue"));

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newMessageCorrelationCommand();
      assertThat(result).isInstanceOf(Success.MessageCorrelated.class);
    }

    @Test
    void messageStartEvent_activationConditionNull_shouldCorrelate() {
      // given
      var dummyCommand = Mockito.spy(new CorrelateMessageCommandDummy());
      when(client.newMessageCorrelationCommand()).thenReturn(dummyCommand);

      var point = new MessageStartEventCorrelationPoint("testMsg", "=myVar", "process1", 1, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.activationCondition()).thenReturn(null);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables =
          Map.of("myVar", "myValue", "myOtherMap", Map.of("myOtherKey", "myOtherValue"));

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newMessageCorrelationCommand();
      assertThat(result).isInstanceOf(Success.MessageCorrelated.class);
    }

    @Test
    void messageStartEvent_activationConditionBlank_shouldCorrelate() {
      // given
      var dummyCommand = Mockito.spy(new CorrelateMessageCommandDummy());
      when(client.newMessageCorrelationCommand()).thenReturn(dummyCommand);

      var point = new MessageStartEventCorrelationPoint("testMsg", "=myVar", "process1", 1, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.activationCondition()).thenReturn("  ");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables =
          Map.of("myVar", "myValue", "myOtherMap", Map.of("myOtherKey", "myOtherValue"));

      // when
      var result = handler.correlate(List.of(element), variables);

      // then
      verify(client).newMessageCorrelationCommand();
      assertThat(result).isInstanceOf(Success.MessageCorrelated.class);
    }
  }

  @Nested
  @SuppressWarnings("unchecked")
  class ResultVariable_And_ResultExpression {

    @Test
    void noResultVar_noResultExpr_shouldNotCopyVariables() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue");

      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      handler.correlate(List.of(element), variables);

      // then
      var argumentsCaptured = ArgumentCaptor.forClass(Map.class);
      verify(dummyCommand).variables((Map<String, String>) argumentsCaptured.capture());

      assertThat(argumentsCaptured.getValue()).isEmpty();
    }

    @Test
    void resultVarProvided_noResultExpr_shouldCopyAllVarsToResultVar() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.resultVariable()).thenReturn("resultVar");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue");

      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      handler.correlate(List.of(element), variables);

      // then
      var argumentsCaptured = ArgumentCaptor.forClass(Map.class);
      verify(dummyCommand).variables((Map<String, String>) argumentsCaptured.capture());

      assertThat(argumentsCaptured.getValue())
          .containsExactlyEntriesOf(Map.of("resultVar", Map.of("testKey", "testValue")));
    }

    @Test
    void noResultVar_resultExprProvided_shouldExtractVariables() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.resultExpression()).thenReturn("={otherKeyAlias: otherKey}");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue", "otherKey", "otherValue");

      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      handler.correlate(List.of(element), variables);

      // then
      var argumentsCaptured = ArgumentCaptor.forClass(Map.class);
      verify(dummyCommand).variables((Map<String, String>) argumentsCaptured.capture());

      assertThat(argumentsCaptured.getValue())
          .containsExactlyEntriesOf(Map.of("otherKeyAlias", "otherValue"));
    }

    @Test
    void resultVarProvided_resultExprProvided_shouldExtractVarsAndCopyAllVarsToResultVar() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.resultVariable()).thenReturn("resultVar");
      when(element.resultExpression()).thenReturn("={otherKeyAlias: otherKey}");
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> variables = Map.of("testKey", "testValue", "otherKey", "otherValue");

      var dummyCommand = spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      handler.correlate(List.of(element), variables);

      // then
      var argumentsCaptured = ArgumentCaptor.forClass(Map.class);
      verify(dummyCommand).variables((Map<String, String>) argumentsCaptured.capture());

      assertThat(argumentsCaptured.getValue())
          .containsExactlyInAnyOrderEntriesOf(
              Map.of(
                  "resultVar",
                  Map.of(
                      "otherKey", "otherValue",
                      "testKey", "testValue"),
                  "otherKeyAlias",
                  "otherValue"));
    }
  }

  @Nested
  class SynchronousResponse {

    @Test
    void startEvent_synchronous_shouldUseWithResult() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.synchronousResponse()).thenReturn(true);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      Map<String, Object> processVariables = Map.of("test", "value");

      var dummyCommand = Mockito.spy(new CreateCommandDummy(processVariables));
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      var result = handler.correlate(List.of(element), Collections.emptyMap());

      // then
      verify(client).newCreateProcessInstanceCommand();
      verifyNoMoreInteractions(client);
      verify(dummyCommand).processDefinitionKey("process1");
      verify(dummyCommand).version(0);
      verify(dummyCommand).withResult();

      assertThat(result).isInstanceOf(Success.ProcessInstanceCreatedWithResult.class);
      var success = (Success.ProcessInstanceCreatedWithResult) result;
      assertThat(success.activatedElement()).isEqualTo(element.element());
      assertThat(success.processInstanceKey()).isEqualTo(42L);
      assertThat(success.variables()).isEqualTo(processVariables);
    }

    @Test
    void startEvent_asynchronous_shouldNotUseWithResult() {
      // given
      var point = new StartEventCorrelationPoint("process1", 0, 0);
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.synchronousResponse()).thenReturn(false);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      var dummyCommand = Mockito.spy(new CreateCommandDummy());
      when(client.newCreateProcessInstanceCommand()).thenReturn(dummyCommand);

      // when
      var result = handler.correlate(List.of(element), Collections.emptyMap());

      // then
      verify(dummyCommand, Mockito.never()).withResult();
      assertThat(result).isInstanceOf(Success.ProcessInstanceCreated.class);
    }

    @Test
    void message_shouldCorrelateSynchronously() {
      // given
      var correlationKeyValue = "myKey";
      var point = new StandaloneMessageCorrelationPoint("msg1", "=correlationKey");
      var element = mock(InboundConnectorElement.class);
      when(element.correlationPoint()).thenReturn(point);
      when(element.element())
          .thenReturn(new ProcessElementWithRuntimeData("process1", 0, 0, "element", "default"));

      var dummyCommand = Mockito.spy(new CorrelateMessageCommandDummy());
      when(client.newMessageCorrelationCommand()).thenReturn(dummyCommand);

      // when
      var result =
          handler.correlate(List.of(element), Map.of("correlationKey", correlationKeyValue));

      // then
      verify(client).newMessageCorrelationCommand();
      verifyNoMoreInteractions(client);
      verify(dummyCommand).messageName("msg1");
      verify(dummyCommand).correlationKey(correlationKeyValue);
      verify(dummyCommand).send();

      assertThat(result).isInstanceOf(Success.MessageCorrelated.class);
      var success = (Success.MessageCorrelated) result;
      assertThat(success.activatedElement()).isEqualTo(element.element());
      assertThat(success.messageKey()).isEqualTo(-1L);
    }
  }
}
