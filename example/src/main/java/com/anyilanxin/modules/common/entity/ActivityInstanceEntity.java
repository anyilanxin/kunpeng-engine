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
 * <p>字段与 sink 建表（changelog 2026.9.0-create-activity-instance 及 add-activity-start-columns）严格对齐，
 * 勿自行增删列名。
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

  /** 流程实例 id */
  @TableField(value = "PROCESS_INSTANCE_ID")
  private String processInstanceId;

  /** 根流程实例 id */
  @TableField(value = "ROOT_PROCESS_INSTANCE_ID")
  private String rootProcessInstanceId;

  /** 父级活动实例 id */
  @TableField(value = "PARENT_ACTIVITY_INSTANCE_ID")
  private String parentActivityInstanceId;

  /** 调用流程实例 id */
  @TableField(value = "CALL_PROCESS_INSTANCE_ID")
  private String callProcessInstanceId;

  /** 流程定义 id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private String processDefinitionId;

  /** 流程定义 key */
  @TableField(value = "DEFINITION_KEY")
  private String processDefinitionKey;

  /** 活动 key */
  @TableField(value = "ACTIVITY_KEY")
  private String activityDefinitionKey;

  /** 活动名称 */
  @TableField(value = "ACTIVITY_NAME")
  private String activityDefinitionName;

  /** 活动类型 */
  @TableField(value = "ACTIVITY_TYPE")
  private String activityDefinitionType;

  /** 任务 id */
  @TableField(value = "TASK_ID")
  private String taskId;

  /** 审批人 */
  @TableField(value = "ASSIGNEE")
  private String assignee;

  /** 状态 */
  @TableField(value = "STATE")
  private String state;

  /** 事件 id */
  @TableField(value = "INCIDENT_ID")
  private String incidentId;

  /** 序列号 */
  @TableField(value = "SEQUENCE_COUNTER")
  private String sequenceCounter;

  /** 起始活动定义 key */
  @TableField(value = "START_ACTIVITY_DEFINITION_KEY")
  private String startActivityDefinitionKey;

  /** 起始活动实例 id */
  @TableField(value = "START_ACTIVITY_INSTANCE_ID")
  private String startActivityInstanceId;

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

  /** 数据版本 */
  @TableField(value = "REVISION")
  private Integer rev;

  /** 资源 id */
  @TableField(value = "RESOURCE_ID")
  private Integer resourceId;
}
