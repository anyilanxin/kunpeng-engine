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
package com.anyilanxin.kunpeng.client.spring.jobhandling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Wrapper bean for {@link ScheduledExecutorService} （Spring 装配的 job 处理所需）, Retry Management and so
 * on.
 *
 * <p>This is wrapped, so you can have multiple executor services in the Spring context and qualify
 * the right one.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientExecutorService {

  private final ScheduledExecutorService scheduledExecutorService;
  private final boolean ownedByClient;

  public KunpengClientExecutorService(
      final ScheduledExecutorService scheduledExecutorService, final boolean ownedByClient) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.ownedByClient = ownedByClient;
  }

  public boolean isOwnedByClient() {
    return ownedByClient;
  }

  public ScheduledExecutorService get() {
    return scheduledExecutorService;
  }

  public static KunpengClientExecutorService createDefault() {
    return createDefault(1);
  }

  public static KunpengClientExecutorService createDefault(final int threads) {
    final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(threads);
    return new KunpengClientExecutorService(threadPool, true);
  }
}
