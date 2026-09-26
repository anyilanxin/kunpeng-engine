package com.anyilanxin.modules.processinstance.controller.dto;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.anyilanxin.modules.processinstance.entity.ProcessInstanceEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 流程实例信息添加或修改Request
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
@Schema
@AutoMappers({@AutoMapper(target = ProcessInstanceEntity.class)})
public class ProcessInstanceDto implements Serializable {
  @Serial private static final long serialVersionUID = -76666522384873092L;

  /** 父级流程实例 id */
  private String parentProcessInstanceId;

  /** 数据版本 */
  private Integer rev;

  /** 引用活动实例 id */
  private String referenceActivityInstanceId;

  /** 业务key */
  private String businessKey;

  /** 流程定义 key */
  private String processDefinitionKey;

  /** 流程定义 id */
  private String processDefinitionId;

  /** 流程定义名称 */
  private String processDefinitionName;

  /** 开始用户 id */
  private String startUserId;

  /** 开始活动定义 key列表 */
  private String startActivityDefinitionKeys;

  /** 开始活动实例 id列表 */
  private String startActivityInstanceIds;

  /** 结束活动定义 key列表 */
  private String endActivityDefinitionKeys;

  /** 结束活动定义实例 id列表 */
  private String endActivityInstanceIds;

  /** 状态 */
  private String state;

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
