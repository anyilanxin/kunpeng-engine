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
package com.anyilanxin.kunpeng.protocol.admin.record.command;

import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import java.util.List;

/**
 * 分区信息元数据 Record 值接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface PartitionInfoMetaRecordValue extends RecordValue {
  String getPartitionGroup();

  int getPartitionId();

  int getTargetPriority();

  String getPrimaryMemberId();

  List<PartitionMemberMetaRecordValue> getPartitionMembers();
}
