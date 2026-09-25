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

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_81;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum MessageDistributeCorrelateLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATE((short) 2, PROCESS_INDEX_205),

  CREATED((short) 2, PROCESS_INDEX_206),

  CORRELATE_DISTRIBUTE((short) 3, PROCESS_INDEX_207),

  CORRELATE_CONFIRM((short) 4, PROCESS_INDEX_208),

  CORRELATE_COMPLETE_CONFIRM((short) 5, PROCESS_INDEX_209),

  CORRELATE_CONFIRMED((short) 4, PROCESS_INDEX_210),

  CORRELATED((short) 6, PROCESS_INDEX_211),

  FAILED((short) 7, PROCESS_INDEX_212),
  ;

  private final short value;
  private final short processIndex;

  MessageDistributeCorrelateLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 2 -> CREATE;
      case 3 -> CORRELATE_DISTRIBUTE;
      case 4 -> CORRELATE_CONFIRM;
      case 5 -> CORRELATE_COMPLETE_CONFIRM;
      case 6 -> CORRELATED;
      case 7 -> FAILED;
      default -> UNKNOWN;
    };
  }

  public static MessageDistributeCorrelateLifeCycle fromValue(final short value) {
    return switch (value) {
      case 2 -> CREATE;
      case 3 -> CORRELATE_DISTRIBUTE;
      case 4 -> CORRELATE_CONFIRM;
      case 5 -> CORRELATE_COMPLETE_CONFIRM;
      case 6 -> CORRELATED;
      case 7 -> FAILED;
      default -> NULL_VAL;
    };
  }

  @Override
  public ValueType getValueType() {
    return ValueType.MESSAGE_DISTRIBUTE_CORRELATE;
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
    return RECORD_INDEX_81;
  }
}
