package com.anyilanxin.modules.variable.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author zxuanhong
 * @date 2026-08-12 16:20
 * @since
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
@Schema
public class VariableChangeCommonDto {
  private Long taskId;
  private Long activityInstanceId;
  private Long processInstanceId;
}
