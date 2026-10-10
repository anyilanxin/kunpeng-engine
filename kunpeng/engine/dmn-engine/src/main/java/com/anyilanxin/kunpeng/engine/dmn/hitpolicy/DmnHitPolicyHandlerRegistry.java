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

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;

/** Registry of hit policy handlers */
public interface DmnHitPolicyHandlerRegistry {

  /**
   * Get a hit policy for a {@link HitPolicy} and {@link BuiltinAggregator} combination.
   *
   * @param hitPolicy the hit policy
   * @return the handler which is registered for this hit policy, or null if none exist
   */
  DmnHitPolicyHandler getHandler(HitPolicyType hitPolicy);

  /**
   * Register a hit policy handler for a {@link HitPolicy} and {@link BuiltinAggregator}
   * combination.
   *
   * @param hitPolicyHandler the hit policy handler to registry
   */
  DmnHitPolicyHandlerRegistry addHandler(DmnHitPolicyHandler hitPolicyHandler);
}
