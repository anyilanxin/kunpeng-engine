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

/**
 * broker 传输层失败应答错误码（{@code BrokerResponse.code} int 通道）：流控/背压类拒绝，调用方可重试。
 *
 * <p>与 record 级业务拒绝（RejectionType）分属两层：这里只表达"这条命令没能进入日志"，不携带业务语义；wire 格式与编号池零改动。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BrokerResponseCodes {
  /** 日志写入被拒（在途窗口耗尽/事件日志拒绝追加） */
  public static final int APPEND_REJECTED = 1;

  /** sink 滞后背压闸门拒绝（导出积压越过配置阈值，落库排空后自动恢复） */
  public static final int SINK_BACKPRESSURE = 2;

  private BrokerResponseCodes() {}
}
