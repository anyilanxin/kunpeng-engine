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
package io.camunda.connector.feel.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class FeelFunctionDeserializerTest {

  private final ObjectMapper mapper =
      JsonMapper.builder().addModule(new JacksonModuleFeelFunction()).build();

  @Test
  void feelFunctionDeserialization_objectResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= { result: a + b }" }
        """;

    // when
    TargetTypeObject targetType = mapper.readValue(json, TargetTypeObject.class);

    // then
    InputContextString inputContext = new InputContextString("foo", "bar");
    OutputContext result = targetType.function().apply(inputContext);
    assertThat(result).isInstanceOf(OutputContext.class);
    assertThat(result.result).isEqualTo("foobar");
  }

  @Test
  void feelFunctionDeserialization_stringResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= a + b" }
        """;

    // when
    TargetTypeString targetType = mapper.readValue(json, TargetTypeString.class);

    // then
    InputContextString inputContext = new InputContextString("foo", "bar");
    String result = targetType.function().apply(inputContext);
    assertThat(result).isEqualTo("foobar");
  }

  @Test
  void feelFunctionDeserialization_booleanResult() throws JacksonException {
    // given: QL equality is '==', not FEEL's single '='
    String json =
        """
        { "function": "= a == b" }
        """;

    // when
    TargetTypeBoolean targetType = mapper.readValue(json, TargetTypeBoolean.class);

    // then
    InputContextString inputContext = new InputContextString("foo", "bar");
    Boolean result = targetType.function().apply(inputContext);
    assertThat(result).isFalse();
  }

  @Test
  void feelFunctionDeserialization_integerResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= a + b" }
        """;

    // when
    TargetTypeInteger targetType = mapper.readValue(json, TargetTypeInteger.class);

    // then
    InputContextInteger inputContext = new InputContextInteger(3, 5);
    Integer result = targetType.function().apply(inputContext);
    assertThat(result).isEqualTo(8);
  }

  @Test
  void feelFunctionDeserialization_nullResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= null" }
        """;

    // when
    TargetTypeObject targetType = mapper.readValue(json, TargetTypeObject.class);

    // then
    InputContextString inputContext = new InputContextString("foo", "bar");
    Object result = targetType.function().apply(inputContext);
    assertThat(result).isNull();
  }

  @Test
  void feelSupplierDeserialization_listResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= [a, b]" }
        """;

    // when
    TargetTypeList targetType = mapper.readValue(json, TargetTypeList.class);

    // then
    InputContextInteger inputContext = new InputContextInteger(3, 5);
    List<Long> result = targetType.function().apply(inputContext);
    assertThat(result).containsExactlyElementsOf(List.of(3L, 5L));
  }

  @Test
  void feelSupplierDeserialization_mapResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= { foo: a + b }" }
        """;

    // when
    TargetTypeMap targetType = mapper.readValue(json, TargetTypeMap.class);

    // then
    InputContextInteger inputContext = new InputContextInteger(3, 5);
    Map<String, Long> result = targetType.function().apply(inputContext);
    assertThat(result).containsEntry("foo", 8L);
  }

  @Test
  void feelSupplierDeserialization_foldedMapResult() throws JacksonException {
    // given
    String json =
        """
        { "function": "= { foo: {bar: a + b, baz: b - a} }" }
        """;

    // when
    TargetTypeFoldedMap targetType = mapper.readValue(json, TargetTypeFoldedMap.class);

    // then
    InputContextInteger inputContext = new InputContextInteger(3, 5);
    var result = targetType.function().apply(inputContext);
    assertThat(result).containsKey("foo");
    // QL arithmetic yields Integer, not FEEL's Long
    assertThat((Map) result.get("foo")).containsEntry("bar", 8);
    assertThat((Map) result.get("foo")).containsEntry("baz", 2);
  }

  @Test
  void feelFunctionDeserialization_convertFromMap() {
    // given
    var jsonAsMap = Map.of("function", "= { result: a + b }");

    // when
    TargetTypeObject targetType = mapper.convertValue(jsonAsMap, TargetTypeObject.class);

    // then
    InputContextString inputContext = new InputContextString("foo", "bar");
    OutputContext result = targetType.function().apply(inputContext);
    assertThat(result).isInstanceOf(OutputContext.class);
    assertThat(result.result).isEqualTo("foobar");
  }

  @Test
  void feelFunctionDeserialization_contextAware_mergedWithInput() throws IOException {
    // given
    var json =
        """
        { "function": "= { result: a + c }" }
        """;
    var contextualReader =
        FeelContextAwareObjectReader.of(mapper)
            .withStaticContext(Map.of("c", "bar"))
            .forType(TargetTypeObject.class);

    // when
    TargetTypeObject targetType = contextualReader.readValue(json);

    // then
    InputContextString inputContext = new InputContextString("foo", "some value");
    OutputContext result = targetType.function().apply(inputContext);
    assertThat(result).isInstanceOf(OutputContext.class);
    assertThat(result.result).isEqualTo("foobar");
  }

  @Test
  void feelFunctionDeserialization_withEvaluatorOverride_usesFunctionEvaluator()
      throws IOException {
    // given
    var json =
        """
        { "function": "= { result: a + b }" }
        """;
    var contextualReader =
        FeelContextAwareObjectReader.of(mapper)
            .withEvaluator(new ThrowingFeelExpressionEvaluator())
            .forType(TargetTypeObject.class);

    // when
    TargetTypeObject targetType = contextualReader.readValue(json);

    // then
    InputContextString inputContext = new InputContextString("foo", "bar");
    OutputContext result = targetType.function().apply(inputContext);
    assertThat(result).isInstanceOf(OutputContext.class);
    assertThat(result.result).isEqualTo("foobar");
  }

  @Test
  void feelFunctionDeserialization_contextAware_knowsJava8Time() throws IOException {
    // given: the QL dialect has no date()/string() functions, so a LocalDate is injected
    // through the reader context instead
    var json =
        """
        { "function": "= d" }
        """;
    var contextualReader =
        FeelContextAwareObjectReader.of(mapper)
            .withStaticContext(Map.of("d", LocalDate.of(2021, 1, 1)))
            .forType(TargetTypeJava8Time.class);

    // when
    TargetTypeJava8Time targetType = contextualReader.readValue(json);

    // then
    InputContextInteger inputContext = new InputContextInteger(3, 5);
    LocalDate result = targetType.function().apply(inputContext);
    assertThat(result).isEqualTo(LocalDate.of(2021, 1, 1));
  }

  private record InputContextString(String a, String b) {}

  private record InputContextInteger(Integer a, Integer b) {}

  private record OutputContext(String result) {}

  private record TargetTypeObject(Function<InputContextString, OutputContext> function) {}

  private record TargetTypeString(Function<InputContextString, String> function) {}

  private record TargetTypeBoolean(Function<InputContextString, Boolean> function) {}

  private record TargetTypeInteger(Function<InputContextInteger, Integer> function) {}

  private record TargetTypeList(Function<InputContextInteger, List<Long>> function) {}

  private record TargetTypeMap(Function<InputContextInteger, Map<String, Long>> function) {}

  private record TargetTypeFoldedMap(Function<InputContextInteger, Map<String, Object>> function) {}

  private record TargetTypeJava8Time(Function<InputContextInteger, LocalDate> function) {}

  private static class ThrowingFeelExpressionEvaluator implements FeelExpressionEvaluator {

    @Override
    public <T> T evaluate(String expression, Object... variables) {
      throw new AssertionError("Function deserialization must use the module's function evaluator");
    }

    @Override
    public <T> T evaluate(String expression, Class<T> targetType, Object... variables) {
      throw new AssertionError("Function deserialization must use the module's function evaluator");
    }

    @Override
    public <T> T evaluate(String expression, JavaType targetType, Object... variables) {
      throw new AssertionError("Function deserialization must use the module's function evaluator");
    }

    @Override
    public String evaluateToJson(String expression, Object... variables) {
      throw new AssertionError("Function deserialization must use the module's function evaluator");
    }
  }
}
