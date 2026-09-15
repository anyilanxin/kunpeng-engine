/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler;

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.AbstractCollectNumberHitPolicyHandler;
import java.util.List;

public class CollectSumHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {

  @Override
  public HitPolicyType getHitPolicy() {
    return HitPolicyType.COLLECT_SUM;
  }

  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.SUM;
  }

  @Override
  protected Integer aggregateIntegerValues(final List<Integer> intValues) {
    int sum = 0;
    for (final Integer intValue : intValues) {
      if (intValue != null) {
        sum += intValue;
      }
    }
    return sum;
  }

  @Override
  protected Long aggregateLongValues(final List<Long> longValues) {
    long sum = 0L;
    for (final Long longValue : longValues) {
      if (longValue != null) {
        sum += longValue;
      }
    }
    return sum;
  }

  @Override
  protected Double aggregateDoubleValues(final List<Double> doubleValues) {
    double sum = 0.0;
    for (final Double doubleValue : doubleValues) {
      if (doubleValue != null) {
        sum += doubleValue;
      }
    }
    return sum;
  }

  @Override
  public String toString() {
    return "CollectSumHitPolicyHandler{}";
  }
}
