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

import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 有界指标缓存：按某个 tag 的取值缓存指标实例，防止高基数 tag（如 jobType）撑爆注册中心。
 *
 * <p>超出上限时按访问顺序淘汰最旧条目，并将其指标从注册中心移除。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BoundedMeterCache<M extends AutoCloseable> {

  /** 缓存上限：与 Prometheus client 的常用 max cardinality 对齐 */
  private static final int MAX_CACHED_METERS = 1024;

  private final Map<String, M> cachedMeters =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, M> eldest) {
          if (size() <= MAX_CACHED_METERS) {
            return false;
          }
          try {
            eldest.getValue().close();
          } catch (final Exception e) {
            // 指标淘汰时的关闭失败无需上抛
          }
          return true;
        }
      };

  private BoundedMeterCache(
      final BiFunction<String, String, M> meterFactory, final KeyName keyName) {
    this.factory = meterFactory;
    this.keyName = keyName;
  }

  private final BiFunction<String, String, M> factory;
  private final KeyName keyName;

  /** 以 {@link StatefulGauge.Builder} 作为指标工厂构建缓存 */
  public static BoundedMeterCache<StatefulGauge> of(
      final MeterRegistry registry, final StatefulGauge.Builder provider, final KeyName keyName) {
    return new BoundedMeterCache<>(
        (key, value) -> provider.tag(key, value).withRegistry(registry), keyName);
  }

  /** 获取（或按 tag 值创建）指标实例 */
  public M get(final String tagValue) {
    synchronized (cachedMeters) {
      return cachedMeters.computeIfAbsent(
          tagValue, value -> factory.apply(keyName.asString(), value));
    }
  }

  /** 当前缓存条目数 */
  public int size() {
    synchronized (cachedMeters) {
      return cachedMeters.size();
    }
  }

  /** 清空缓存并移除全部指标 */
  public void clear() {
    synchronized (cachedMeters) {
      final Iterator<M> it = cachedMeters.values().iterator();
      while (it.hasNext()) {
        try {
          it.next().close();
        } catch (final Exception e) {
          // 同上：关闭失败忽略
        }
        it.remove();
      }
    }
  }
}
