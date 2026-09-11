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

/**
 * Represents the various event types for `task listeners` in a BPMN workflow. Task listeners allow
 * users to execute custom logic during specific lifecycle events of a user task in a process.
 *
 * <ul>
 *   <li>{@code create} - Triggered when the user task is created.
 *   <li>{@code assignment} - Triggered when the user task is assigned/claimed or unassigned.
 *   <li>{@code update} - Triggered when the user task details are updated.
 *   <li>{@code complete} - Triggered when the user task is completed.
 *   <li>{@code cancel} - Triggered when the user task is canceled.
 * </ul>
 */
public enum KunpengTaskListenerEventType {
  /**
   * @deprecated use {@link #creating} instead
   */
  @Deprecated
  create,

  /**
   * @deprecated use {@link #assigning} instead
   */
  @Deprecated
  assignment,

  /**
   * @deprecated use {@link #updating} instead
   */
  @Deprecated
  update,

  /**
   * @deprecated use {@link #completing} instead
   */
  @Deprecated
  complete,

  /**
   * @deprecated use {@link #canceling} instead
   */
  @Deprecated
  cancel,

  creating,
  assigning,
  updating,
  completing,
  canceling
}
