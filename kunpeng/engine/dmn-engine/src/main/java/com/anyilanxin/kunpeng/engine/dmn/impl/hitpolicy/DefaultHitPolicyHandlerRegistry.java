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
package com.anyilanxin.kunpeng.engine.dmn.impl.hitpolicy;

import java.util.HashMap;
import java.util.Map;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.hitpolicy.DmnHitPolicyHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;

public class DefaultHitPolicyHandlerRegistry implements DmnHitPolicyHandlerRegistry {

  protected static final Map<HitPolicyEntry, DmnHitPolicyHandler> handlers = getDefaultHandlers();

  protected static Map<HitPolicyEntry, DmnHitPolicyHandler> getDefaultHandlers() {
    final Map<HitPolicyEntry, DmnHitPolicyHandler> handlers =
        new HashMap<HitPolicyEntry, DmnHitPolicyHandler>();

    handlers.put(new HitPolicyEntry(HitPolicy.UNIQUE, null), new UniqueHitPolicyHandler());
    handlers.put(new HitPolicyEntry(HitPolicy.FIRST, null), new FirstHitPolicyHandler());
    handlers.put(new HitPolicyEntry(HitPolicy.ANY, null), new AnyHitPolicyHandler());
    handlers.put(new HitPolicyEntry(HitPolicy.RULE_ORDER, null), new RuleOrderHitPolicyHandler());
    handlers.put(new HitPolicyEntry(HitPolicy.COLLECT, null), new CollectHitPolicyHandler());
    handlers.put(
        new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.COUNT),
        new CollectCountHitPolicyHandler());
    handlers.put(
        new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.SUM),
        new CollectSumHitPolicyHandler());
    handlers.put(
        new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MIN),
        new CollectMinHitPolicyHandler());
    handlers.put(
        new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MAX),
        new CollectMaxHitPolicyHandler());

    return handlers;
  }

  public DmnHitPolicyHandler getHandler(final HitPolicy hitPolicy, final BuiltinAggregator builtinAggregator) {
    return handlers.get(new HitPolicyEntry(hitPolicy, builtinAggregator));
  }

  public void addHandler(
    final HitPolicy hitPolicy,
    final BuiltinAggregator builtinAggregator,
    final DmnHitPolicyHandler hitPolicyHandler) {
    handlers.put(new HitPolicyEntry(hitPolicy, builtinAggregator), hitPolicyHandler);
  }
}
