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
package com.anyilanxin.kunpeng.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink 运行时的日志命名空间；每个 Sink 实例都有自己的子日志器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkLoggers {

  private static final String SINK_LOGGER_NAME = "com.anyilanxin.kunpeng.sink";

  public static final Logger SINK = LoggerFactory.getLogger(SINK_LOGGER_NAME);

  private SinkLoggers() {}

  /**
   * @param sinkId 单个 Sink 实例的 id
   * @return 该 Sink 专属的日志器，便于按 Sink 单独调整日志级别
   */
  public static Logger forSink(final String sinkId) {
    return LoggerFactory.getLogger(SINK_LOGGER_NAME + "." + sinkId);
  }
}
