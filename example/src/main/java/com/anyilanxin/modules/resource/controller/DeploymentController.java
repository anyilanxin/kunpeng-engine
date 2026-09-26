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
package com.anyilanxin.modules.resource.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.modules.resource.controller.dto.DeploymentDto;
import com.anyilanxin.modules.resource.service.IDeploymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "部署", description = "部署")
@RequestMapping(value = "/resource/deployment", produces = MediaType.APPLICATION_JSON_VALUE)
public class DeploymentController extends AnYiBaseController {
  private final IDeploymentService service;

  /**
   * 通过文件部署
   *
   * @param deploymentName 部署名称
   * @param file 部署文件
   */
  @PostMapping(value = "/by-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AnYiResult<Long> deploymentByFile(
      @RequestParam(required = false) final String deploymentName,
      @RequestParam("file") final MultipartFile file) {
    return ok(service.deploymentByFile(deploymentName, file), "部署成功");
  }

  /**
   * 通过base64信息部署
   *
   * @param dto 部署信息
   */
  @PostMapping(value = "/by-base64")
  public AnYiResult<Long> deploymentByBase64(@RequestBody final DeploymentDto dto) {
    return ok(service.deploymentByBase64(dto), "部署成功");
  }
}
