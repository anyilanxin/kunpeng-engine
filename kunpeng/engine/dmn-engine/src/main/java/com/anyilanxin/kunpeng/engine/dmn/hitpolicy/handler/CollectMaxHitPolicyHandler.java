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
import java.util.Collections;
import java.util.List;

public class CollectMaxHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  @Override
  public HitPolicyType getHitPolicy() {
    return HitPolicyType.COLLECT_MAX;
  }

  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.MAX;
  }

  @Override
  protected Integer aggregateIntegerValues(final List<Integer> intValues) {
    return Collections.max(intValues);
  }

  @Override
  protected Long aggregateLongValues(final List<Long> longValues) {
    return Collections.max(longValues);
  }

  @Override
  protected Double aggregateDoubleValues(final List<Double> doubleValues) {
    return Collections.max(doubleValues);
  }

  @Override
  public String toString() {
    return "CollectMaxHitPolicyHandler{}";
  }
}
