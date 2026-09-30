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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable;

import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.scheduler.CloseableSilently;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;
import java.util.Map;

/**
 * 跨分区转移镜像拍摄 SPI（引导 + 合并）：只拍不恢复——引导/合并镜像由源分区按此接口拍摄， 接收端走常规快照接收与恢复链路，拍摄端无恢复、目录与 store 访问能力。
 *
 * <p>目录由镜像模块创建与管理，业务只写文件、不建/删目录；抛出异常即本次拍摄失败，临时目录会被清理。 需要随镜像传给接收端的信息既可写成目录内文件
 * （目录内全部文件都会作为分片随镜像传输），也可经返回值随镜像持久化到 snapshot.metadata。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface TransferSnapshotProvider extends CloseableSilently {
  void setRaftPartition(RaftPartition partition);

  /**
   * 拍摄引导镜像内容（跨分区引导新分区）：把各分区共有的内容写入 {@code snapshotDirectory}，不含 raft 位点类数据。
   *
   * <p>实现应以本分区最新持久化 raft 镜像为一致视图来源裁剪； 只读打开读不到 WAL，不能直接读运行目录。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   * @param parameters 拍摄参数（由实现自行解析）
   * @return 业务信息键值清单（随镜像持久化到 snapshot.metadata；可为空/null； key/value 均不得包含 '=' 与换行，值取 {@code
   *     String.valueOf}）
   */
  Map<String, Object> takeBootstrapSnapshot(Path snapshotDirectory, Map<String, String> parameters);

  /**
   * 拍摄合并镜像内容（分区迁移）：把本分区需要迁移的数据写入 {@code snapshotDirectory}。
   *
   * <p>推送合并的参数由 {@code RaftPartition} 推送入口传入（本地拍摄使用）；拉取合并的参数由对端 {@code RaftPartition}
   * 拉取入口传入（随拍摄命令带给本端使用）。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   * @param parameters 拍摄参数（由实现自行解析）
   * @return 业务信息键值清单（随镜像持久化到 snapshot.metadata；可为空/null； key/value 均不得包含 '=' 与换行，值取 {@code
   *     String.valueOf}）
   */
  Map<String, Object> takeMergeSnapshot(Path snapshotDirectory, Map<String, String> parameters);

  /**
   * 合并结果回调（合并执行端，合并流终止时回调一次），返回的 future 完成即业务对结果的处理完成， 整个合并流等待其完成后才向发起方（推送端/拉取方）应答。
   *
   * <p>结果回调完成后传输层无论成败都会删除本地落地的合并镜像——失败时业务对镜像的处理（如数据回滚）须在回调内完成。
   *
   * @param result 合并结果：true=成功，false=失败
   * @param message 失败原因描述；成功为 null
   * @param persistedSnapshot 成功为 null；失败为本次接收的合并镜像（仅在回调期间有效，回调返回后即被传输层删除）
   * @param parameters 拍摄该镜像时的参数（推送：源端拍摄参数随信息批传至；拉取：发起方传入的拍摄参数）
   * @return 业务处理完成 future；异常完成即业务处理失败，合并流整体以失败应答
   */
  ActorFuture<Void> mergeSnapshotResult(
      boolean result,
      String message,
      PersistedSnapshot persistedSnapshot,
      Map<String, String> parameters);
}
