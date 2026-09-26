package com.anyilanxin.modules.message.service;

import com.anyilanxin.modules.message.controller.dto.MessageCorrelationDto;

/**
 * 流程实例信息(ProcessInstance)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
public interface IMessageService {
  /** 关联消息成功 */
  String messageCorrelation(MessageCorrelationDto dto);
}
