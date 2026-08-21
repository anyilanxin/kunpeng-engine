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
package com.anyilanxin.kunpeng.utils.micrometer;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 基于 Micrometer 的耗时统计句柄。
 *
 * <p>典型用法是配合 try-with-resources：进入代码块时通过 {@link #start()} 开始采样， 代码块结束时由 {@link #close()}
 * 停止采样并将耗时记录到指定的 {@link Timer}：
 *
 * <pre>{@code
 * try (CloseableTime time = new CloseableTime(timer, registry).start()) {
 *   // 需要统计耗时的逻辑
 * }
 * }</pre>
 *
 * @author zxuanhong
 * @date 2025-12-16 16:34
 */
public final class CloseableTime implements CloseableSilently {
  private final Timer timer;
  private final MeterRegistry registry;
  private Timer.Sample sample;

  /**
   * @param timer 耗时最终记录到的目标计时器
   * @param registry 采样所用的指标注册中心
   */
  public CloseableTime(final Timer timer, final MeterRegistry registry) {
    this.timer = timer;
    this.registry = registry;
  }

  /** 停止采样并将耗时记录到目标 {@link Timer}；若尚未 {@link #start()} 则不做任何事。 */
  @Override
  public void close() {
    if (sample != null) {
      sample.stop(timer);
    }
  }

  /** 开始计时采样，返回自身以支持链式调用与 try-with-resources。 */
  public CloseableTime start() {
    sample = Timer.start(registry);
    return this;
  }
}
