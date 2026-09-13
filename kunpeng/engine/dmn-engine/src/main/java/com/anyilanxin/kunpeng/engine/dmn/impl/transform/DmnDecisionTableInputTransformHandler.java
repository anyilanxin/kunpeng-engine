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

import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Input;

public class DmnDecisionTableInputTransformHandler
    implements DmnElementTransformHandler<Input, DmnDecisionTableInputImpl> {

  public DmnDecisionTableInputImpl handleElement(final DmnElementTransformContext context, final Input input) {
    return createFromInput(context, input);
  }

  protected DmnDecisionTableInputImpl createFromInput(
    final DmnElementTransformContext context, final Input input) {
    final DmnDecisionTableInputImpl decisionTableInput = createDmnElement(context, input);

    decisionTableInput.setId(input.getId());
    decisionTableInput.setName(input.getLabel());
    decisionTableInput.setInputVariable(input.getCamundaInputVariable());

    return decisionTableInput;
  }

  protected DmnDecisionTableInputImpl createDmnElement(
    final DmnElementTransformContext context, final Input input) {
    return new DmnDecisionTableInputImpl();
  }
}
