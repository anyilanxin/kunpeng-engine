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

import com.anyilanxin.modules.resource.controller.dto.DeploymentDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 部署服务
 *
 * @author zxuanhong
 */
public interface IDeploymentService {
  /**
   * 通过文件部署
   *
   * @param deploymentName 部署名称
   * @param file 文件
   * @return Long
   */
  Long deploymentByFile(String deploymentName, MultipartFile file);

  /**
   * 通过base64信息部署
   *
   * @param dto
   * @return {@link Long }
   */
  Long deploymentByBase64(DeploymentDto dto);
}
