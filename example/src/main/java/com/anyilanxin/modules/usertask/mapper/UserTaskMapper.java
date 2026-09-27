/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.modules.usertask.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.usertask.controller.dto.UserTaskPageDto;
import com.anyilanxin.modules.usertask.controller.dto.UserTaskQueryDto;
import com.anyilanxin.modules.usertask.entity.UserTaskEntity;
import com.anyilanxin.modules.usertask.service.vo.UserTaskPageVo;
import com.anyilanxin.modules.usertask.service.vo.UserTaskVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTaskMapper extends AnYiBaseMapper<UserTaskEntity> {
  /**
   * 分页查询
   *
   * @param vo 查询条件
   * @param page 分页信息
   * @return IPage<UserTaskPageVo> 查询结果
   * @author zxh
   * @date 2026-03-06 09:53:24
   */
  IPage<UserTaskPageVo> pageByModel(Page<UserTaskPageVo> page, @Param("query") UserTaskPageDto dto);

  /**
   * 条件查询多条
   *
   * @param vo 查询条件
   * @return List<UserTaskVo> 查询结果
   * @author zxh
   * @date 2026-03-06 09:53:24
   */
  List<UserTaskVo> selectListByModel(UserTaskQueryDto dto);
}
