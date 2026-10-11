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

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.EvaluateExpressionDto;
import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.ExpressionService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.EvaluateExpressionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 表达式求值接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(value = "/v1/expressions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "表达式", description = "表达式求值")
public class ExpressionController {
  private final ExpressionService expressionService;

  public ExpressionController(final ExpressionService expressionService) {
    this.expressionService = expressionService;
  }

  @PostMapping("/evaluate")
  @Operation(
      summary = "求值表达式",
      tags = {"v1.0.0"})
  public Mono<Result<EvaluateExpressionVo>> evaluate(
      @Valid @RequestBody final EvaluateExpressionDto dto) {
    return expressionService.evaluate(dto).map(Result::ok);
  }
}
