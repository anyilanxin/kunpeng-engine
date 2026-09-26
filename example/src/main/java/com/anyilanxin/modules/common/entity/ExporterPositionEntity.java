package com.anyilanxin.modules.common.entity;

import static com.anyilanxin.core.CommonCoreConstant.TIME_ZONE_GMT8;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 导出记录(ExporterPosition)Entity
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
@TableName("ANYI_EXPORTER_POSITION")
public class ExporterPositionEntity implements Serializable {
  @Serial private static final long serialVersionUID = 735984843213335878L;

  /** 分区id */
  @TableId(value = "PARTITION_ID")
  private BigDecimal partitionId;

  /** 导出器 id */
  @TableField(value = "EXPORTER")
  private String exporter;

  /** 最后导出位置 */
  @TableField(value = "LAST_EXPORTED_POSITION")
  private String lastExportedPosition;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "CREATED")
  private LocalDateTime created;

  /** 更新时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "LAST_UPDATED")
  private LocalDateTime lastUpdated;
}
