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
package com.anyilanxin.kunpeng.sink.debug;

import com.anyilanxin.kunpeng.configuration.broker.SinkCfg;
import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.sink.api.context.SinkController;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * 内置 Sink ：把每条记录以 JSON 写到日志——开发或排障时快速查看事件流的便捷方式。
 *
 * <p>配置项：{@code logLevel}（trace/debug/info/warn/error，默认 debug）与 {@code prettyPrint}（默认 false）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DebugLogSink implements RecordSink {

  private Settings settings;
  private ObjectMapper json;
  private LogLine logLine;

  /** 记录输出的日志级别，在 {@link #initialize(SinkContext)} 中解析。 */
  public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
  }

  /** 本 Sink 的用户配置项。 */
  public static final class Settings {
    private String logLevel = "debug";
    private boolean prettyPrint = false;

    public String getLogLevel() {
      return logLevel;
    }

    public void setLogLevel(final String logLevel) {
      this.logLevel = logLevel;
    }

    public boolean isPrettyPrint() {
      return prettyPrint;
    }

    public void setPrettyPrint(final boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
    }
  }

  @FunctionalInterface
  private interface LogLine {
    void log(String format, Object... arguments);
  }

  private static final Map<LogLevel, Function<Logger, LogLine>> LOG_LINES =
      new EnumMap<>(LogLevel.class);

  static {
    LOG_LINES.put(LogLevel.TRACE, logger -> logger::trace);
    LOG_LINES.put(LogLevel.DEBUG, logger -> logger::debug);
    LOG_LINES.put(LogLevel.INFO, logger -> logger::info);
    LOG_LINES.put(LogLevel.WARN, logger -> logger::warn);
    LOG_LINES.put(LogLevel.ERROR, logger -> logger::error);
  }

  @Override
  public void initialize(final SinkContext context) {
    settings = context.getConfiguration().createSettings(Settings.class);
    final var wantedLevel = parseLevel(settings.getLogLevel());
    logLine = LOG_LINES.get(wantedLevel).apply(context.getLogger());
  }

  private LogLevel parseLevel(final String rawLevel) {
    final var normalized = rawLevel == null ? "" : rawLevel.trim().toUpperCase();
    return Arrays.stream(LogLevel.values())
        .filter(level -> level.name().equals(normalized))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Expected debug sink logLevel to be one of %s but got '%s'"
                        .formatted(Arrays.toString(LogLevel.values()), rawLevel)));
  }

  @Override
  public void start(final SinkController controller) {
    logLine.log("Debug sink started");

    final var builder = JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    if (settings.prettyPrint) {
      builder.enable(SerializationFeature.INDENT_OUTPUT);
    }
    json = builder.build();
  }

  @Override
  public void close() {
    logLine.log("Debug sink closed");
  }

  @Override
  public void sink(final BusinessEventRecord<?> record) {
    try {
      logLine.log("{}", json.writeValueAsString(record));
    } catch (final JacksonException e) {
      logLine.log("Failed to serialize record {} to JSON", record, e);
    }
  }

  /**
   * @return 引擎默认登记本 Sink 所用的 id
   */
  public static String defaultSinkId() {
    return DebugLogSink.class.getSimpleName();
  }

  /**
   * @return 指向本 Sink 的 broker 配置项
   */
  public static SinkCfg defaultConfig() {
    final var config = new SinkCfg();
    config.setClassName(DebugLogSink.class.getName());
    return config;
  }
}
