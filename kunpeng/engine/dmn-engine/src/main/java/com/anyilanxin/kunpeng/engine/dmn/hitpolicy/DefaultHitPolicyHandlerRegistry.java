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
