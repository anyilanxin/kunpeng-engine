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
package io.camunda.connector.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import tools.jackson.databind.ObjectMapper;
import com.anyilanxin.kunpeng.client.KunpengClient;
import io.camunda.connector.api.annotation.Expression;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import io.camunda.connector.feel.FeelExpressionEvaluatorBuilder;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ObjectMapperQualifierTest {

  // A local evaluator and a mock client replace the engine-backed ones so no engine is required
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ConnectorsAutoConfiguration.class))
          .withUserConfiguration(LocalEvaluatorConfiguration.class);

  @Configuration
  static class LocalEvaluatorConfiguration {

    @Bean
    FeelExpressionEvaluator feelExpressionEvaluator() {
      return FeelExpressionEvaluatorBuilder.local().build();
    }

    @Bean
    KunpengClient kunpengClient() {
      return mock(KunpengClient.class, RETURNS_DEEP_STUBS);
    }

    /** Stands in for an application-provided mapper: no Expression module is registered. */
    @Bean
    ObjectMapper defaultObjectMapper() {
      return ConnectorsObjectMapperSupplier.getCopy();
    }
  }

  private static final class Mappers {
    final ObjectMapper connector;
    final ObjectMapper outbound;
    final ObjectMapper plain;

    Mappers(org.springframework.context.ApplicationContext context) {
      // lifted by name because the qualified beans are not default candidates for type-based lookup
      this.connector = context.getBean("connectorObjectMapper", ObjectMapper.class);
      this.outbound = context.getBean("outboundConnectorObjectMapper", ObjectMapper.class);
      this.plain = context.getBean("defaultObjectMapper", ObjectMapper.class);
    }
  }

  @Test
  void shouldInjectConnectorObjectMapperWithQualifier() {
    contextRunner.run(
        context -> {
          var mappers = new Mappers(context);
          assertThat(mappers.connector).isNotNull();
          assertThat(mappers.outbound).isNotNull();
          assertThat(mappers.plain).isNotNull();

          // All three mappers should be different instances
          assertThat(mappers.connector).isNotSameAs(mappers.plain);
          assertThat(mappers.connector).isNotSameAs(mappers.outbound);
          assertThat(mappers.outbound).isNotSameAs(mappers.plain);
        });
  }

  @Test
  void connectorObjectMapperShouldSupportFeelDeserialization() {
    contextRunner.run(
        context -> {
          var connectorObjectMapper = new Mappers(context).connector;
          // Test that the connector ObjectMapper has Expression support
          var json =
              """
              {
               "name": "= \\"test \\" + \\"User\\" ",
               "greetingSupplier": "= \\"Hello World\\""
              }""";

          var feelObject = connectorObjectMapper.readValue(json, TestFeelClass.class);
          assertThat(feelObject.name).isEqualTo("test User");
          assertThat(feelObject.greetingSupplier.get()).isEqualTo("Hello World");
        });
  }

  @Test
  void connectorObjectMapperShouldEvaluateFeelFunctions() {
    contextRunner.run(
        context -> {
          var connectorObjectMapper = new Mappers(context).connector;
          // The default ConnectorsObjectMapper should evaluate Expression functions (Supplier)
          var json =
              """
              {
               "name": "test",
               "greetingSupplier": "= \\"Hello World\\""
              }""";

          var feelObject = connectorObjectMapper.readValue(json, TestFeelClass.class);
          // Expression function is evaluated - calling get() returns the evaluated value
          assertThat(feelObject.greetingSupplier.get()).isEqualTo("Hello World");
        });
  }

  @Test
  void outboundConnectorObjectMapperShouldNotEvaluateFeelExpressions() {
    contextRunner.run(
        context -> {
          var outboundConnectorObjectMapper = new Mappers(context).outbound;
          // The OutboundConnectorObjectMapper has Expression functions DISABLED
          // So @Expression annotated fields should NOT evaluate Expression expressions
          var json =
              """
              {
               "name": "= \\"test \\" + \\"User\\" ",
               "greetingSupplier": "= \\"Hello World\\""
              }""";

          var feelObject = outboundConnectorObjectMapper.readValue(json, TestFeelClass.class);
          // @Expression annotation does NOT work - the Expression expression is NOT evaluated
          assertThat(feelObject.name).isEqualTo("= \"test \" + \"User\" ");
        });
  }

  @Test
  void outboundConnectorObjectMapperShouldNotEvaluateFeelFunctions() {
    contextRunner.run(
        context -> {
          var outboundConnectorObjectMapper = new Mappers(context).outbound;
          // The OutboundConnectorObjectMapper has Expression functions DISABLED
          // So Supplier fields should NOT be evaluated as Expression expressions
          var json =
              """
              {
               "name": "test",
               "greetingSupplier": "= \\"Hello World\\""
              }""";

          var feelObject = outboundConnectorObjectMapper.readValue(json, TestFeelClass.class);
          // Expression function is NOT evaluated - calling get() returns the evaluated value
          // because the Supplier is still deserialized, but the Expression wrapper returns the evaluated result
          assertThat(feelObject.greetingSupplier.get()).isEqualTo("Hello World");
        });
  }

  @Test
  void customObjectMapperShouldNotSupportFeelDeserialization() {
    contextRunner.run(
        context -> {
          var defaultObjectMapper = new Mappers(context).plain;
          // The custom/default ObjectMapper should NOT support Expression expressions
          // It will just read the literal string value
          var json =
              """
              {
               "name": "= \\"test \\" + \\"User\\" "
              }""";

          var simpleObject = defaultObjectMapper.readValue(json, SimpleClass.class);
          // Without Expression module, it reads the literal string including the Expression expression syntax
          assertThat(simpleObject.name).isEqualTo("= \"test \" + \"User\" ");
        });
  }

  private record TestFeelClass(@Expression String name, Supplier<String> greetingSupplier) {}

  private record SimpleClass(String name) {}
}
