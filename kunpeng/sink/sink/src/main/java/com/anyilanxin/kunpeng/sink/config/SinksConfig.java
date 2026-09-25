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
package com.anyilanxin.kunpeng.sink.config;

import com.anyilanxin.kunpeng.sink.registry.SinkRegistry;
import java.util.Objects;

/**
 * 引擎侧的 Sink 配置摘要：哪些可选内置 Sink 开启，以及可安装到分区上的 Sink 注册表。
 *
 * <p>在 broker 启动时根据 broker 配置构建一次，随后交给每一次分区角色切换。
 *
 * @param enableDebugSink 是否启用内置的 {@link com.anyilanxin.kunpeng.sink.debug.DebugLogSink}
 * @param registry 可加载 Sink 的注册表，永不为 {@code null}
 * @author zxuanhong
 * @since 2026.9.0
 */
public record SinksConfig(boolean enableDebugSink, SinkRegistry registry) {

  public SinksConfig {
    Objects.requireNonNull(registry, "registry must not be null");
  }
}
