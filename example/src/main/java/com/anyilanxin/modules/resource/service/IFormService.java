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
package com.anyilanxin.modules.resource.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.resource.controller.dto.FormDto;
import com.anyilanxin.modules.resource.controller.dto.FormPageDto;
import com.anyilanxin.modules.resource.controller.dto.FormQueryDto;
import com.anyilanxin.modules.resource.entity.FormEntity;
import com.anyilanxin.modules.resource.service.vo.FormPageVo;
import com.anyilanxin.modules.resource.service.vo.FormVo;
import java.util.List;

public interface IFormService extends BaseService<FormEntity> {
  /**
   * 保存
   *
   * @param dto 表单信息保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  void save(FormDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param formKey 表单 key
   * @param dto 表单信息更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  void updateById(String formKey, FormDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @throws RuntimeException
   * @return AnYiPlusPageResult<FormPageVo> 分页查询结果
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  AnYiPageResult<FormPageVo> pageByModel(FormPageDto dto) throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 表单信息查询条件
   * @throws RuntimeException
   * @return List<FormVo> 查询结果
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  List<FormVo> selectListByModel(FormQueryDto dto) throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param formKey 表单 key
   * @throws RuntimeException
   * @return FormVo 查询结果
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  FormVo getById(String formKey) throws RuntimeException;

  /**
   * 通过formKey删除
   *
   * @param formKey 表单 key
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  void deleteById(String formKey) throws RuntimeException;

  /**
   * 表单信息批量删除
   *
   * @param formKeys 表单 key列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-03-06 09:55:01
   */
  void deleteBatch(List<String> formKeys) throws RuntimeException;
}
