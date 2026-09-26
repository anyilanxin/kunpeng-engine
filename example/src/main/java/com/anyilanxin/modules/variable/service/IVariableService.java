package com.anyilanxin.modules.variable.service;

import com.anyilanxin.modules.variable.controller.dto.VariableRemoveDto;
import com.anyilanxin.modules.variable.controller.dto.VariableUpdateDto;

/**
 * 流程实例信息(ProcessInstance)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
public interface IVariableService {
  /** 关联消息成功 */
  void update(VariableUpdateDto dto);

  void remove(VariableRemoveDto dto);
}
