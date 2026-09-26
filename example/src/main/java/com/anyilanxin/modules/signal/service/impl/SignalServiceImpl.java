package com.anyilanxin.modules.signal.service.impl;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.signal.correlation.SignalCorrelationCommand;
import com.anyilanxin.modules.signal.controller.dto.SignalCorrelationDto;
import com.anyilanxin.modules.signal.service.ISignalService;
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
public class SignalServiceImpl implements ISignalService {
  private final KunpengClient client;

  @Override
  public String signalCorrelation(final SignalCorrelationDto dto) {

    final SignalCorrelationCommand messageCorrelationCommand = client.newSignalCorrelationCommand();
    final SignalCorrelationCommand.SignalCorrelationCommandStep1 messageCorrelationCommandStep1 =
        messageCorrelationCommand.signalName(dto.getSignalName());
    if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
      messageCorrelationCommandStep1.variables(dto.getVariables());
    }
    messageCorrelationCommandStep1.send().join();
    return "成功";
  }
}
