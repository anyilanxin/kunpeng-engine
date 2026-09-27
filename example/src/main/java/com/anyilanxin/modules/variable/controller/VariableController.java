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
package com.anyilanxin.modules.variable.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.modules.variable.controller.dto.VariableRemoveDto;
import com.anyilanxin.modules.variable.controller.dto.VariableUpdateDto;
import com.anyilanxin.modules.variable.service.IVariableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = " Variable", description = "变量变更")
@MessageMapping(value = "/variable")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class VariableController extends AnYiBaseController {
  private final IVariableService service;

  @Operation(
      summary = "添加或修改变量",
      tags = {"v1.0.0"},
      description = "添加或修改变量")
  @PostMapping(value = "/update")
  public AnYiResult<String> update(@RequestBody @Valid final VariableUpdateDto dto) {
    service.update(dto);
    return ok("变量变更成功");
  }

  @Operation(
      summary = "移除变量",
      tags = {"v1.0.0"},
      description = "移除变量")
  @PostMapping(value = "/remove")
  public AnYiResult<String> remove(@RequestBody @Valid final VariableRemoveDto dto) {
    service.remove(dto);
    return ok("变量删除成功");
  }
}
