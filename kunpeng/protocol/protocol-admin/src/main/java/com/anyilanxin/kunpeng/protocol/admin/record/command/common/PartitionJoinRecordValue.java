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
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;

/**
 * 分区加入命令记录契约，描述成员加入时目标分区的拓扑元数据。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface PartitionJoinRecordValue extends PartitionExecutionRecordValue {
  PartitionInfoMetaRecordValue getPartitionMeta();
}
