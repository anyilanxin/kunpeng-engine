/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
