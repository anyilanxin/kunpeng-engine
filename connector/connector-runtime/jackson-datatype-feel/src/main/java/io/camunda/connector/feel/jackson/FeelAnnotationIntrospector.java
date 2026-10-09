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

import io.camunda.connector.api.annotation.Expression;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class FeelAnnotationIntrospector extends JacksonAnnotationIntrospector {

  private final FeelExpressionEvaluator evaluator;

  /**
   * Creates an introspector with the specified Expression expression evaluator.
   *
   * @param evaluator the Expression expression evaluator to use
   */
  public FeelAnnotationIntrospector(FeelExpressionEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  public Object findDeserializer(MapperConfig<?> config, Annotated a) {
    Expression ann = _findAnnotation(a, Expression.class);
    if (ann != null) {
      return new FeelDeserializer(evaluator);
    }
    return super.findDeserializer(config, a);
  }
}
