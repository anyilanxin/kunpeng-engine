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
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import com.alibaba.druid.pool.DruidDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程级连接池登记处。一个 broker 内每个分区会各自实例化 sink，但它们指向同一个库； 按「url + 用户名」共享同一个 {@link
 * DruidDataSource}，引用计数归零时才真正关闭，避免连接数随分区数翻倍。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class ConnectionPool {

  private static final Map<String, PooledDataSource> POOLS = new ConcurrentHashMap<>();

  private ConnectionPool() {}

  static PooledDataSource acquire(
      final String url, final String userName, final String password, final int maxPoolSize) {
    final var key = url + "::" + userName;
    synchronized (ConnectionPool.class) {
      final var existing = POOLS.get(key);
      if (existing != null) {
        existing.references.incrementAndGet();
        return existing;
      }
      final var created = new PooledDataSource(key, url, userName, password, maxPoolSize);
      POOLS.put(key, created);
      return created;
    }
  }

  /** 带引用计数的池句柄；{@link #release()} 是幂等的，最后一个释放者负责关池。 */
  static final class PooledDataSource {

    private final String key;
    private final DruidDataSource dataSource;
    private final AtomicInteger references = new AtomicInteger(1);

    private PooledDataSource(
        final String key,
        final String url,
        final String userName,
        final String password,
        final int maxPoolSize) {
      this.key = key;
      dataSource = new DruidDataSource();
      dataSource.setName("kunpeng-sink-rdbms-" + key.hashCode());
      dataSource.setUrl(url);
      dataSource.setUsername(userName);
      dataSource.setPassword(password);
      dataSource.setInitialSize(1);
      dataSource.setMinIdle(1);
      dataSource.setMaxActive(maxPoolSize);
      dataSource.setMaxWait(10_000);
      dataSource.setTestWhileIdle(true);
      dataSource.setValidationQuery("SELECT 1");
    }

    DruidDataSource dataSource() {
      return dataSource;
    }

    void release() {
      synchronized (ConnectionPool.class) {
        if (references.decrementAndGet() > 0) {
          return;
        }
        POOLS.remove(key);
      }
      try {
        dataSource.close();
      } catch (final Exception e) {
        // 关池失败只影响连接回收，进程退出后由数据库侧超时兜底
      }
    }
  }
}
