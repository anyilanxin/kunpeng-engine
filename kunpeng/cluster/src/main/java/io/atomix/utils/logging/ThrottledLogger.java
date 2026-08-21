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
package io.atomix.utils.logging;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * A rate-limited {@link Logger} wrapper: identical log events within the throttle window are
 * suppressed, and a summary of the suppressed events is logged once the window elapses.
 *
 * <p>Use for expected-but-noisy events (e.g. rejected requests during reconfiguration) to avoid
 * flooding the log.
 */
public final class ThrottledLogger implements Logger {

  private final Logger wrappedLogger;
  private final long thresholdIntervalMs;
  private final boolean ignoreExitedThrowable;
  private final AtomicLong lastLoggedAt = new AtomicLong(-1);

  public ThrottledLogger(final Logger logger, final Duration throttleInterval) {
    this(logger, throttleInterval, true);
  }

  public ThrottledLogger(
      final Logger logger, final Duration throttleInterval, final boolean ignoreExitedThrowable) {
    this.wrappedLogger = logger;
    this.thresholdIntervalMs = throttleInterval.toMillis();
    this.ignoreExitedThrowable = ignoreExitedThrowable;
  }

  private boolean shouldLog() {
    final var now = System.currentTimeMillis();
    final var lastTime = lastLoggedAt.get();
    return now - lastTime >= thresholdIntervalMs
        && lastLoggedAt.compareAndSet(lastTime, now);
  }

  private boolean shouldLog(final Throwable t) {
    return !(ignoreExitedThrowable && t.getMessage() != null && t.getMessage().contains("exited"))
        && shouldLog();
  }

  @Override
  public String getName() {
    return wrappedLogger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return wrappedLogger.isTraceEnabled();
  }

  @Override
  public void trace(final String msg) {
    if (isTraceEnabled() && shouldLog()) {
      wrappedLogger.trace(msg);
    }
  }

  @Override
  public void trace(final String format, final Object arg) {
    if (isTraceEnabled() && shouldLog()) {
      wrappedLogger.trace(format, arg);
    }
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    if (isTraceEnabled() && shouldLog()) {
      wrappedLogger.trace(format, arg1, arg2);
    }
  }

  @Override
  public void trace(final String format, final Object... arguments) {
    if (isTraceEnabled() && shouldLog()) {
      wrappedLogger.trace(format, arguments);
    }
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    if (isTraceEnabled() && shouldLog(t)) {
      wrappedLogger.trace(msg, t);
    }
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return wrappedLogger.isTraceEnabled(marker);
  }

  @Override
  public void trace(final Marker marker, final String msg) {
    if (isTraceEnabled(marker) && shouldLog()) {
      wrappedLogger.trace(marker, msg);
    }
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg) {
    if (isTraceEnabled(marker) && shouldLog()) {
      wrappedLogger.trace(marker, format, arg);
    }
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
    if (isTraceEnabled(marker) && shouldLog()) {
      wrappedLogger.trace(marker, format, arg1, arg2);
    }
  }

  @Override
  public void trace(final Marker marker, final String format, final Object... argArray) {
    if (isTraceEnabled(marker) && shouldLog()) {
      wrappedLogger.trace(marker, format, argArray);
    }
  }

  @Override
  public void trace(final Marker marker, final String msg, final Throwable t) {
    if (isTraceEnabled(marker) && shouldLog(t)) {
      wrappedLogger.trace(marker, msg, t);
    }
  }

  @Override
  public boolean isDebugEnabled() {
    return wrappedLogger.isDebugEnabled();
  }

  @Override
  public void debug(final String msg) {
    if (isDebugEnabled() && shouldLog()) {
      wrappedLogger.debug(msg);
    }
  }

  @Override
  public void debug(final String format, final Object arg) {
    if (isDebugEnabled() && shouldLog()) {
      wrappedLogger.debug(format, arg);
    }
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    if (isDebugEnabled() && shouldLog()) {
      wrappedLogger.debug(format, arg1, arg2);
    }
  }

  @Override
  public void debug(final String format, final Object... arguments) {
    if (isDebugEnabled() && shouldLog()) {
      wrappedLogger.debug(format, arguments);
    }
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    if (isDebugEnabled() && shouldLog(t)) {
      wrappedLogger.debug(msg, t);
    }
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return wrappedLogger.isDebugEnabled(marker);
  }

  @Override
  public void debug(final Marker marker, final String msg) {
    if (isDebugEnabled(marker) && shouldLog()) {
      wrappedLogger.debug(marker, msg);
    }
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg) {
    if (isDebugEnabled(marker) && shouldLog()) {
      wrappedLogger.debug(marker, format, arg);
    }
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
    if (isDebugEnabled(marker) && shouldLog()) {
      wrappedLogger.debug(marker, format, arg1, arg2);
    }
  }

  @Override
  public void debug(final Marker marker, final String format, final Object... arguments) {
    if (isDebugEnabled(marker) && shouldLog()) {
      wrappedLogger.debug(marker, format, arguments);
    }
  }

  @Override
  public void debug(final Marker marker, final String msg, final Throwable t) {
    if (isDebugEnabled(marker) && shouldLog(t)) {
      wrappedLogger.debug(marker, msg, t);
    }
  }

  @Override
  public boolean isInfoEnabled() {
    return wrappedLogger.isInfoEnabled();
  }

  @Override
  public void info(final String msg) {
    if (isInfoEnabled() && shouldLog()) {
      wrappedLogger.info(msg);
    }
  }

