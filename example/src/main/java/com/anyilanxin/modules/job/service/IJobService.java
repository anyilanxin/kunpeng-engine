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
