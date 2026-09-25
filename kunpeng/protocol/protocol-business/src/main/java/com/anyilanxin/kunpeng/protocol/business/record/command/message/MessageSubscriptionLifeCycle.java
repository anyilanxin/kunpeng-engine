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
package com.anyilanxin.kunpeng.protocol.business.record.command.message;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_67;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum MessageSubscriptionLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATING((short) 1, PROCESS_INDEX_151),
  CREATED((short) 2, PROCESS_INDEX_152),

  DISTRIBUTE_CREATED((short) 3, PROCESS_INDEX_153),

  CORRELATE((short) 4, PROCESS_INDEX_154),

  CORRELATE_DISTRIBUTE((short) 4, PROCESS_INDEX_192),

  CORRELATE_DISTRIBUTE_AFTER((short) 4, PROCESS_INDEX_193),

  CORRELATED((short) 5, PROCESS_INDEX_155),
  CANCEL((short) 6, PROCESS_INDEX_156),
  CANCELED((short) 7, PROCESS_INDEX_157),
  DISTRIBUTE_CANCELED((short) 8, PROCESS_INDEX_158),
  ;

  private final short value;
  private final short processIndex;

  MessageSubscriptionLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> DISTRIBUTE_CREATED;
      case 4 -> CORRELATE;
      case 5 -> CORRELATED;
      case 6 -> CANCEL;
      case 7 -> CANCELED;
      case 8 -> DISTRIBUTE_CANCELED;
      default -> UNKNOWN;
    };
  }

  public static MessageSubscriptionLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> DISTRIBUTE_CREATED;
      case 4 -> CORRELATE;
      case 5 -> CORRELATED;
      case 6 -> CANCEL;
      case 7 -> CANCELED;
      case 8 -> DISTRIBUTE_CANCELED;
      default -> NULL_VAL;
    };
  }

  @Override
  public ValueType getValueType() {
    return ValueType.MESSAGE_SUBSCRIPTION;
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
    return RECORD_INDEX_67;
  }
}
