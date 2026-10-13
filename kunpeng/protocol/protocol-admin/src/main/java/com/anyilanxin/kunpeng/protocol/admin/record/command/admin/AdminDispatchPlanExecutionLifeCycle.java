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
package com.anyilanxin.kunpeng.protocol.admin.record.command.admin;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_1;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 管理面调度计划执行明细生命周期，描述单条调度动作从创建、执行到完成或失败的状态流转。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum AdminDispatchPlanExecutionLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  EXECUTING((short) 1, PROCESS_INDEX_12),

  EXECUTED((short) 2, PROCESS_INDEX_13),

  ACKNOWLEDGE((short) 3, PROCESS_INDEX_14),

  ACKNOWLEDGE_TIMED_OUT((short) 4, PROCESS_INDEX_15),

  SUCCEED((short) 5, PROCESS_INDEX_16),

  FAILED((short) 6, PROCESS_INDEX_17);

  private final short value;
  private final short processIndex;

  AdminDispatchPlanExecutionLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> EXECUTING;
      case 2 -> EXECUTED;
      case 3 -> ACKNOWLEDGE;
      case 4 -> ACKNOWLEDGE_TIMED_OUT;
      case 5 -> SUCCEED;
      case 6 -> FAILED;
      default -> UNKNOWN;
    };
  }

  public static AdminDispatchPlanExecutionLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> EXECUTING;
      case 2 -> EXECUTED;
      case 3 -> ACKNOWLEDGE;
      case 4 -> ACKNOWLEDGE_TIMED_OUT;
      case 5 -> SUCCEED;
      case 6 -> FAILED;
      default -> NULL_VAL;
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isState() {
    return switch (this) {
      case NULL_VAL, EXECUTING, ACKNOWLEDGE, ACKNOWLEDGE_TIMED_OUT -> false;
      case EXECUTED, SUCCEED, FAILED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.ADMIN_DISPATCH_EXECUTION;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_1;
  }
}
