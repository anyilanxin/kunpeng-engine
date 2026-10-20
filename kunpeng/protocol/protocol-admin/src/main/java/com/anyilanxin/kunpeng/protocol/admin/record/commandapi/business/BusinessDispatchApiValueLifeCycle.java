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
package com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.*;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandApiValueLifeCycle;

/**
 * 业务面调度对外 API 生命周期，负载均衡、分区变更、副本变更、取消变更与调度查询的请求/响应成对状态。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum BusinessDispatchApiValueLifeCycle implements CommandApiValueLifeCycle {
  CLUSTER_BALANCE_REQUEST((short) 0, PROCESS_INDEX_52, RECORD_INDEX_12),
  CLUSTER_BALANCE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_13),
  CHANGE_PARTITION_REQUEST((short) 2, PROCESS_INDEX_53, RECORD_INDEX_14),
  CHANGE_PARTITION_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_15),
  CHANGE_REPLICATION_REQUEST((short) 4, PROCESS_INDEX_54, RECORD_INDEX_16),
  CHANGE_REPLICATION_RESPONSE((short) 5, NOT_PROCESS_INDEX, RECORD_INDEX_17),
  CHANGE_CANCEL_REQUEST((short) 6, PROCESS_INDEX_55, RECORD_INDEX_18),
  CHANGE_CANCEL_RESPONSE((short) 7, NOT_PROCESS_INDEX, RECORD_INDEX_19),
  DISPATCH_QUERY_REQUEST((short) 8, PROCESS_INDEX_56, RECORD_INDEX_20),
  DISPATCH_QUERY_RESPONSE((short) 9, NOT_PROCESS_INDEX, RECORD_INDEX_21);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  BusinessDispatchApiValueLifeCycle(
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
      case 0 -> CLUSTER_BALANCE_REQUEST;
      case 1 -> CLUSTER_BALANCE_RESPONSE;
      case 2 -> CHANGE_PARTITION_REQUEST;
      case 3 -> CHANGE_PARTITION_RESPONSE;
      case 4 -> CHANGE_REPLICATION_REQUEST;
      case 5 -> CHANGE_REPLICATION_RESPONSE;
      case 6 -> CHANGE_CANCEL_REQUEST;
      case 7 -> CHANGE_CANCEL_RESPONSE;
      case 8 -> DISPATCH_QUERY_REQUEST;
      case 9 -> DISPATCH_QUERY_RESPONSE;
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
  public AdminValueType getValueType() {
    return AdminValueType.BUSINESS_DISPATCH_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
