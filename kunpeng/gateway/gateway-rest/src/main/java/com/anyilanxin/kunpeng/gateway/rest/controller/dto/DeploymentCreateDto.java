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
package com.anyilanxin.kunpeng.gateway.rest.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 部署创建请求。
 *
 * @param tenantId 租户
 * @param resources 资源列表（BPMN XML 等文件内容，Base64 编码）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record DeploymentCreateDto(
    String tenantId, @NotEmpty @Valid List<DeploymentResourceDto> resources) {

  /**
   * 单个部署资源。
   *
   * @param name 资源名（含扩展名，如 order-process.bpmn）
   * @param contentBase64 资源内容（Base64）
   */
  public record DeploymentResourceDto(
      @NotBlank(message = "资源名不能为空") String name,
      @NotBlank(message = "资源内容不能为空") String contentBase64) {}
}
