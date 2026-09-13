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
package com.anyilanxin.kunpeng.cluster.config;

import static com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer.SERIALIZER;

import com.anyilanxin.kunpeng.utils.FileDataStoreUtils;
import com.anyilanxin.kunpeng.utils.FileUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * ClusterMetaStore 接口的默认实现，将集群配置序列化后持久化到数据目录下的 .cluster.meta 文件，并通过同步锁保证读写线程安全。
 *
 * @author zxuanhong
 * @since
 */
public class DefaultClusterMetaStore implements ClusterMetaStore {
  private static final String CLUSTER_FILE_NAME = ".cluster.meta";
  private final ClusterConfiguration configuration;
  private final Path configurationFile;

  public DefaultClusterMetaStore(final Path dataRootDirectory) {
    try {
      FileUtil.ensureDirectory(dataRootDirectory);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to create data directory", e);
    }
    configurationFile = dataRootDirectory.resolve(CLUSTER_FILE_NAME);
    configuration = new ClusterConfiguration();
    try {
      readFromFile();
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read cluster meta store", e);
    }
  }

  private void readFromFile() throws IOException {
    synchronized (configuration) {
      final Optional<byte[]> bytes = FileDataStoreUtils.readFromFile(configurationFile);
      if (bytes.isEmpty()) {
        flush();
        return;
      }
      final ClusterConfiguration decoded = SERIALIZER.decode(bytes.get());
      if (decoded != null) {
        configuration.loadConfiguration(decoded);
      }
    }
  }

  @Override
  public ClusterRaftConfiguration getRaftConfiguration() {
    synchronized (configuration) {
      return configuration.getRaftConfiguration().clone();
    }
  }

  @Override
  public ClusterRaftConfiguration updateRaftConfiguration(
      final ClusterRaftConfiguration raftConfiguration) {
    synchronized (configuration) {
      configuration.setRaftConfiguration(raftConfiguration);
      flush();
      return configuration.getRaftConfiguration().clone();
    }
  }

  @Override
  public ClusterAdminConfiguration getAdminConfiguration() {
    synchronized (configuration) {
      return configuration.getAdminConfiguration().clone();
    }
  }

  @Override
  public ClusterAdminConfiguration updateAdminConfiguration(
      final ClusterAdminConfiguration adminConfiguration) {
    synchronized (configuration) {
      configuration.setAdminConfiguration(adminConfiguration);
      flush();
      return configuration.getAdminConfiguration().clone();
    }
  }

  @Override
  public ClusterNodeConfiguration getNodeConfiguration() {
    synchronized (configuration) {
      return configuration.getNodeConfiguration().clone();
    }
  }

  @Override
  public ClusterNodeConfiguration updateNodeConfiguration(
      final ClusterNodeConfiguration nodeConfiguration) {
    synchronized (configuration) {
      configuration.setNodeConfiguration(nodeConfiguration);
      flush();
      return configuration.getNodeConfiguration().clone();
    }
  }

  private void flush() {
    FileDataStoreUtils.writeToFile(configurationFile, SERIALIZER.encode(configuration));
  }

  @Override
  public void close() {
    flush();
  }
}
