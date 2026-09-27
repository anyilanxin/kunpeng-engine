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
package com.anyilanxin.modules.job.service.vo;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.anyilanxin.modules.job.entity.JobEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
@AutoMappers({@AutoMapper(target = JobEntity.class)})
public class JobVo implements Serializable {
  @Serial private static final long serialVersionUID = -35041915306736093L;

  /** 作业id */
  private Long jobId;

  /** job 类型 */
  private String jobType;

  /** 种类：ACTIVITY，ACTIVITY_LISTENER，PROCESS_LISTENER，USER_TASK_LISTENER */
  private String jobKind;

  /** 重试次数 */
  private Integer retries;

  /** 重试偏移量 */
  private Integer retryBackoff;

  /** 优先级 */
  private Integer priority;

  /** 锁定过期执行时间 */
  @Schema(type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime dueDate;

  /** 锁定过期时间,单位s */
  private Integer lockExpireTime;

  /** 锁定者 */
  private String lockOwner;

  /** 流程定义 key */
  private String processDefinitionKey;

  /** 流程定义id */
  private Long processDefinitionId;

  /** 流程实例 id */
  private Long processInstanceId;

  /** 活动定义 key */
  private String activityDefinitionKey;

  /** 活动实例 id */
  private Long activityInstanceId;

  /** 任务 id */
  private Long taskId;

  /** 事件id */
  private Long incidentId;

  /** 状态 */
  private String state;

  /** 被拒绝，true-是，false-不是 */
  private Integer denied;

  /** 拒绝原因 */
  private String deniedReason;

  /** 开始时间 */
  @Schema(type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime startTime;

  /** 结束时间 */
  @Schema(type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime endTime;

  /** 耗时 */
  private Long duration;

  /** 租户 id */
  private String tenantId;

  /** 分区 id */
  private Integer partitionId;
}
