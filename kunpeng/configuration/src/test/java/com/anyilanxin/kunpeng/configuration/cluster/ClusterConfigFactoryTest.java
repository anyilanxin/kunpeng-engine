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
package com.anyilanxin.kunpeng.configuration.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link ClusterConfigFactory} 集群配置映射测试：钉住已开放用户配置项到运行时 {@code AeronMessagingConfig} 的传递， 防止映射遗漏导致开关静默失效。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ClusterConfigFactoryTest {

  @Test
  void localIpcFlagFlowsToAeronConfig() {
    final ClusterCfg disabled = new ClusterCfg();
    disabled.setLocalIpcEnabled(false);
    disabled.init("build/configuration-test");
    assertFalse(
        new ClusterConfigFactory().mapConfiguration(disabled, true).getAeronConfig()
            .isLocalIpcEnabled(),
        "kunpeng.cluster.local-ipc-enabled=false 应映射到集群面 Aeron 配置");

    final ClusterCfg defaults = new ClusterCfg();
    defaults.init("build/configuration-test");
    assertTrue(
        new ClusterConfigFactory().mapConfiguration(defaults, false).getAeronConfig()
            .isLocalIpcEnabled(),
        "缺省应保持开启");
  }
}
