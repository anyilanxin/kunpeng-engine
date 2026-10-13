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
package com.anyilanxin.kunpeng.protocol.admin.record.command.business;

/**
 * 集群管理调度操作的分发类型枚举，作为管理面与业务面 Raft 消息分发时的操作类型标识。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum BusinessDispatchType {
  /** 业务负载平衡 */
  CLUSTER_BALANCE,
  /** 调整分区数 */
  CHANGE_PARTITION,
  /** 调整副本数 */
  CHANGE_REPLICATION,
}
