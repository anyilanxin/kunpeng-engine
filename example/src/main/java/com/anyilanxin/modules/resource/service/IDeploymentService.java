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
