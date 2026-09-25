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
package com.anyilanxin.kunpeng.sink.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Caffeine 实体缓存测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class CaffeineEntityCacheTest {

  @Test
  void loadsMissingEntriesOnlyOnce() {
    // 假设
    final var loads = new AtomicInteger();
    final SinkEntityCache<String, String> cache =
        new CaffeineEntityCache<>(key -> {
          loads.incrementAndGet();
          return key.toUpperCase();
        });

    // 当
    final var first = cache.getOrLoad("a");
    final var second = cache.getOrLoad("a");

    // 则
    assertThat(first).isEqualTo("A");
    assertThat(second).isEqualTo("A");
    assertThat(loads).hasValue(1);
    assertThat(cache.estimatedSize()).isEqualTo(1);
  }

  @Test
  void getDoesNotTriggerTheLoader() {
    // 假设
    final var loads = new AtomicInteger();
    final SinkEntityCache<String, String> cache =
        new CaffeineEntityCache<>(key -> {
          loads.incrementAndGet();
          return key;
        });

    // 当
    assertThat(cache.get("missing")).isNull();

    // 则
    assertThat(loads).hasValue(0);
  }

  @Test
  void putOverwritesAndInvalidateRemoves() {
    // 假设
    final SinkEntityCache<String, Integer> cache = new CaffeineEntityCache<>(k -> 0);
    cache.put("k", 1);

    // 当
    cache.put("k", 2);
    assertThat(cache.get("k")).isEqualTo(2);

    // 则
    cache.invalidate("k");
    assertThat(cache.get("k")).isNull();
    assertThat(cache.estimatedSize()).isZero();
  }

  @Test
  void evictsWhenMaximumSizeIsExceeded() {
    // 假设：direct executor 让 Caffeine 同步执行淘汰维护，测试可以确定性地观察到容量上限
    final SinkEntityCache<Integer, Integer> cache =
        new CaffeineEntityCache<>(
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(2)
                .executor(Runnable::run),
            k -> k);

    // 当
    cache.getOrLoad(1);
    cache.getOrLoad(2);
    cache.getOrLoad(3);
    cache.getOrLoad(4);

    // 则
    assertThat(cache.estimatedSize()).isLessThanOrEqualTo(2);
  }

  @Test
  void clearDropsEverything() {
    // 假设
    final SinkEntityCache<Integer, Integer> cache = new CaffeineEntityCache<>(k -> k);
    cache.getOrLoad(1);
    cache.getOrLoad(2);

    // 当
    cache.clear();

    // 则
    assertThat(cache.estimatedSize()).isZero();
    assertThat(cache.get(1)).isNull();
  }
}
