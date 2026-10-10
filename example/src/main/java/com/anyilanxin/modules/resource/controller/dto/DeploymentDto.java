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
package com.anyilanxin.modules.resource.controller.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author zxuanhong
 * @date 2026-03-05 12:22
 * @since
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
public class DeploymentDto implements Serializable {
  @Serial private static final long serialVersionUID = 1772684724997L;

  /** 部署激活时间 */
  private String activateProcessDate;

  /** 类别 */
  private String category;

  /** 部署名称 */
  private String deploymentName;

  /** 模型 base64数据 */
  private String diagramBase64Data;

  /** 模型名称 */
  private String diagramNames;

  /** 是否此话：0-不是，1-是 */
  private Integer havePool;

  /** 流程定义 key */
  private String processDefinitionKeys;
}
