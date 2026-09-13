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

import static com.anyilanxin.kunpeng.engine.dmn.impl.transform.DmnExpressionTransformHelper.getExpression;
import static com.anyilanxin.kunpeng.engine.dmn.impl.transform.DmnExpressionTransformHelper.getExpressionLanguage;

import com.anyilanxin.kunpeng.engine.dmn.impl.DmnExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputEntry;

public class DmnDecisionTableConditionTransformHandler
    implements DmnElementTransformHandler<InputEntry, DmnExpressionImpl> {

  public DmnExpressionImpl handleElement(
    final DmnElementTransformContext context, final InputEntry inputEntry) {
    return createFromInputEntry(context, inputEntry);
  }

  protected DmnExpressionImpl createFromInputEntry(
    final DmnElementTransformContext context, final InputEntry inputEntry) {
    final DmnExpressionImpl condition = createDmnElement(context, inputEntry);

    condition.setId(inputEntry.getId());
    condition.setName(inputEntry.getLabel());
    condition.setExpressionLanguage(getExpressionLanguage(context, inputEntry));
    condition.setExpression(getExpression(inputEntry));

    return condition;
  }

  protected DmnExpressionImpl createDmnElement(
    final DmnElementTransformContext context, final InputEntry inputEntry) {
    return new DmnExpressionImpl();
  }
}
