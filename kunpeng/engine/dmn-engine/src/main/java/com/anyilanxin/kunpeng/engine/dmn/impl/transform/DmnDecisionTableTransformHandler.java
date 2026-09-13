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

import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnLogger;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.hitpolicy.DmnHitPolicyHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DecisionTable;

public class DmnDecisionTableTransformHandler
    implements DmnElementTransformHandler<DecisionTable, DmnDecisionTableImpl> {

  protected static final DmnTransformLogger LOG = DmnLogger.TRANSFORM_LOGGER;

  public DmnDecisionTableImpl handleElement(
    final DmnElementTransformContext context, final DecisionTable decisionTable) {
    return createFromDecisionTable(context, decisionTable);
  }

  protected DmnDecisionTableImpl createFromDecisionTable(
    final DmnElementTransformContext context, final DecisionTable decisionTable) {
    final DmnDecisionTableImpl dmnDecisionTable = createDmnElement(context, decisionTable);

    dmnDecisionTable.setHitPolicyHandler(
        getHitPolicyHandler(context, decisionTable, dmnDecisionTable));

    return dmnDecisionTable;
  }

  protected DmnDecisionTableImpl createDmnElement(
    final DmnElementTransformContext context, final DecisionTable decisionTable) {
    return new DmnDecisionTableImpl();
  }

  protected DmnHitPolicyHandler getHitPolicyHandler(
    final DmnElementTransformContext context,
    final DecisionTable decisionTable,
    final DmnDecisionTableImpl dmnDecisionTable) {
    HitPolicy hitPolicy = decisionTable.getHitPolicy();
    if (hitPolicy == null) {
      // use default hit policy
      hitPolicy = HitPolicy.UNIQUE;
    }
    final BuiltinAggregator aggregation = decisionTable.getAggregation();
    final DmnHitPolicyHandler hitPolicyHandler =
        context.getHitPolicyHandlerRegistry().getHandler(hitPolicy, aggregation);
    if (hitPolicyHandler != null) {
      return hitPolicyHandler;
    } else {
      throw LOG.hitPolicyNotSupported(dmnDecisionTable, hitPolicy, aggregation);
    }
  }
}
