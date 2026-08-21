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
package io.atomix.raft.snapshot;

import java.nio.file.Path;
import java.util.Map;

/**
 * 快照内容拍摄 SPI：具体"拍什么、怎么拍"由业务系统实现，创建 Raft 服务时通过
 * {@code RaftServer.builder().withSnapshotProvider(...)} 注入。
 *
 * <p>快照模块负责编排：为拍摄准备临时目录并回调 {@link #takeSnapshot(Path)}，业务把内容文件
 * 写入该目录；拍摄完成后由模块生成 manifest（含业务元数据）、逐文件 CRC 校验、原子提交与保留
 * 策略。业务通过 {@link #snapshotVersion()} 与 {@link #businessInfo()} 提供随快照持久化的元数据。
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
  default Map<String, String> businessInfo() {
    return Map.of();
  }

  /** 缺省占位实现：未注入业务实现时使用，拍摄时抛出明确异常提示接入方式。 */
  final class NoopSnapshotProvider implements SnapshotProvider {

    @Override
    public void takeSnapshot(final Path snapshotDirectory) {
      throw new SnapshotException(
          "No business SnapshotProvider configured; implement SnapshotProvider and pass it via"
              + " the RaftPartition constructor to enable snapshots");
    }
  }
}
