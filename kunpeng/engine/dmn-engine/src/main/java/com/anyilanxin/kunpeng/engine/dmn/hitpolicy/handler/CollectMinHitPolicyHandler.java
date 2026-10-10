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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler;

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.AbstractCollectNumberHitPolicyHandler;
import java.util.Collections;
import java.util.List;

public class CollectMinHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  @Override
  public HitPolicyType getHitPolicy() {
    return HitPolicyType.COLLECT_MIN;
  }

  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.MIN;
  }

  @Override
  protected Integer aggregateIntegerValues(final List<Integer> intValues) {
    return Collections.min(intValues);
  }

  @Override
  protected Long aggregateLongValues(final List<Long> longValues) {
    return Collections.min(longValues);
  }

  @Override
  protected Double aggregateDoubleValues(final List<Double> doubleValues) {
    return Collections.min(doubleValues);
  }

  @Override
  public String toString() {
    return "CollectMinHitPolicyHandler{}";
  }
}
