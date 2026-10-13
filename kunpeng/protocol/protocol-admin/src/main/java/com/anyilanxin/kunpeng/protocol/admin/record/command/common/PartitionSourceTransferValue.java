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
package com.anyilanxin.kunpeng.protocol.admin.record.command.common;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;

/**
 * 分区数据合并命令记录契约，描述数据合并的目标成员。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface PartitionSourceTransferValue extends PartitionExecutionRecordValue {
  /** 资源标识符 */
  int getSourceId();

  /** 资源来源分区组 */
  String getSourcePartitionGroup();

  /** 资源来源分区id */
  int getSourcePartitionId();
}
