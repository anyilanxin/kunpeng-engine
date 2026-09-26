package com.anyilanxin.modules.common.entity;

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
 * 活动实例信息(ActivityInstance)Entity
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_ACTIVITY_INSTANCE")
public class ActivityInstanceEntity implements Serializable {
  @Serial private static final long serialVersionUID = 724293901047825035L;

  /** 活动实例 id */
  @TableId(value = "ACTIVITY_INSTANCE_ID")
  private String activityInstanceId;

  /** 父级流程活动实例 id */
  @TableField(value = "PARENT_ACTIVITY_INSTANCE_ID")
  private String parentActivityInstanceId;

  /** 数据版本 */
  @TableField(value = "REV")
  private Integer rev;

  /** 流程实例 id */
  @TableField(value = "PROCESS_INSTANCE_ID")
  private String processInstanceId;

  /** 流程定义 key */
  @TableField(value = "PROCESS_DEFINITION_KEY")
  private String processDefinitionKey;

  /** 流程定义 id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private String processDefinitionId;

  /** call流程实例 id */
  @TableField(value = "CALL_PROCESS_INSTANCE_ID")
  private String callProcessInstanceId;

  /** 活动 key */
  @TableField(value = "ACTIVITY_DEFINITION_KEY")
  private String activityDefinitionKey;

  /** 活动名称 */
  @TableField(value = "ACTIVITY_DEFINITION_NAME")
  private String activityDefinitionName;

  /** 活动类型 */
  @TableField(value = "ACTIVITY_DEFINITION_TYPE")
  private String activityDefinitionType;

  /** 任务 id */
  @TableField(value = "TASK_ID")
  private String taskId;

  /** 任务审批人 */
  @TableField(value = "ASSIGNEE")
  private String assignee;

  /** 开始活动定义 key */
  @TableField(value = "START_ACTIVITY_DEFINITION_KEY")
  private String startActivityDefinitionKey;

  /** 开始活动实例 id */
  @TableField(value = "START_ACTIVITY_INSTANCE_ID")
  private String startActivityInstanceId;

  /** 状态 */
  @TableField(value = "STATE")
  private String state;

  /** 执行序号 */
  @TableField(value = "SEQUENCE_COUNTER")
  private String sequenceCounter;

  /** 事件 id */
  @TableField(value = "INCIDENT_ID")
  private String incidentId;

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
