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

/** A Kunpeng extension for an ad-hoc sub-process. */
public interface KunpengAdHoc extends BpmnModelElementInstance {

  /**
   * @return the collection of elements that should be activated when entering the ad-hoc
   *     sub-process.
   */
  String getActiveElementsCollection();

  /**
   * Sets the collection of elements that should be activated when entering the ad-hoc sub-process.
   *
   * @param activateElements the collection of element to be activated
   */
  void setActiveElementsCollection(final String activateElements);
}
