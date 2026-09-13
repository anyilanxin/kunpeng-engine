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
package com.anyilanxin.kunpeng.configuration.broker;

import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/** Broker 根配置，聚合线程、数据、网关与 raft 等子配置。 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class BrokerCfg {

  @NestedConfigurationProperty private BrokerThreadsCfg threads = new BrokerThreadsCfg();

  @NestedConfigurationProperty private DataCfg data = new DataCfg();

  @NestedConfigurationProperty private EmbeddedGatewayCfg gateway = new EmbeddedGatewayCfg();

  @NestedConfigurationProperty private BusinessRaftCfg raft = new BusinessRaftCfg();
  @NestedConfigurationProperty private ManageRaftCfg manage = new ManageRaftCfg();

  @NestedConfigurationProperty private RocksdbCfg rocksdb = new RocksdbCfg();

  @NestedConfigurationProperty private FlowControlCfg flowControl = new FlowControlCfg();

  private Path brokerBase;

  private boolean enableMetricsExporter;

  private boolean enableDebugExporter;

  public void init(final String brokerBase, final Environment environment) {
    this.brokerBase = Path.of(brokerBase);
    applyEnvironment(environment);
    threads.init(this, brokerBase);
    rocksdb.init(this, brokerBase);
    data.init(this, brokerBase);
    gateway.init(this, brokerBase);
    raft.init(this, brokerBase);
    manage.init(this, brokerBase);
  }

  /** 配置覆盖：支持环境变量 KUNPENG_DEBUG、-Dkunpeng.debug、--kunpeng.debug 及配置项 kunpeng.debug。 */
  private void applyEnvironment(final Environment environment) {
    if (Binder.get(environment).bind("kunpeng.debug", Bindable.of(Boolean.class)).orElse(false)) {
      enableDebugExporter = true;
    }
  }
}
