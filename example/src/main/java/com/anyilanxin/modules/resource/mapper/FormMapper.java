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
package com.anyilanxin.modules.resource.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.resource.controller.dto.FormPageDto;
import com.anyilanxin.modules.resource.controller.dto.FormQueryDto;
import com.anyilanxin.modules.resource.entity.FormEntity;
import com.anyilanxin.modules.resource.service.vo.FormPageVo;
import com.anyilanxin.modules.resource.service.vo.FormVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FormMapper extends AnYiBaseMapper<FormEntity> {
  /**
   * 分页查询
   *
   * @param vo 查询条件
   * @param page 分页信息
   * @return IPage<FormPageVo> 查询结果
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  IPage<FormPageVo> pageByModel(Page<FormPageVo> page, @Param("query") FormPageDto dto);

  /**
   * 条件查询多条
   *
   * @param vo 查询条件
   * @return List<FormVo> 查询结果
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  List<FormVo> selectListByModel(FormQueryDto dto);
}
