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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.complete;

import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.util.List;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface UserTaskCompleteRequestRecordValue
    extends TenantOwned, RequestRecordValue, JsonSerializable {

  /** 完成时携带的后加签名单（空列表 = 不加签，仅按既有预登记处理） */
  List<String> getAddSignAssignees();

  UserTaskCompleteRequestRecordValue setAddSignAssignees(final List<String> addSignAssignees);

  /** 携带名单非空时的加签模式 */
  AddSignMode getAddSignMode();

  UserTaskCompleteRequestRecordValue setAddSignMode(final AddSignMode addSignMode);
}
