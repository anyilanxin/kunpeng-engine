/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import com.anyilanxin.kunpeng.sink.rdbms.model.UserTaskCandidateDbModel;
import org.apache.ibatis.annotations.Param;

/** 任务候选链接表语句（XML：mapper/UserTaskCandidateMapper.xml）。 */
public interface UserTaskCandidateMapper {

  int insert(UserTaskCandidateDbModel model);

  /** 任务候选整任务刷新的清空腿：按任务删全部候选行。 */
  int cleanupByTask(@Param("taskId") long taskId);

  int cleanupHistory(@Param("processInstanceId") long processInstanceId);

  int clearAll();
}
