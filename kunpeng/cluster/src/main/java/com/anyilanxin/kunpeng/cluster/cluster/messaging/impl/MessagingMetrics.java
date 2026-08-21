/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import com.anyilanxin.kunpeng.utils.CloseableSilently;

/**
 * 消息服务的度量采集接口。
 *
 * <p>实现方负责把请求耗时、报文体积、收发计数以及在途请求数等指标投递到具体的度量后端；
 * 接口本身只约定采集点，不引入任何后端依赖。
 */
public interface MessagingMetrics {

  /**
   * 开启一次请求计时。
   *
   * <p>返回的句柄在请求结束时关闭（try-with-resources），关闭动作即完成耗时的记录。
   *
   * @param name 请求名（subject）
   * @return 计时句柄
   */
  CloseableSilently startRequestTimer(String name);

  /** 记录一次请求的报文体积。 */
  void observeRequestSize(String to, String name, int requestSizeInBytes);

  /** 累计一条单向消息。 */
  void countMessage(String to, String name);

  /** 累计一次请求-应答往返。 */
  void countRequestResponse(String to, String name);

  /** 累计一次成功应答。 */
  void countSuccessResponse(String address, String name);

  /** 累计一次失败应答，附带失败原因。 */
  void countFailureResponse(String address, String name, String error);

  /** 在途请求计数加一。 */
  void incInFlightRequests(String address, String topic);

  /** 在途请求计数减一。 */
  void decInFlightRequests(String address, String topic);
}
