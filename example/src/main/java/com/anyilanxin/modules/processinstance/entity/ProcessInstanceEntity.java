package com.anyilanxin.modules.processinstance.entity;

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
 * 流程实例信息(ProcessInstance)Entity
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_PROCESS_INSTANCE")
public class ProcessInstanceEntity implements Serializable {
  @Serial private static final long serialVersionUID = -24205842676659370L;

  /** 流程实例id */
  @TableId(value = "PROCESS_INSTANCE_ID")
  private String processInstanceId;

  /** 父级流程实例 id */
  @TableField(value = "PARENT_PROCESS_INSTANCE_ID")
  private String parentProcessInstanceId;

  /** 数据版本 */
  @TableField(value = "REV")
  private Integer rev;

  /** 引用活动实例 id */
  @TableField(value = "REFERENCE_ACTIVITY_INSTANCE_ID")
  private String referenceActivityInstanceId;

  /** 业务key */
  @TableField(value = "BUSINESS_KEY")
  private String businessKey;

  /** 流程定义 key */
  @TableField(value = "PROCESS_DEFINITION_KEY")
  private String processDefinitionKey;

  /** 流程定义 id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private String processDefinitionId;

  /** 流程定义名称 */
  @TableField(value = "PROCESS_DEFINITION_NAME")
  private String processDefinitionName;

  /** 开始用户 id */
  @TableField(value = "START_USER_ID")
  private String startUserId;

  /** 状态 */
  @TableField(value = "STATE")
  private String state;

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
