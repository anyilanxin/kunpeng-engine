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
package com.anyilanxin.kunpeng.protocol.business.record.command;

import java.util.Arrays;
import java.util.Optional;

public enum BpmnEventType {

  // Default
  UNSPECIFIED(null),

  // Event Type
  CONDITIONAL("conditional"),
  ERROR("error"),
  ESCALATION("escalation"),
  LINK("link"),
  MESSAGE("message"),
  NONE("none"),
  SIGNAL("signal"),
  TERMINATE("terminate"),
  TIMER("timer"),
  COMPENSATION("compensation");

  private final String eventTypeName;

  BpmnEventType(final String eventTypeName) {
    this.eventTypeName = eventTypeName;
  }

  public Optional<String> getEventTypeName() {
    return Optional.ofNullable(eventTypeName);
  }

  public static BpmnEventType bpmnEventTypeFor(final String eventTypeName) {
    return Arrays.stream(values())
        .filter(
            bpmnEventType ->
                bpmnEventType.eventTypeName != null
                    && bpmnEventType.eventTypeName.equals(eventTypeName))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Unsupported BPMN event of type " + eventTypeName));
  }
}
