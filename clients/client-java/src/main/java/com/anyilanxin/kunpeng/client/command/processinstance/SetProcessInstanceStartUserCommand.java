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
package com.anyilanxin.kunpeng.client.command.processinstance;

import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

/**
 * 修改流程实例发起人命令：把这一条实例的发起人换成接手人（离职交接）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface SetProcessInstanceStartUserCommand
    extends FinalCommandStep<SetProcessInstanceStartUserCommandResponse> {
  SetProcessInstanceStartUserCommand processInstanceId(long processInstanceId);

  /** 新发起人 id，不能为空 */
  SetProcessInstanceStartUserCommand startUserId(String startUserId);
}
