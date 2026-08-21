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
package io.atomix.utils.concurrent;

import org.slf4j.Logger;

/** Handler invoked when an unrecoverable error occurs on a component's thread. */
@FunctionalInterface
public interface FatalErrorHandler {

  /**
   * Called when an error cannot be recovered from; the component is expected to shut down.
   *
   * @param error the fatal error
   */
  void handleError(Throwable error);

  /**
   * Returns a handler which logs the error (and its stack trace) before rethrowing it.
   *
   * @param logger the logger to log to
   * @return a logging fatal error handler
   */
  static FatalErrorHandler withLogger(final Logger logger) {
    return error -> {
      if (error instanceof final Error error1) {
        // errors (e.g. OOM) should not be swallowed; log and propagate to fail fast
        logger.error("Fatal error, terminating", error1);
        throw error1;
      }
      logger.error(
          "Unrecoverable error, this thread will terminate; the component owning it should be"
              + " restarted",
          error);
    };
  }
}
