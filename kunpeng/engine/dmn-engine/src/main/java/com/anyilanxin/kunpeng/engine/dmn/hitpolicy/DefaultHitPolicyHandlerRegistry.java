/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler.*;
import java.util.HashMap;
import java.util.Map;

public class DefaultHitPolicyHandlerRegistry implements DmnHitPolicyHandlerRegistry {
  protected static final Map<HitPolicyType, DmnHitPolicyHandler> handlers = getDefaultHandlers();

  protected static Map<HitPolicyType, DmnHitPolicyHandler> getDefaultHandlers() {
    final Map<HitPolicyType, DmnHitPolicyHandler> handlers = new HashMap<>();
    register(handlers, new UniqueHitPolicyHandler());
    register(handlers, new FirstHitPolicyHandler());
    register(handlers, new AnyHitPolicyHandler());
    register(handlers, new RuleOrderHitPolicyHandler());
    register(handlers, new CollectHitPolicyHandler());
    register(handlers, new CollectCountHitPolicyHandler());
    register(handlers, new CollectSumHitPolicyHandler());
    register(handlers, new CollectMinHitPolicyHandler());
    register(handlers, new CollectMaxHitPolicyHandler());
    return handlers;
  }

  private static void register(
      final Map<HitPolicyType, DmnHitPolicyHandler> handlers,
      final DmnHitPolicyHandler policyHandler) {
    handlers.put(policyHandler.getHitPolicy(), policyHandler);
  }

  @Override
  public DmnHitPolicyHandler getHandler(final HitPolicyType hitPolicy) {
    return handlers.get(hitPolicy);
  }

  @Override
  public DefaultHitPolicyHandlerRegistry addHandler(final DmnHitPolicyHandler hitPolicyHandler) {
    handlers.put(hitPolicyHandler.getHitPolicy(), hitPolicyHandler);
    return this;
  }
}
