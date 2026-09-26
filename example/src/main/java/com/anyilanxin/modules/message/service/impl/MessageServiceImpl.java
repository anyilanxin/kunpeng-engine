package com.anyilanxin.modules.message.service.impl;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand;
import com.anyilanxin.modules.message.controller.dto.MessageCorrelationDto;
import com.anyilanxin.modules.message.service.IMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流程实例信息(ProcessInstance)业务层实现
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MessageServiceImpl implements IMessageService {
  private final KunpengClient client;

  @Override
  public String messageCorrelation(final MessageCorrelationDto dto) {
    final MessageCorrelationCommand messageCorrelationCommand =
        client.newMessageCorrelationCommand();
    MessageCorrelationCommand.MessageCorrelationCommandStep1 messageCorrelationCommandStep1 =
        messageCorrelationCommand
            .messageName(dto.getMessageName())
            .correlationKey(dto.getCorrelationKey());
    if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
      messageCorrelationCommandStep1.variables(dto.getVariables());
    }

    messageCorrelationCommandStep1.send().join();
    return "成功";
  }
}
