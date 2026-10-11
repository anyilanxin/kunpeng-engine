/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.core;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;

/**
 * 传输面策略：统一封装之上的面别差异（请求寻址与响应记录物化）。
 *
 * <p>业务面/管理面各自提供实现：业务面按请求类别寻址分区 leader，管理面固定寻址管理分区； 响应侧按面别的 valueType/lifeCycle 编号池物化具体 Record 实例。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BrokerPlane {

  /**
   * 按请求解析目标主题与地址。
   *
   * @param request 待发送请求
   * @return 目标（主题 + 地址）
   * @throws BrokerException 无法解析目标（无分区/无 leader/地址不可得等明确失败）
   */
  Destination resolve(BrokerRequest<?> request) throws BrokerException;

  /**
   * 按统一响应封装物化面别 Record 实例并装载数据帧。
   *
   * @param response 统一响应读取器
   * @return 已装载的记录实例；无数据（lifeCycle 未知或 hasData 为假）时返回 null
   */
  ResponseRecordValue materialize(ApiResponseReader response);

  record Destination(String topicName, Address apiAddress) {}
}
