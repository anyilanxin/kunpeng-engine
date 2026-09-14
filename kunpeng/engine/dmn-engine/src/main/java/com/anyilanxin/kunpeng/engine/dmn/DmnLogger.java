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

package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler.DmnHitPolicyLogger;
import com.anyilanxin.kunpeng.engine.dmn.util.BaseLogger;

public class DmnLogger extends BaseLogger {

  public static final String PROJECT_CODE = "DMN";
  public static final String PROJECT_LOGGER = "org.camunda.bpm.dmn";

  public static DmnEngineLogger ENGINE_LOGGER =
      createLogger(DmnEngineLogger.class, PROJECT_CODE, PROJECT_LOGGER, "01");

  public static DmnHitPolicyLogger HIT_POLICY_LOGGER =
      createLogger(DmnHitPolicyLogger.class, PROJECT_CODE, PROJECT_LOGGER + ".hitPolicy", "03");
}
