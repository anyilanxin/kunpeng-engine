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

import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommandResponse;
import com.anyilanxin.modules.resource.controller.dto.DeploymentDto;
import com.anyilanxin.modules.resource.service.IDeploymentService;
import com.anyilanxin.utils.Base64FileUtils;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 部署服务
 *
 * @author zxuanhong
 * @date 2026-01-30 12:17
 * @since
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentServiceImpl implements IDeploymentService {
  private final KunpengClient client;

  @Override
  public Long deploymentByFile(final String deploymentName, final MultipartFile file) {
    final DeployResourceCommandResponse execute;
    try {
      final InputStream inputStream = file.getInputStream();
      execute =
          client
              .newDeployResourceCommand()
              .addResourceStream(inputStream, file.getOriginalFilename())
              .execute();
    } catch (final IOException e) {
      throw new AnYiResponseException(AnYiResultStatus.API_ERROR, e);
    }
    return execute.getProcessDefinitions().getFirst().getProcessDefinitionId();
  }

  @Override
  public Long deploymentByBase64(final DeploymentDto dto) {
    final DeployResourceCommandResponse execute;
    try (final InputStream inputStream =
        Base64FileUtils.base64ToInputStream(dto.getDiagramBase64Data())) {
      String deploymentName = dto.getDeploymentName();
      if (StringUtils.isBlank(deploymentName)) {
        deploymentName = "未知.bpmn";
      } else {
        deploymentName += ".bpmn";
      }
      execute =
          client
              .newDeployResourceCommand()
              .addResourceStream(inputStream, deploymentName)
              .execute();
    } catch (final IOException e) {
      throw new AnYiResponseException(AnYiResultStatus.API_ERROR, e);
    }
    return execute.getProcessDefinitions().getFirst().getProcessDefinitionId();
  }
}
