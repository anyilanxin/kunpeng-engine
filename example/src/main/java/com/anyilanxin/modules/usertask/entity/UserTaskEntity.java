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
package com.anyilanxin.modules.usertask.entity;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.anyilanxin.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 用户任务信息(UserTask)Entity
 *
 * <p>字段与 sink 建表（changelog 2026.9.0-create-user-task）严格对齐，勿自行增删列名； Java 字段名保留示例既有命名，经
 * {@code @TableField} 映射到实际列。
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_USER_TASK")
public class UserTaskEntity extends BaseEntity {
  @Serial private static final long serialVersionUID = -37672295329211817L;

  /** 任务 key（task_id） */
  @TableId(value = "TASK_ID")
  private String userTaskKey;

  /** 元素 id（task_definition_key） */
  @TableField(value = "TASK_DEFINITION_KEY")
  private String elementId;

  /** 元素名称（task_definition_name） */
  @TableField(value = "TASK_DEFINITION_NAME")
  private String name;

  /** 流程定义 id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private String processDefinitionId;

  /** 业务 key */
  @TableField(value = "BUSINESS_KEY")
  private String businessKey;

  /** 创建时间（start_time） */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "START_TIME")
  private LocalDateTime creationDate;

  /** 完成时间（end_time） */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "END_TIME")
  private LocalDateTime completionDate;

  /** 审批人 */
  @TableField(value = "ASSIGNEE")
  private String assignee;

  /** 拥有者 */
  @TableField(value = "OWNER")
  private String owner;

  /** 状态 */
  @TableField(value = "STATE")
  private String state;

  /** 流程定义key（process_definition_key） */
  @TableField(value = "PROCESS_DEFINITION_KEY")
  private String processDefinitionKey;

  /** 流程实例 key（process_instance_id） */
  @TableField(value = "PROCESS_INSTANCE_ID")
  private String processInstanceKey;

  /** 元素实例 key（activity_instance_id） */
  @TableField(value = "ACTIVITY_INSTANCE_ID")
  private String elementInstanceKey;

  /** 截止日期（due_date） */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "DUE_DATE")
  private LocalDateTime dueDate;

  /** 跟进日期（follow_up_date） */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "FOLLOW_UP_DATE")
  private LocalDateTime followUpDate;

  /** 候选组 */
  @TableField(value = "CANDIDATE_GROUPS")
  private String candidateGroups;

  /** 候选用户 */
  @TableField(value = "CANDIDATE_USERS")
  private String candidateUsers;

  /** 优先级 */
  @TableField(value = "PRIORITY")
  private Integer priority;

  /** 耗时 */
  @TableField(value = "DURATION")
  private Long duration;

  /** 数据版本 */
  @TableField(value = "REVISION")
  private Integer rev;

  /** 资源 id */
  @TableField(value = "RESOURCE_ID")
  private Integer resourceId;
}
