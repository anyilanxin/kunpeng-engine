package com.anyilanxin.modules.variable.controller.dto;

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
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema
public class VariableUpdateDto extends VariableChangeCommonDto implements Serializable {
  @Serial private static final long serialVersionUID = -76666522384873092L;

  /** 待更新或者添加的变量 key */
  private Map<String, Object> variables;
}
