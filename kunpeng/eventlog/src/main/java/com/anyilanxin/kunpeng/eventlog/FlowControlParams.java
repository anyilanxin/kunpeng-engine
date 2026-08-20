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
package com.anyilanxin.kunpeng.eventlog;

import java.time.Duration;

/**
 * 流控参数（builder 一次性注入，运行期不可变）。
 *
 * <ul>
 *   <li>AIMD 在途窗口：仅约束 {@link WriteContext#USER_COMMAND}；RTT 梯度（EMA/minRTT） 低于容差时线性增，超限按梯度乘性减
 *   <li>令牌桶：写入速率（permits = 批 entry 数）
 *   <li>积压调节：written-exported 差超阈值时按观察速率压低令牌桶
 * </ul>
 *
 * @param requestWindowInitial 初始窗口（默认 100）
 * @param requestWindowMin 窗口下限（默认 10）
 * @param requestWindowMax 窗口上限（默认 1000）
 * @param rttTolerance 梯度容差（默认 0.1，容差内不动窗口）
 * @param writeRateLimitPerSecond 写入速率上限（entry 数/秒；<=0 关闭）
 * @param writeRateBurst 令牌桶突发容量（默认 = 1 秒配额）
 * @param minThrottleRate 积压压低的速率下限（默认 10）
 * @param acceptableBacklog 可接受积压条数（默认 100，超出开始降速）
 * @param throttleResolution 积压调节分辨率（默认 10s）
 */
public record FlowControlParams(
    int requestWindowInitial,
    int requestWindowMin,
    int requestWindowMax,
    double rttTolerance,
    double writeRateLimitPerSecond,
    double writeRateBurst,
    double minThrottleRate,
    long acceptableBacklog,
    Duration throttleResolution) {

  public static FlowControlParams defaults() {
    return new FlowControlParams(100, 10, 1000, 0.1, -1, 0, 10, 100, Duration.ofSeconds(10));
  }

  /** 全部关闭（仅保序与水位记账，不做任何拒绝/调节） */
  public static FlowControlParams disabled() {
    return new FlowControlParams(
        Integer.MAX_VALUE,
        1,
        Integer.MAX_VALUE,
        0.0,
        -1,
        0,
        0,
        Long.MAX_VALUE,
        Duration.ofSeconds(10));
  }

  /** AIMD 窗口是否生效（窗口上限 > 1 视为开启） */
  public boolean aimdEnabled() {
    return requestWindowMax > 1;
  }

  /** 写速率限制是否生效 */
  public boolean rateLimitEnabled() {
    return writeRateLimitPerSecond > 0;
  }
}
