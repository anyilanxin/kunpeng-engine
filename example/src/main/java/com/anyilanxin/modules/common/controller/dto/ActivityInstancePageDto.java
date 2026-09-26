package com.anyilanxin.modules.common.controller.dto;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.anyilanxin.core.AnYiPageQuery;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 活动实例信息分页查询Request
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Schema
public class ActivityInstancePageDto extends AnYiPageQuery {
  @Serial private static final long serialVersionUID = 944275795643688531L;

  /** 活动实例 id */
  private String activityInstanceId;

  /** 父级流程活动实例 id */
  private String parentActivityInstanceId;

  /** 数据版本 */
  private Integer rev;

  /** 流程实例 id */
  private String processInstanceId;

  /** 流程定义 key */
  private String processDefinitionKey;

  /** 流程定义 id */
  private String processDefinitionId;

  /** call流程实例 id */
  private String callProcessInstanceId;

  /** 活动 key */
  private String activityDefinitionKey;

  /** 活动名称 */
  private String activityDefinitionName;

  /** 活动类型 */
  private String activityDefinitionType;

  /** 任务 id */
  private String taskId;

  /** 任务审批人 */
  private String assignee;

  /** 开始活动定义 key */
  private String startActivityDefinitionKey;

  /** 开始活动实例 id */
  private String startActivityInstanceId;

  /** 状态 */
  private String state;

  /** 执行序号 */
  private String sequenceCounter;

  /** 事件 id */
  private String incidentId;

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
