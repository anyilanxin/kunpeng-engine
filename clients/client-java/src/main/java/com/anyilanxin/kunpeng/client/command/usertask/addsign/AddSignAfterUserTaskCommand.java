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
package com.anyilanxin.kunpeng.client.command.usertask.addsign;

import com.anyilanxin.kunpeng.client.command.usertask.AddSignMode;
import java.util.List;

/**
 * 后加签预登记命令：原任务完成前登记/追加名单，完成时按登记创建加签任务（同模式追加、跨模式拒绝）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AddSignAfterUserTaskCommand {
  /**
   * 指定登记的原任务。
   *
   * @param userTaskId 用户任务 id
   * @return the builder for this command
   */
  AddSignAfterUserTaskCommand taskId(long userTaskId);

  /**
   * 加签人名单（不可含原任务审批人本人）。
   *
   * @param assignees 加签人列表
   * @return the builder for this command
   */
  AddSignAfterUserTaskCommand assignees(List<String> assignees);

  /**
   * 加签模式（并行/串行）。
   *
   * @param addSignMode 加签模式
   * @return the builder for this command
   */
  AddSignAfterUserTaskCommand addSignMode(AddSignMode addSignMode);
}
