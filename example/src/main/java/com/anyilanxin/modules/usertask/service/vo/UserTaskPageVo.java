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
package com.anyilanxin.modules.usertask.service.vo;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode
@Schema
public class UserTaskPageVo implements Serializable {
  @Serial private static final long serialVersionUID = 439773538478229502L;

  /** 用户任务 key */
  private String userTaskKey;

  /** 元素 id */
  private String elementId;

  /** 元素名称 */
  private String name;

  /** 流程定义 id */
  private String processDefinitionId;

  @Schema(title = "创建时间", type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime creationDate;

  @Schema(title = "完成时间", type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime completionDate;

  /** 审批人 */
  private String assignee;

  /** 状态 */
  private String state;

  /** 表单 key */
  private String formKey;

  /** 流程定义key */
  private String processDefinitionKey;

  /** 流程实例 key */
  private String processInstanceKey;

  /** 元素示例 key */
  private String elementInstanceKey;

  @Schema(title = "截止日期", type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime dueDate;

  @Schema(title = "更近日期", type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime followUpDate;

  /** 外部参考 */
  private String externalFormReference;

  /** 流程定义版本 */
  private Integer processDefinitionVersion;

  /** 自定义 header */
  private String customHeaders;

  /** 优先级 */
  private Integer priority;

  /** 租户 id */
  private String tenantId;

  /** 分区 id */
  private BigDecimal partitionId;

  @Schema(title = "预计清除时间", type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime historyCleanupDate;
}
