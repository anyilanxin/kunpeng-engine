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
package com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;

public interface KunpengPublishMessage extends BpmnModelElementInstance {

  /**
   * @return the correlation key of the message
   */
  String getCorrelationKey();

  /**
   * Sets the correlation key of the message.
   *
   * @param correlationKey the correlation key of the message
   */
  void setCorrelationKey(String correlationKey);

  /**
   * @return the time to live of the message
   */
  String getTimeToLive();

  /**
   * Sets the time to live of the message.
   *
   * @param timeToLive the time to live of the message
   */
  void setTimeToLive(final String timeToLive);
}
