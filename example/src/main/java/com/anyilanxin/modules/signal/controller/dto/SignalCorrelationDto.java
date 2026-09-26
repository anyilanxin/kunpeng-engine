package com.anyilanxin.modules.signal.controller.dto;

import com.anyilanxin.modules.processinstance.entity.ProcessInstanceEntity;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
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
public class SignalCorrelationDto implements Serializable {
  @Serial private static final long serialVersionUID = -76666522384873092L;

  /** 信号 名称 */
  private String signalName;

  /** 变量信息 */
  private Map<String, Object> variables;
}
