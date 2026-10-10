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
package com.anyilanxin.modules.processinstance.controller.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
public class ProcessInstanceCreateDto implements Serializable {
  @Serial private static final long serialVersionUID = 1770101820662L;

  /** 流程定义 id */
  private Long processDefinitionId;

  /** 流程定义 key */
  private String processDefinitionKey;

  /** 流程定义版本 */
  private Integer processDefinitionVersion;

  /** 变量信息 */
  private Map<String, Object> variables;
}
