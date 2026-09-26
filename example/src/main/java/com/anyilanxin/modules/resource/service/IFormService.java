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
