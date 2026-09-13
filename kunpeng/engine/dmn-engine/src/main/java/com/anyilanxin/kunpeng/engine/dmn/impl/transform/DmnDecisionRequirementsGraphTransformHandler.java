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

import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionRequirementsGraphImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Definitions;

public class DmnDecisionRequirementsGraphTransformHandler
    implements DmnElementTransformHandler<Definitions, DmnDecisionRequirementsGraphImpl> {

  public DmnDecisionRequirementsGraphImpl handleElement(
    final DmnElementTransformContext context, final Definitions definitions) {
    return createFromDefinitions(context, definitions);
  }

  protected DmnDecisionRequirementsGraphImpl createFromDefinitions(
    final DmnElementTransformContext context, final Definitions definitions) {
    final DmnDecisionRequirementsGraphImpl drd = createDmnElement();

    drd.setKey(definitions.getId());
    drd.setName(definitions.getName());

    return drd;
  }

  protected DmnDecisionRequirementsGraphImpl createDmnElement() {
    return new DmnDecisionRequirementsGraphImpl();
  }
}
