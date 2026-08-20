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
import java.nio.file.Path;

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
public interface SnapshotHandler {

  /**
   * 拍摄引导快照（新分区创建场景）：将业务数据写入指定目录。
   *
   * <p>由 BootstrapReplicaSender 在源节点调用；目标分区通过引导传输接收后恢复。
   *
   * @param directory 快照写入目录（vault 的 staging 目录，写完后由 vault 原子提交）
   * @return 完成时 vault 将目录内容封装为快照
   */
  ActorFuture<Void> onTakeBootstrapSnapshot(Path directory);

  /**
   * 拍摄常规快照：将业务数据写入指定目录。
   *
   * <p>由 Raft 层在快照触发点调用（定期 / 日志压缩前 / Leader 主动）。
   *
   * @param directory 快照写入目录
   * @return 完成时 vault 将目录内容封装为快照
   */
  ActorFuture<Void> onTakeSnapshot(Path directory);

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
   * @return 完成时 vault 将目录内容封装为快照
   */
  ActorFuture<Void> onTakeMergeSnapshot(Path directory);

  /**
   * 合并迁移快照到当前分区。
   *
   * <p>目标分区在迁移传输完成后调用，将远端分区的状态合并到本地。
   *
   * @param archivedSnapshot 已接收落档的迁移快照
   * @return 合并完成
   */
  ActorFuture<Void> onMergeSnapshot(ArchivedSnapshot archivedSnapshot);
}
