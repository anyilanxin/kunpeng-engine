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
