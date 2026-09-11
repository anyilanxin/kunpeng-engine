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

public interface KunpengPriorityDefinition extends BpmnModelElementInstance {
  /**
   * Kunpeng User Task priority is defined as a number between 0 and 100. The default assigned
   * priority is 50.
   */
  String DEFAULT_LITERAL_PRIORITY = "50";

  Integer DEFAULT_NUMBER_PRIORITY = Integer.parseInt(DEFAULT_LITERAL_PRIORITY);

  String getPriority();

  void setPriority(String priority);
}
