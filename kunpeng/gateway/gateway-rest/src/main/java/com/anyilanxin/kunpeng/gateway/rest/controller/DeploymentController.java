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

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.DeploymentCreateDto;
import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.DeploymentService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.DeploymentVo;
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
 * 部署管理接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(
    value = "/{version}/deployments",
    version = "v1",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "部署管理", description = "流程资源部署与删除")
public class DeploymentController {
  private final DeploymentService deploymentService;

  public DeploymentController(final DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @PostMapping
  @Operation(summary = "部署流程资源")
  public Mono<Result<DeploymentVo>> deploy(@Valid @RequestBody final DeploymentCreateDto dto) {
    return deploymentService.deploy(dto).map(Result::ok);
  }

  @DeleteMapping("/{deploymentKey}")
  @Operation(summary = "删除部署")
  public Mono<Result<Void>> delete(@PathVariable final long deploymentKey) {
    return deploymentService.delete(deploymentKey).thenReturn(Result.ok());
  }
}
