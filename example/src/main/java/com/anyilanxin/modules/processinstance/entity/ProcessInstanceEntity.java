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
 * <p>字段与 sink 建表（changelog 2026.9.0-create-process-instance）严格对齐，勿自行增删列名。
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

  /** 根流程实例 id */
  @TableField(value = "ROOT_PROCESS_INSTANCE_ID")
  private String rootProcessInstanceId;

  /** 业务key */
  @TableField(value = "BUSINESS_KEY")
  private String businessKey;

  /** 流程定义 key */
  @TableField(value = "DEFINITION_KEY")
  private String processDefinitionKey;

  /** 流程定义 id */
  @TableField(value = "PROCESS_DEFINITION_ID")
  private String processDefinitionId;

  /** 流程定义名称 */
  @TableField(value = "DEFINITION_NAME")
  private String processDefinitionName;

  /** 开始用户 id */
  @TableField(value = "START_USER")
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

  /** 数据版本 */
  @TableField(value = "REVISION")
  private Integer rev;

  /** 资源 id */
  @TableField(value = "RESOURCE_ID")
  private Integer resourceId;
}
