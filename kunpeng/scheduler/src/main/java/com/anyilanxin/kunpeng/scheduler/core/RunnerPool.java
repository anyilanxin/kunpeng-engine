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

import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** 平台线程池（CPU/IO 各一实例）：固定 runner + 随机路由 + 软上限溢出转移 */
public final class RunnerPool implements CellPool {

  private final StealingRunner[] runners;
  private final AtomicInteger routeCursor = new AtomicInteger();
  private final String name;
  private final String poolShortName;
  private final SchedulerMetrics metrics;

  public RunnerPool(
      final int threadCount,
      final String namePrefix,
      final ThreadFactory threadFactory,
      final BlockingRunner blocking,
      final ActorClock sharedClock,
      final SchedulerMetrics metrics) {
    this.name = namePrefix;
    this.poolShortName = derivePoolName(namePrefix);
    this.metrics = metrics;
    this.runners = new StealingRunner[threadCount];
    for (int i = 0; i < threadCount; i++) {
      runners[i] =
          new StealingRunner(
              this, namePrefix + "-" + i, threadFactory, blocking, sharedClock, metrics);
    }
    metrics.registerQueueDepthGauge(
        poolShortName,
        () -> {
          long total = 0;
          for (final StealingRunner runner : runners) {
            total += runner.getQueue().size();
          }
          return total;
        });
  }

  /** "{scheduler}-cpu-scheduler" → "cpu" */
  private static String derivePoolName(final String namePrefix) {
    final String stripped =
        namePrefix.endsWith("-scheduler")
            ? namePrefix.substring(0, namePrefix.length() - "-scheduler".length())
            : namePrefix;
    final int dash = stripped.lastIndexOf('-');
    return dash > 0 ? stripped.substring(dash + 1) : stripped;
  }

  @Override
  public String poolName() {
    return poolShortName;
  }

  public void start() {
    for (final StealingRunner runner : runners) {
      runner.start();
    }
  }

  public void stop() {
    for (final StealingRunner runner : runners) {
      runner.signalStop();
    }
    for (final StealingRunner runner : runners) {
      runner.join();
    }
  }

  /** 唤醒路由：优先 home runner（缓存局部性）, 饱和则轮转, 全饱和挑最浅强投——永不丢弃唤醒 */
  public void route(final ActorCell cell) {
    final CellRunner home = cell.getHomeRunner();
    if (home instanceof final StealingRunner homeRunner
        && homeRunner.getQueue().offerIfNotSaturated(cell)) {
      homeRunner.hint();
      return;
    }
    final int begin = routeCursor.getAndIncrement();
    for (int i = 0; i < runners.length; i++) {
      final StealingRunner runner = runners[Math.floorMod(begin + i, runners.length)];
      if (runner.getQueue().offerIfNotSaturated(cell)) {
        runner.hint();
        return;
      }
    }
    // 全部饱和（cell 数远超软上限的极端态）: 挑当前最浅的队列强制入队——
    // 丢唤醒会让 cell 永久卡死 WAKING_UP（gate 再无人 CAS 成功）, 宁可超限
    StealingRunner shallowest = runners[0];
    for (final StealingRunner runner : runners) {
      if (runner.getQueue().size() < shallowest.getQueue().size()) {
        shallowest = runner;
      }
    }
    shallowest.getQueue().offer(cell);
    shallowest.hint();
  }

  public int runnerCount() {
    return runners.length;
  }

  public StealingRunner runnerAt(final int index) {
    return runners[index];
  }
}
