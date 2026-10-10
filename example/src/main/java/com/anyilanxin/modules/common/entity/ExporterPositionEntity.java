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
 * sink 导出位置(ExporterPosition)Entity
 *
 * <p>字段与 sink 建表（changelog 2026.9.0-create-sink-position）严格对齐：复合主键 (PARTITION_ID, SINK) 中 仅
 * PARTITION_ID 声明为 {@code @TableId}（示例只做列表查询），勿自行增删列名。
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
@TableName("ANYI_SINK_POSITION")
public class ExporterPositionEntity implements Serializable {
  @Serial private static final long serialVersionUID = 735984843213335878L;

  /** 分区id */
  @TableId(value = "PARTITION_ID")
  private Integer partitionId;

  /** sink id */
  @TableField(value = "SINK")
  private String exporter;

  /** 最后导出位置 */
  @TableField(value = "EXPORTED_POSITION")
  private String lastExportedPosition;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "CREATED_TIME")
  private LocalDateTime created;

  /** 更新时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIME_ZONE_GMT8)
  @TableField(value = "UPDATE_TIME")
  private LocalDateTime lastUpdated;
}
