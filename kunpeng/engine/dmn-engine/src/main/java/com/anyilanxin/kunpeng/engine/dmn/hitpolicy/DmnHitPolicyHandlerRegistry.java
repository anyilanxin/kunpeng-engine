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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;

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
