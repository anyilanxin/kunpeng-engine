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
package com.anyilanxin.modules.resource.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.NotNullSize;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.resource.controller.dto.FormDto;
import com.anyilanxin.modules.resource.controller.dto.FormPageDto;
import com.anyilanxin.modules.resource.controller.dto.FormQueryDto;
import com.anyilanxin.modules.resource.service.IFormService;
import com.anyilanxin.modules.resource.service.vo.FormPageVo;
import com.anyilanxin.modules.resource.service.vo.FormVo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@Hidden
@Tag(name = "FormDefinition", description = "表单信息相关")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/form", produces = MediaType.APPLICATION_JSON_VALUE)
public class FormController extends AnYiBaseController {
  private final IFormService service;

  @Operation(
      summary = "表单信息添加",
      tags = {"v1.0.0"},
      description = "添加表单信息",
      hidden = true)
  @PostMapping(value = "/insert")
  public AnYiResult<String> insert(@RequestBody @Valid final FormDto dto) {
    service.save(dto);
    return ok("保存成功");
  }

  @Operation(
      summary = "通过表单 key修改",
      tags = {"v1.0.0"},
      description = "修改表单信息",
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "表单 key", name = "formKey", required = true)
  @PutMapping(value = "/update/{formKey}")
  public AnYiResult<String> update(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "表单 key不能为空")
          final String formKey,
      @RequestBody @Valid final FormDto dto) {
    service.updateById(formKey, dto);
    return ok("更新成功");
  }

  @Operation(
      summary = "表单信息逻辑删除",
      tags = {"v1.0.0"},
      description = "删除表单信息",
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "表单 key", name = "formKey", required = true)
  @DeleteMapping(value = "/delete-one/{formKey}")
  public AnYiResult<String> deleteById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "表单 key不能为空")
          final String formKey) {
    service.deleteById(formKey);
    return ok("删除成功");
  }

  @Operation(
      summary = "表单信息逻辑批量删除",
      tags = {"v1.0.0"},
      description = "批量删除表单信息",
      hidden = true)
  @PostMapping(value = "/delete-batch")
  public AnYiResult<String> deleteBatchByIds(
      @RequestBody @NotNullSize(message = "待删除表单 key不能为空") final List<String> formKeys) {
    service.deleteBatch(formKeys);
    return ok("批量删除成功");
  }

  @Operation(
      summary = "通过表单 key查询详情",
      tags = {"v1.0.0"},
      description = "查询表单信息详情",
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "表单 key", name = "formKey", required = true)
  @GetMapping(value = "/select/one/{formKey}")
  public AnYiResult<FormVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "表单 key不能为空")
          final String formKey) {
    return ok(service.getById(formKey));
  }

  @Operation(
      summary = "通过条件查询表单信息多条数据",
      tags = {"v1.0.0"},
      description = "通过条件查询表单信息",
      hidden = true)
  @PostMapping(value = "/select/list/by-model")
  public AnYiResult<List<FormVo>> selectListByModel(@RequestBody final FormQueryDto dto) {
    return ok(service.selectListByModel(dto));
  }

  @Operation(
      summary = "表单信息分页查询",
      tags = {"v1.0.0"},
      description = "分页查询表单信息",
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<FormPageVo>> selectPage(@RequestBody final FormPageDto dto) {
    return ok(service.pageByModel(dto));
  }
}
