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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.*;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiDeploymentValueLifeCycle implements CommandApiValueLifeCycle {
  CREATE_REQUEST((short) 0, PROCESS_INDEX_0, RECORD_INDEX_0),
  CREATE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_1),
  DELETE_REQUEST((short) 2, PROCESS_INDEX_1, RECORD_INDEX_2),
  DELETE_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_3);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiDeploymentValueLifeCycle(
      final short value, final short processIndex, final short recordIndex) {
    this.value = value;
    this.processIndex = processIndex;
    this.recordIndex = recordIndex;
  }

  public short getValueState() {
    return value;
  }

  public static CommandApiValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CREATE_REQUEST;
      case 1 -> CREATE_RESPONSE;
      case 2 -> DELETE_REQUEST;
      case 3 -> DELETE_RESPONSE;
      default -> throw new IllegalStateException("Unexpected value: " + value);
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
  public ValueType getValueType() {
    return ValueType.DEPLOYMENT_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
