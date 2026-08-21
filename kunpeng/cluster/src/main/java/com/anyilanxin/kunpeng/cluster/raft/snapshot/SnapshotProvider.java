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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.nio.file.Path;
import java.util.Map;

/**
 * 快照内容拍摄 SPI：具体"拍什么、怎么拍"由业务系统在外部实现，拍摄编排也由业务侧自行驱动。
 *
 * <p>本模块不再内置拍摄编排，业务侧的典型用法（把实现与快照存储串起来）：
 *
 * <pre>{@code
 * SnapshotProvider provider = ...; // 业务实现
 * var store = raftPartitionServer.getPersistedSnapshotStore();
 * final var pending = store.newTransientSnapshot(index, term, provider.businessInfo());
 * pending.ifRight(handle -> {
 *   try {
 *     provider.takeSnapshot(handle.getPath()); // 业务直接写目录
 *     handle.persist();                        // 三阶段提交
 *   } catch (final Exception e) {
 *     handle.abort();
 *   }
 * });
 * }</pre>
 *
 * <p>快照模块负责的部分：为拍摄准备临时目录、生成 manifest（含业务元数据）、逐文件 CRC 校验、
 * 原子提交、保留策略以及提交后的日志自动压缩。
 */
public interface SnapshotProvider {

  /**
   * 拍摄快照内容：把任意数量的内容文件写入 {@code snapshotDirectory}。
   *
   * <p>目录由快照模块创建与管理，业务只写文件、不建/删目录；抛出异常即本次拍摄失败，临时
   * 目录会被清理。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   */
  void takeSnapshot(Path snapshotDirectory) throws Exception;

  /** 业务元数据版本号，随 manifest 持久化，缺省 1。 */
  default int snapshotVersion() {
    return SnapshotMetadata.DEFAULT_VERSION;
  }

  /** 业务信息键值清单，随 manifest 持久化，缺省为空；key/value 不得包含 '=' 与换行。 */
  default Map<String, Object> businessInfo() {
    return Map.of();
  }
}
