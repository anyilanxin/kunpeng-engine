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
package com.anyilanxin.kunpeng.protocol.business.record.command.deployment;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_13;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum DecisionRequirementDefinitionLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATED((short) 0, PROCESS_INDEX_36),
  DELETED((short) 1, PROCESS_INDEX_37);

  private final short value;
  private final short processIndex;

  DecisionRequirementDefinitionLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    return false;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_13;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.DECISION_REQUIREMENTS;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CREATED;
      case 1 -> DELETED;
      default -> UNKNOWN;
    };
  }

  public static DecisionRequirementDefinitionLifeCycle fromValue(final short value) {
    return switch (value) {
      case 0 -> CREATED;
      case 1 -> DELETED;
      default -> CREATED;
    };
  }
}
