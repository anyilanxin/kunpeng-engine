package com.anyilanxin.modules.common.controller.dto;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.anyilanxin.modules.common.entity.ExporterPositionEntity;
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
 * 导出记录添加或修改Request
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
@Schema
@AutoMappers({@AutoMapper(target = ExporterPositionEntity.class)})
public class ExporterPositionDto implements Serializable {
  @Serial private static final long serialVersionUID = 652725226365347400L;

  /** 导出器 id */
  private String exporter;

  /** 最后导出位置 */
  private String lastExportedPosition;

  /** 创建时间 */
  @Schema(type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime created;

  /** 更新时间 */
  @Schema(type = "string", example = "2020-11-12 11:23:59")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  private LocalDateTime lastUpdated;
}
