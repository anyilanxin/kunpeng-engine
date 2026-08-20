/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.ArchivedSnapshot;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;

/**
 * 快照操作处理器：由创建 {@code RaftPartition} 的调用方（业务层）实现，
 * Raft 层在合适的生命周期节点回调。
 *
 * <p>三类快照场景：
 * <ul>
 *   <li><b>常规快照</b>——{@link #onTakeSnapshot} 定期/触发式拍摄，
 *       {@link #onRecoverFromSnapshot} 在本节点落后过多、Leader 发送快照时恢复</li>
 *   <li><b>引导快照</b>——{@link #onTakeBootstrapSnapshot} 新分区创建时从源节点拍摄，
 *       {@link #onRecoverFromSnapshot} 新节点首次启动时恢复</li>
 *   <li><b>迁移快照</b>——{@link #onTakeMergeSnapshot} 分区迁移时从源分区拍摄，
 *       {@link #onMergeSnapshot} 目标分区接收并合并</li>
 * </ul>
 *
 * <p>{@link #onRecoverFromSnapshot} 在初始引导与 Leader 发送快照两种场景均会被调用。
 */
public interface SnapshotHandler<T extends SnapshotMeta> {

  /**
   * 拍摄引导快照（新分区创建场景）：将业务数据写入指定目录。
   *
   * <p>由 BootstrapReplicaSender 在源节点调用；目标分区通过引导传输接收后恢复。
   *
   * @param directory 快照写入目录（vault 的 staging 目录，写完后由 vault 原子提交）
   * @return 快照元数据；vault 调用 {@link #encodeSnapshotMeta} 序列化后写入 snapshot.metadata
   */
  ActorFuture<T> onTakeBootstrapSnapshot(Path directory);

  /**
   * 拍摄常规快照：将业务数据写入指定目录。
   *
   * <p>由 Raft 层在快照触发点调用（定期 / 日志压缩前 / Leader 主动）。
   *
   * @param directory 快照写入目录
   * @return 快照元数据；vault 调用 {@link #encodeSnapshotMeta} 序列化后写入 snapshot.metadata
   */
  ActorFuture<T> onTakeSnapshot(Path directory);

  /**
   * 从快照恢复业务状态。
   *
   * <p>两种调用场景：
   * <ol>
   *   <li>初始引导——新节点首次启动时从引导快照恢复</li>
   *   <li>Leader 发送——本节点日志落后过多，Leader 通过 Install 协议发送快照后恢复</li>
   * </ol>
   *
   * @param archivedSnapshot 已落档的快照（含目录、清单、元数据）
   * @return 恢复完成
   */
  ActorFuture<Void> onRecoverFromSnapshot(ArchivedSnapshot archivedSnapshot);

  /**
   * 拍摄迁移快照（分区迁移场景）：将业务数据写入指定目录。
   *
   * <p>由 MergeReplicaSender 在源分区节点调用；目标分区通过迁移传输接收。
   *
   * @param directory 快照写入目录
   * @return 快照元数据；vault 调用 {@link #encodeSnapshotMeta} 序列化后写入 snapshot.metadata
   */
  ActorFuture<T> onTakeMergeSnapshot(Path directory);

  /**
   * 合并迁移快照到当前分区。
   *
   * <p>目标分区在迁移传输完成后调用，将远端分区的状态合并到本地。
   *
   * @param archivedSnapshot 已接收落档的迁移快照
   * @return 合并完成
   */
  ActorFuture<Void> onMergeSnapshot(ArchivedSnapshot archivedSnapshot);

  /**
   * 反序列化快照元数据（raft 加载落档快照时调用）。
   *
   * @param bytes snapshot.metadata 文件内容
   * @return 快照元数据
   */
  T decodeSnapshotMeta(byte[] bytes);

  /**
   * 序列化快照元数据（raft 拍摄完成后调用，写入 snapshot.metadata）。
   *
   * @param t 拍摄回调返回的快照元数据
   * @return 序列化字节
   */
  byte[] encodeSnapshotMeta(T t);


  /**
   * 关闭业务系统资源。
   *
   * <p>由 Raft 层在分区关闭时调用：引擎先拍摄一次终局快照（经
   * {@link #onTakeSnapshot}，业务位置沿用最近一次快照），完成后调用本方法释放资源。
   *
   * @return 关闭完成
   */
  ActorFuture<Void> onClose();

  /**
   * 提供快照目录的逐文件校验和（如存储引擎级校验和，适配不同校验算法）。
   *
   * <p>引擎构建快照清单时调用：未覆盖的文件回退为引擎计算的 CRC32C。
   * 默认实现为递归遍历快照目录下全部常规文件（含子目录），key 为相对路径
   * （子目录下的文件为 {@code 子目录/文件名}），value 为 CRC32 校验和。
   *
   * @param snapshotPath 快照目录
   * @return 文件相对路径 → 校验和字符串
   */
  default Map<String, String> onSnapshotChecksums(final Path snapshotPath) {
    final Map<String, String> checksums = new TreeMap<>();
    try (final var files = Files.walk(snapshotPath)) {
      files.filter(Files::isRegularFile)
        .forEach(file -> checksums.put(
          snapshotPath.relativize(file).toString().replace('\\', '/'),
          crc32Hex(file)));
    } catch (final IOException e) {
      throw new UncheckedIOException("快照校验和遍历失败: " + snapshotPath, e);
    }
    return checksums;
  }

  /**
   * 逐文件 CRC32（8KB 流式读取），十六进制字符串
   */
  private static String crc32Hex(final Path file) {
    final var crc = new CRC32();
    try (final var input = Files.newInputStream(file)) {
      final byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        crc.update(buffer, 0, read);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("快照校验和计算失败: " + file, e);
    }
    return Long.toHexString(crc.getValue());
  }
}
