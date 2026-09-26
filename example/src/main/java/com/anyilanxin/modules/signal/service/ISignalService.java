package com.anyilanxin.modules.signal.service;

import com.anyilanxin.modules.signal.controller.dto.SignalCorrelationDto;

/**
 * 流程实例信息(ProcessInstance)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
public interface ISignalService {
  /** 关联信号成功 */
  String signalCorrelation(SignalCorrelationDto dto);
}
