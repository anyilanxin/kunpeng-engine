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
package io.camunda.connector.api.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Marks a property that needs to be deserialized as Expression expression.
 *
 * <p>Consider the following properties class:
 *
 * <pre>
 *   record MyInboundConnectorProperties({@literal @}Expression private String property) {}
 * </pre>
 *
 * Raw connector properties contain a Expression expression, e.g.:
 *
 * <pre>
 *   { "property": "=\"foo\" + \"bar\"" }
 * </pre>
 *
 * Then the property is deserialized as a Expression expression when the connector is executed.
 *
 * <pre>
 *   MyInboundConnectorProperties properties = ctx.bindProperties(MyInboundConnectorProperties.class);
 *   properties.property(); // returns "foobar"
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@JacksonAnnotationsInside
@JsonDeserialize
public @interface Expression {}
