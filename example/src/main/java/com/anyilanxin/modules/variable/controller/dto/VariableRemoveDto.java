package com.anyilanxin.modules.variable.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
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
public class VariableRemoveDto extends VariableChangeCommonDto implements Serializable {
  @Serial private static final long serialVersionUID = -76666522384873092L;

  /** 移除所有 */
  private boolean removeAll;

  /** 待移除的变量 key */
  private List<String> removeVariable;
}
