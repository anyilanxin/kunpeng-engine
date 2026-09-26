/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
