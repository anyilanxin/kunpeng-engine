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
package com.anyilanxin.modules.usertask.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.usertask.controller.dto.*;
import com.anyilanxin.modules.usertask.entity.UserTaskEntity;
import com.anyilanxin.modules.usertask.service.vo.UserTaskPageVo;
import com.anyilanxin.modules.usertask.service.vo.UserTaskVo;
import java.util.List;

public interface IUserTaskService extends BaseService<UserTaskEntity> {
  /**
   * 保存
   *
   * @param dto 用户任务信息保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  void save(UserTaskDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param userTaskKey 用户任务 key
   * @param dto 用户任务信息更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  void updateById(String userTaskKey, UserTaskDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @return AnYiPlusPageResult<UserTaskPageVo> 分页查询结果
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  AnYiPageResult<UserTaskPageVo> pageByModel(UserTaskPageDto dto) throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 用户任务信息查询条件
   * @return List<UserTaskVo> 查询结果
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  List<UserTaskVo> selectListByModel(UserTaskQueryDto dto) throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param userTaskKey 用户任务 key
   * @return UserTaskVo 查询结果
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  UserTaskVo getById(String userTaskKey) throws RuntimeException;

  /**
   * 通过userTaskKey删除
   *
   * @param userTaskKey 用户任务 key
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  void deleteById(String userTaskKey) throws RuntimeException;

  /**
   * 用户任务信息批量删除
   *
   * @param userTaskKeys 用户任务 key列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:53:25
   */
  void deleteBatch(List<String> userTaskKeys) throws RuntimeException;

  /**
   * 完成用户任务
   *
   * @param dto
   */
  void complete(final UserTaskCompleteDto dto);

  /**
   * 取消用户任务成功
   *
   * @param dto
   */
  void cancel(UserTaskCancelDto dto);
}