  @Override
  public void info(final String format, final Object arg) {
    if (isInfoEnabled() && shouldLog()) {
      wrappedLogger.info(format, arg);
    }
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    if (isInfoEnabled() && shouldLog()) {
      wrappedLogger.info(format, arg1, arg2);
    }
  }

  @Override
  public void info(final String format, final Object... arguments) {
    if (isInfoEnabled() && shouldLog()) {
      wrappedLogger.info(format, arguments);
    }
  }

  @Override
  public void info(final String msg, final Throwable t) {
    if (isInfoEnabled() && shouldLog(t)) {
      wrappedLogger.info(msg, t);
    }
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return wrappedLogger.isInfoEnabled(marker);
  }

  @Override
  public void info(final Marker marker, final String msg) {
    if (isInfoEnabled(marker) && shouldLog()) {
      wrappedLogger.info(marker, msg);
    }
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg) {
    if (isInfoEnabled(marker) && shouldLog()) {
      wrappedLogger.info(marker, format, arg);
    }
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
    if (isInfoEnabled(marker) && shouldLog()) {
      wrappedLogger.info(marker, format, arg1, arg2);
    }
  }

  @Override
  public void info(final Marker marker, final String format, final Object... arguments) {
    if (isInfoEnabled(marker) && shouldLog()) {
      wrappedLogger.info(marker, format, arguments);
    }
  }

  @Override
  public void info(final Marker marker, final String msg, final Throwable t) {
    if (isInfoEnabled(marker) && shouldLog(t)) {
      wrappedLogger.info(marker, msg, t);
    }
  }

  @Override
  public boolean isWarnEnabled() {
    return wrappedLogger.isWarnEnabled();
  }

  @Override
  public void warn(final String msg) {
    if (isWarnEnabled() && shouldLog()) {
      wrappedLogger.warn(msg);
    }
  }

  @Override
  public void warn(final String format, final Object arg) {
    if (isWarnEnabled() && shouldLog()) {
      wrappedLogger.warn(format, arg);
    }
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    if (isWarnEnabled() && shouldLog()) {
      wrappedLogger.warn(format, arg1, arg2);
    }
  }

  @Override
  public void warn(final String format, final Object... arguments) {
    if (isWarnEnabled() && shouldLog()) {
      wrappedLogger.warn(format, arguments);
    }
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    if (isWarnEnabled() && shouldLog(t)) {
      wrappedLogger.warn(msg, t);
    }
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return wrappedLogger.isWarnEnabled(marker);
  }

  @Override
  public void warn(final Marker marker, final String msg) {
    if (isWarnEnabled(marker) && shouldLog()) {
      wrappedLogger.warn(marker, msg);
    }
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg) {
    if (isWarnEnabled(marker) && shouldLog()) {
      wrappedLogger.warn(marker, format, arg);
    }
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
    if (isWarnEnabled(marker) && shouldLog()) {
      wrappedLogger.warn(marker, format, arg1, arg2);
    }
  }

  @Override
  public void warn(final Marker marker, final String format, final Object... arguments) {
    if (isWarnEnabled(marker) && shouldLog()) {
      wrappedLogger.warn(marker, format, arguments);
    }
  }

  @Override
  public void warn(final Marker marker, final String msg, final Throwable t) {
    if (isWarnEnabled(marker) && shouldLog(t)) {
      wrappedLogger.warn(marker, msg, t);
    }
  }

  @Override
  public boolean isErrorEnabled() {
    return wrappedLogger.isErrorEnabled();
  }

  @Override
  public void error(final String msg) {
    if (isErrorEnabled() && shouldLog()) {
      wrappedLogger.error(msg);
    }
  }

  @Override
  public void error(final String format, final Object arg) {
    if (isErrorEnabled() && shouldLog()) {
      wrappedLogger.error(format, arg);
    }
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    if (isErrorEnabled() && shouldLog()) {
      wrappedLogger.error(format, arg1, arg2);
    }
  }

  @Override
  public void error(final String format, final Object... arguments) {
    if (isErrorEnabled() && shouldLog()) {
      wrappedLogger.error(format, arguments);
    }
  }

  @Override
  public void error(final String msg, final Throwable t) {
    if (isErrorEnabled() && shouldLog(t)) {
      wrappedLogger.error(msg, t);
    }
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return wrappedLogger.isErrorEnabled(marker);
  }

  @Override
  public void error(final Marker marker, final String msg) {
    if (isErrorEnabled(marker) && shouldLog()) {
      wrappedLogger.error(marker, msg);
    }
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg) {
    if (isErrorEnabled(marker) && shouldLog()) {
      wrappedLogger.error(marker, format, arg);
    }
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
    if (isErrorEnabled(marker) && shouldLog()) {
      wrappedLogger.error(marker, format, arg1, arg2);
    }
  }

  @Override
  public void error(final Marker marker, final String format, final Object... arguments) {
    if (isErrorEnabled(marker) && shouldLog()) {
      wrappedLogger.error(marker, format, arguments);
    }
  }

  @Override
  public void error(final Marker marker, final String msg, final Throwable t) {
    if (isErrorEnabled(marker) && shouldLog(t)) {
      wrappedLogger.error(marker, msg, t);
    }
  }

  /** Formats a message slf4j-style; exposed for tests and callers building summaries. */
  static String format(final String format, final Object... args) {
    return MessageFormatter.arrayFormat(format, args).getMessage();
  }
}
