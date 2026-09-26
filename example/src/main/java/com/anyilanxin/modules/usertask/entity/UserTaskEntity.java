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
package com.anyilanxin.modules.usertask.entity;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.anyilanxin.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_USER_TASK")
public class UserTaskEntity extends BaseEntity {
  @Serial private static final long serialVersionUID = -37672295329211817L;

  @TableId private String userTaskKey;

  /** 元素 id */
  private String elementId;

  /** 元素名称 */
  private String name;

  /** 流程定义 id */
  private String processDefinitionId;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime creationDate;

  /** 完成时间 */
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

  /** 截止日期 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime dueDate;

  /** 更近日期 */
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

  /** 预计清除时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime historyCleanupDate;
}
