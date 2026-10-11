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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.controller;

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.VariableRemoveDto;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.VariableUpdateDto;
import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.VariableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 变量接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(value = "/v1/variables", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "变量", description = "变量更新与删除")
public class VariableController {
  private final VariableService variableService;

  public VariableController(final VariableService variableService) {
    this.variableService = variableService;
  }

  @PutMapping
  @Operation(
      summary = "更新（合并写入）变量",
      tags = {"v1.0.0"})
  public Mono<Result<Void>> update(@Valid @RequestBody final VariableUpdateDto dto) {
    return variableService.update(dto).thenReturn(Result.ok());
  }

  @DeleteMapping
  @Operation(
      summary = "按变量名清单删除变量",
      tags = {"v1.0.0"})
  public Mono<Result<Void>> remove(@Valid @RequestBody final VariableRemoveDto dto) {
    return variableService.remove(dto).thenReturn(Result.ok());
  }
}
