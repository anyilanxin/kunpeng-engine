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

import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.type.DmnDataTypeTransformer;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.engine.dmn.impl.type.DefaultTypeDefinition;
import com.anyilanxin.kunpeng.engine.dmn.impl.type.DmnTypeDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Output;

public class DmnDecisionTableOutputTransformHandler
    implements DmnElementTransformHandler<Output, DmnDecisionTableOutputImpl> {

  public DmnDecisionTableOutputImpl handleElement(
    final DmnElementTransformContext context, final Output output) {
    return createFromOutput(context, output);
  }

  protected DmnDecisionTableOutputImpl createFromOutput(
    final DmnElementTransformContext context, final Output output) {
    final DmnDecisionTableOutputImpl decisionTableOutput = createDmnElement(context, output);

    decisionTableOutput.setId(output.getId());
    decisionTableOutput.setName(output.getLabel());
    decisionTableOutput.setOutputName(output.getName());
    decisionTableOutput.setTypeDefinition(getTypeDefinition(context, output));

    return decisionTableOutput;
  }

  protected DmnDecisionTableOutputImpl createDmnElement(
    final DmnElementTransformContext context, final Output output) {
    return new DmnDecisionTableOutputImpl();
  }

  protected DmnTypeDefinition getTypeDefinition(final DmnElementTransformContext context, final Output output) {
    final String typeRef = output.getTypeRef();
    if (typeRef != null) {
      final DmnDataTypeTransformer transformer =
          context.getDataTypeTransformerRegistry().getTransformer(typeRef);
      return new DmnTypeDefinitionImpl(typeRef, transformer);
    } else {
      return new DefaultTypeDefinition();
    }
  }
}
