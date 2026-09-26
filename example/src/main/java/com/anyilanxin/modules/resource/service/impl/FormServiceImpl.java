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
package com.anyilanxin.modules.resource.service.impl;

import static com.anyilanxin.core.BaseService.getPage;
import static com.anyilanxin.core.BaseService.toPageData;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.modules.resource.controller.dto.FormDto;
import com.anyilanxin.modules.resource.controller.dto.FormPageDto;
import com.anyilanxin.modules.resource.controller.dto.FormQueryDto;
import com.anyilanxin.modules.resource.entity.FormEntity;
import com.anyilanxin.modules.resource.mapper.FormMapper;
import com.anyilanxin.modules.resource.service.IFormService;
import com.anyilanxin.modules.resource.service.vo.FormPageVo;
import com.anyilanxin.modules.resource.service.vo.FormVo;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class FormServiceImpl extends ServiceImpl<FormMapper, FormEntity> implements IFormService {
  private final Converter converter;
  private final FormMapper mapper;

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void save(final FormDto dto) throws RuntimeException {
    final var entity = converter.convert(dto, FormEntity.class);
    final var result = super.save(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "保存数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void updateById(final String formKey, final FormDto dto) throws RuntimeException {
    // 查询数据是否存在
    getById(formKey);
    // 更新数据
    final var entity = converter.convert(dto, FormEntity.class);
    entity.setFormKey(formKey);
    final var result = super.updateById(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "更新数据失败");
    }
  }

  @Override
  public List<FormVo> selectListByModel(final FormQueryDto dto) throws RuntimeException {
    final var list = mapper.selectListByModel(dto);
    if (list == null || list.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return list;
  }

  @Override
  public AnYiPageResult<FormPageVo> pageByModel(final FormPageDto dto) throws RuntimeException {
    return toPageData(mapper.pageByModel(getPage(dto), dto));
  }

  @Override
  public FormVo getById(final String formKey) throws RuntimeException {
    final var byId = super.getById(formKey);
    if (Objects.isNull(byId)) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return converter.convert(byId, FormVo.class);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteById(final String formKey) throws RuntimeException {
    // 查询数据是否存在
    getById(formKey);
    // 删除数据
    final var b = removeById(formKey);
    if (!b) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "删除数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteBatch(final List<String> formKeys) throws RuntimeException {
    final var entities = listByIds(formKeys);
    if (entities == null || entities.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "数据不存在或已经被别人删除");
    }
    final var waitDeleteList = new ArrayList<String>();
    entities.forEach(v -> waitDeleteList.add(v.getFormKey()));
    final var i = mapper.deleteByIds(waitDeleteList);
    if (i <= 0) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "批量删除成功");
    }
  }
}
