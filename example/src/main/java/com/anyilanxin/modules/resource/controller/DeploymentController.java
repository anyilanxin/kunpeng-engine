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
