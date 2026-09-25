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
package com.anyilanxin.kunpeng.protocol.business.record.query.businessroute;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.*;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.query.QueryApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum BusinessRouteApiLifeCycle implements QueryApiValueLifeCycle {
  MESSAGE_REQUEST((short) 0, PROCESS_INDEX_183, RECORD_INDEX_72),
  MESSAGE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_73),

  SIGNAL_REQUEST((short) 2, PROCESS_INDEX_184, RECORD_INDEX_74),
  SIGNAL_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_75),
  ;

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  BusinessRouteApiLifeCycle(final short value, final short processIndex, final short recordIndex) {
    this.value = value;
    this.processIndex = processIndex;
    this.recordIndex = recordIndex;
  }

  public short getValueState() {
    return value;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> MESSAGE_REQUEST;
      case 1 -> MESSAGE_RESPONSE;
      case 2 -> SIGNAL_REQUEST;
      case 3 -> SIGNAL_RESPONSE;
      default -> UNKNOWN;
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.QUERY_BUSINESS_ROUTE;
  }
}
