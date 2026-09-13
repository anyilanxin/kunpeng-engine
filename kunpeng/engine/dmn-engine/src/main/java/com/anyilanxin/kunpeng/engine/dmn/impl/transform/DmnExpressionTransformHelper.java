/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.dmn.impl.transform;

import com.anyilanxin.kunpeng.engine.dmn.impl.DefaultDmnEngineConfiguration;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.type.DmnDataTypeTransformer;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.engine.dmn.impl.type.DefaultTypeDefinition;
import com.anyilanxin.kunpeng.engine.dmn.impl.type.DmnTypeDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InformationItem;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.LiteralExpression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Text;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.UnaryTests;

public class DmnExpressionTransformHelper {

  public static DmnTypeDefinition createTypeDefinition(
    final DmnElementTransformContext context, final LiteralExpression expression) {
    return createTypeDefinition(context, expression.getTypeRef());
  }

  public static DmnTypeDefinition createTypeDefinition(
    final DmnElementTransformContext context, final InformationItem informationItem) {
    return createTypeDefinition(context, informationItem.getTypeRef());
  }

  protected static DmnTypeDefinition createTypeDefinition(
    final DmnElementTransformContext context, final String typeRef) {
    if (typeRef != null) {
      final DmnDataTypeTransformer transformer =
          context.getDataTypeTransformerRegistry().getTransformer(typeRef);
      return new DmnTypeDefinitionImpl(typeRef, transformer);
    } else {
      return new DefaultTypeDefinition();
    }
  }

  public static String getExpressionLanguage(
    final DmnElementTransformContext context, final LiteralExpression expression) {
    return getExpressionLanguage(context, expression.getExpressionLanguage());
  }

  public static String getExpressionLanguage(
    final DmnElementTransformContext context, final UnaryTests expression) {
    return getExpressionLanguage(context, expression.getExpressionLanguage());
  }

  protected static String getExpressionLanguage(
    final DmnElementTransformContext context, final String expressionLanguage) {
    if (expressionLanguage != null) {
      return expressionLanguage;
    } else {
      return getGlobalExpressionLanguage(context);
    }
  }

  protected static String getGlobalExpressionLanguage(final DmnElementTransformContext context) {
    final String expressionLanguage = context.getModelInstance().getDefinitions().getExpressionLanguage();
    if (!DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE.equals(expressionLanguage)
        && !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN12.equals(expressionLanguage)
        && !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN13.equals(expressionLanguage)
        && !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN14.equals(expressionLanguage)
        && !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN15.equals(
            expressionLanguage)) {
      return expressionLanguage;
    } else {
      return null;
    }
  }

  public static String getExpression(final LiteralExpression expression) {
    return getExpression(expression.getText());
  }

  public static String getExpression(final UnaryTests expression) {
    return getExpression(expression.getText());
  }

  protected static String getExpression(final Text text) {
    if (text != null) {
      final String textContent = text.getTextContent();
      if (textContent != null && !textContent.isEmpty()) {
        return textContent;
      }
    }
    return null;
  }
}
