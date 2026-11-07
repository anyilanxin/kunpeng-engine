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
package com.anyilanxin.kunpeng.protocol.business.record.command.processinstance;

public enum BatchBusinessType {
  PROCESS_INSTANCE,
  ACTIVITY_INSTANCE,
  USER_TASK,

  /** 无业务类型的哨兵（创建类批无 batchAfter 回调时的缺省；ordinal 上 wire 只能尾追） */
  UNKNOW;
}
