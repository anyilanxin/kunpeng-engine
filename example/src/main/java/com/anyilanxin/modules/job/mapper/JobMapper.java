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
package com.anyilanxin.modules.job.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.job.controller.dto.JobPageDto;
import com.anyilanxin.modules.job.controller.dto.JobQueryDto;
import com.anyilanxin.modules.job.entity.JobEntity;
import com.anyilanxin.modules.job.service.vo.JobPageVo;
import com.anyilanxin.modules.job.service.vo.JobVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobMapper extends AnYiBaseMapper<JobEntity> {
  /**
   * 分页查询
   *
   * @param dto 查询条件
   * @param page 分页信息
   * @return IPage<JobPageVo> 查询结果
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  IPage<JobPageVo> pageByModel(Page<JobPageVo> page, @Param("query") JobPageDto dto);

  /**
   * 条件查询多条
   *
   * @param dto 查询条件
   * @return List<JobVo> 查询结果
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  List<JobVo> selectListByModel(JobQueryDto dto);
}
