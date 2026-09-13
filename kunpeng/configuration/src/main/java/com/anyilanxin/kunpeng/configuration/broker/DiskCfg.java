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

import com.anyilanxin.kunpeng.configuration.ConfigurationLoggers;
import java.time.Duration;
import org.slf4j.Logger;
import org.springframework.util.unit.DataSize;

/** 磁盘配置，定义磁盘使用监控与空闲空间阈值。 */
public class DiskCfg implements ConfigurationEntry {

  private static final Logger LOG = ConfigurationLoggers.CONFIGURATION_LOGGER;

  private static final boolean DEFAULT_DISK_MONITORING_ENABLED = true;
  private static final DataSize DISABLED_DISK_FREESPACE = DataSize.ofBytes(0);
  private static final Duration DEFAULT_DISK_USAGE_MONITORING_DELAY = Duration.ofSeconds(1);
  private boolean enableMonitoring = DEFAULT_DISK_MONITORING_ENABLED;
  private Duration monitoringInterval = DEFAULT_DISK_USAGE_MONITORING_DELAY;
  private FreeSpaceCfg freeSpace = new FreeSpaceCfg();

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    freeSpace.init(globalConfig, brokerBase);

    if (!enableMonitoring) {
      LOG.info(
          "Disk usage monitoring is disabled, setting required freespace to {}",
          DISABLED_DISK_FREESPACE);
      freeSpace.setReplication(DISABLED_DISK_FREESPACE);
      freeSpace.setProcessing(DISABLED_DISK_FREESPACE);
    }
  }

  public boolean isEnableMonitoring() {
    return enableMonitoring;
  }

  public void setEnableMonitoring(final boolean enableMonitoring) {
    this.enableMonitoring = enableMonitoring;
  }

  public Duration getMonitoringInterval() {
    return monitoringInterval;
  }

  public void setMonitoringInterval(final Duration monitoringInterval) {
    this.monitoringInterval = monitoringInterval;
  }

  public FreeSpaceCfg getFreeSpace() {
    return freeSpace;
  }

  public void setFreeSpace(final FreeSpaceCfg freeSpace) {
    this.freeSpace = freeSpace;
  }

  @Override
  public String toString() {
    return "DiskCfg{" + "enableMonitoring=" + enableMonitoring + ", freeSpace=" + freeSpace + '}';
  }

  /** 空闲空间配置，定义处理与复制所需的最小空闲空间。 */
  public static class FreeSpaceCfg implements ConfigurationEntry {

    private static final DataSize DEFAULT_PROCESSING_FREESPACE = DataSize.ofGigabytes(2);
    private static final DataSize DEFAULT_REPLICATION_FREESPACE = DataSize.ofGigabytes(1);
    private DataSize processing = DEFAULT_PROCESSING_FREESPACE;
    private DataSize replication = DEFAULT_REPLICATION_FREESPACE;

    public DataSize getProcessing() {
      return processing;
    }

    public void setProcessing(final DataSize processing) {
      this.processing = processing;
    }

    public DataSize getReplication() {
      return replication;
    }

    public void setReplication(final DataSize replication) {
      this.replication = replication;
    }

    @Override
    public String toString() {
      return "FreeSpaceCfg{"
          + "processing='"
          + processing
          + '\''
          + ", replication='"
          + replication
          + '\''
          + '}';
    }
  }
}
