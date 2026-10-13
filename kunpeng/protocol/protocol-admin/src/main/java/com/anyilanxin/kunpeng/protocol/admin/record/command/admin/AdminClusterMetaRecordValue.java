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

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;

/**
 * 集群元数据记录契约，描述集群配置版本号、副本因子、创建与更新时间，以及当前与上一版的分区组拓扑信息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminClusterMetaRecordValue extends RecordValue {
  int getVersion();

  int getReplicationFactor();

  int getCurrentReplicationFactor();

  long getCreateTime();

  long getUpdateTime();

  PartitionInfoMetaRecordValue getMeta();

  PartitionInfoMetaRecordValue getLastMeta();
}
