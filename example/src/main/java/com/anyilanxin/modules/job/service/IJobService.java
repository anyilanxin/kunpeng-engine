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
package com.anyilanxin.modules.job.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.job.controller.dto.JobCompleteDto;
import com.anyilanxin.modules.job.controller.dto.JobDto;
import com.anyilanxin.modules.job.controller.dto.JobPageDto;
import com.anyilanxin.modules.job.controller.dto.JobQueryDto;
import com.anyilanxin.modules.job.entity.JobEntity;
import com.anyilanxin.modules.job.service.vo.JobPageVo;
import com.anyilanxin.modules.job.service.vo.JobVo;
import java.util.List;

public interface IJobService extends BaseService<JobEntity> {
  /**
   * 保存
   *
   * @param dto 作业信息保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  void save(JobDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param jobId 作业id
   * @param dto 作业信息更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  void updateById(Long jobId, JobDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @return AnYiPlusPageResult<JobPageVo> 分页查询结果
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  AnYiPageResult<JobPageVo> pageByModel(JobPageDto dto) throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 作业信息查询条件
   * @return List<JobVo> 查询结果
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  List<JobVo> selectListByModel(JobQueryDto dto) throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param jobId 作业id
   * @return JobVo 查询结果
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  JobVo getById(Long jobId) throws RuntimeException;

  /**
   * 通过jobId删除
   *
   * @param jobId 作业id
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  void deleteById(Long jobId) throws RuntimeException;

  /**
   * 作业信息批量删除
   *
   * @param jobIds 作业id列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-05-13 16:45:42
   */
  void deleteBatch(List<Long> jobIds) throws RuntimeException;

  /**
   * 完成作业
   *
   * @param dto
   */
  void completeJob(JobCompleteDto dto);
}
