/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.modules.job.entity;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_JOB")
public class JobEntity implements Serializable {
  @Serial private static final long serialVersionUID = 170813103804393047L;

  /** 作业id */
  @TableId(value = "JOB_ID")
  private Long jobId;

  /** job 类型 */
  @TableField(value = "JOB_TYPE")
  private String jobType;

  /** 种类：ACTIVITY，ACTIVITY_LISTENER，PROCESS_LISTENER，USER_TASK_LISTENER */
  @TableField(value = "JOB_KIND")
  private String jobKind;

  /** 重试次数 */
  @TableField(value = "RETRIES")
  private Integer retries;

  /** 重试偏移量 */
  @TableField(value = "RETRY_BACKOFF")
  private Integer retryBackoff;

  /** 优先级 */
  @TableField(value = "PRIORITY")
  private Integer priority;

  /** 锁定过期执行时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "DUE_DATE")
  private LocalDateTime dueDate;

  /** 锁定过期时间,单位s */
  @TableField(value = "LOCK_EXPIRE_TIME")
  private Integer lockExpireTime;

  /** 锁定者 */
  @TableField(value = "LOCK_OWNER")
  private String lockOwner;

  /** 流程定义 key */
  @TableField(value = "PROCESS_DEFINITION_KEY")
  private String processDefinitionKey;

  /** 流程定义id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private Long processDefinitionId;

  /** 流程实例 id */
  @TableField(value = "PROCESS_INSTANCE_ID")
  private Long processInstanceId;

  /** 活动定义 key */
  @TableField(value = "ACTIVITY_DEFINITION_KEY")
  private String activityDefinitionKey;

  /** 活动实例 id */
  @TableField(value = "ACTIVITY_INSTANCE_ID")
  private Long activityInstanceId;

  /** 任务 id */
  @TableField(value = "TASK_ID")
  private Long taskId;

  /** 事件id */
  @TableField(value = "INCIDENT_ID")
  private Long incidentId;

  /** 状态 */
  @TableField(value = "STATE")
  private String state;

  /** 被拒绝，true-是，false-不是 */
  @TableField(value = "DENIED")
  private Integer denied;

  /** 拒绝原因 */
  @TableField(value = "DENIED_REASON")
  private String deniedReason;

  /** 开始时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "START_TIME")
  private LocalDateTime startTime;

  /** 结束时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "END_TIME")
  private LocalDateTime endTime;

  /** 耗时 */
  @TableField(value = "DURATION")
  private Long duration;

  /** 租户 id */
  @TableField(value = "TENANT_ID")
  private String tenantId;

  /** 分区 id */
  @TableField(value = "PARTITION_ID")
  private Integer partitionId;
}
