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
package com.anyilanxin.kunpeng.scheduler.core;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 阻塞动作外包池（cached pool; runBlocking 语义） */
public final class BlockingRunner {

  private static final long DEFAULT_SHUTDOWN_SECONDS = 300;

  private final ExecutorService executor;
  private final SchedulerMetrics metrics;

  public BlockingRunner(final String schedulerName, final SchedulerMetrics metrics) {
    this.metrics = metrics == null ? SchedulerMetrics.noop() : metrics;
    final var factory =
        new java.util.concurrent.ThreadFactory() {
          private final java.util.concurrent.atomic.AtomicInteger counter =
              new java.util.concurrent.atomic.AtomicInteger();

          @Override
          public Thread newThread(final Runnable r) {
            final Thread thread =
                new Thread(r, schedulerName + "-blocking-task-runner-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
          }
        };
    this.executor =
        new ThreadPoolExecutor(
            0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), factory);
  }

  public void submit(final Runnable task) {
    metrics.incBlockingSubmitted();
    executor.execute(
        () -> {
          try {
            task.run();
          } finally {
            metrics.incBlockingCompleted();
          }
        });
  }

  public void shutdown(final Duration timeout) {
    executor.shutdown();
    try {
      executor.awaitTermination(
          timeout == null ? DEFAULT_SHUTDOWN_SECONDS : timeout.getSeconds(), TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
