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

/**
 * 任务信息(Job)Entity
 *
 * <p>字段与 sink 建表（changelog 2026.9.0-create-job）严格对齐，勿自行增删列名。
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_JOB")
public class JobEntity implements Serializable {
  @Serial private static final long serialVersionUID = 170813103804393047L;

  /** 任务 id */
  @TableId(value = "JOB_ID")
  private Long jobId;

  /** 任务类型 */
  @TableField(value = "JOB_TYPE")
  private String jobType;

  /** 任务种类 */
  @TableField(value = "JOB_KIND")
  private String jobKind;

  /** 状态 */
  @TableField(value = "STATE")
  private String state;

  /** 重试次数 */
  @TableField(value = "RETRIES")
  private Integer retries;

  /** 重试退避 */
  @TableField(value = "RETRY_BACKOFF")
  private Integer retryBackoff;

  /** 优先级 */
  @TableField(value = "PRIORITY")
  private Integer priority;

  /** 到期时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "DUE_DATE")
  private LocalDateTime dueDate;

  /** 锁持有者 */
  @TableField(value = "LOCK_OWNER")
  private String lockOwner;

  /** 锁到期时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "LOCK_EXPIRE_TIME")
  private LocalDateTime lockExpireTime;

  /** 流程实例 id */
  @TableField(value = "PROCESS_INSTANCE_ID")
  private Long processInstanceId;

  /** 活动实例 id */
  @TableField(value = "ACTIVITY_INSTANCE_ID")
  private Long activityInstanceId;

  /** 流程定义 id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private Long processDefinitionId;

  /** 流程定义 key */
  @TableField(value = "PROCESS_DEFINITION_KEY")
  private String processDefinitionKey;

  /** 活动 key */
  @TableField(value = "ACTIVITY_DEFINITION_KEY")
  private String activityDefinitionKey;

  /** 任务 id（关联用户任务） */
  @TableField(value = "TASK_ID")
  private Long taskId;

  /** 事件 id */
  @TableField(value = "INCIDENT_ID")
  private Long incidentId;

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

  /** 变量 json */
  @TableField(value = "VARIABLES_JSON")
  private String variablesJson;

  /** 本地变量 json */
  @TableField(value = "LOCAL_VARIABLES_JSON")
  private String localVariablesJson;

  /** 数据版本 */
  @TableField(value = "REVISION")
  private Integer rev;

  /** 资源 id */
  @TableField(value = "RESOURCE_ID")
  private Integer resourceId;
}
