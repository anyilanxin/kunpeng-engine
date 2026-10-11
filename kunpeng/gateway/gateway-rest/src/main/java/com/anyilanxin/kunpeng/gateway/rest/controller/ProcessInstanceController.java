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

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.ProcessInstanceCreateDto;
import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.ProcessInstanceService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.ProcessInstanceVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 流程实例接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(value = "/v1/process-instances", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "流程实例", description = "流程实例创建与取消")
public class ProcessInstanceController {
  private final ProcessInstanceService processInstanceService;

  public ProcessInstanceController(final ProcessInstanceService processInstanceService) {
    this.processInstanceService = processInstanceService;
  }

  @PostMapping
  @Operation(
      summary = "创建并启动流程实例",
      tags = {"v1.0.0"})
  public Mono<Result<ProcessInstanceVo>> create(
      @Valid @RequestBody final ProcessInstanceCreateDto dto) {
    return processInstanceService.create(dto).map(Result::ok);
  }

  @PostMapping("/with-result")
  @Operation(
      summary = "创建流程实例并等待结果（未实现）",
      tags = {"v1.0.0"})
  public Mono<Result<ProcessInstanceVo>> createWithResult(
      @Valid @RequestBody final ProcessInstanceCreateDto dto) {
    return processInstanceService.createWithResult(dto).map(Result::ok);
  }

  @DeleteMapping("/{processInstanceKey}")
  @Operation(
      summary = "取消流程实例",
      tags = {"v1.0.0"})
  public Mono<Result<Void>> cancel(@PathVariable final long processInstanceKey) {
    return processInstanceService.cancel(processInstanceKey).thenReturn(Result.ok());
  }
}
