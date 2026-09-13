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

import com.anyilanxin.kunpeng.scheduler.CloseableSilently;
import java.nio.file.Path;
import java.util.Map;

/**
 * 跨分区转移镜像拍摄 SPI（引导 + 合并）：只拍不恢复——引导/合并镜像由源分区按此接口拍摄， 接收端走常规快照接收与恢复链路，拍摄端无恢复、目录与
 * store 访问能力。
 *
 * <p>目录由镜像模块创建与管理，业务只写文件、不建/删目录；抛出异常即本次拍摄失败，临时目录会被清理。 需要随镜像传给接收端的信息既可写成目录内文件
 * （目录内全部文件都会作为分片随镜像传输），也可经返回值随镜像持久化到 snapshot.metadata。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface TransferSnapshotProvider extends CloseableSilently {

  /**
   * 拍摄引导镜像内容（跨分区引导新分区）：把各分区共有的内容写入 {@code snapshotDirectory}，不含 raft 位点类数据。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   * @return 业务信息键值清单（随镜像持久化到 snapshot.metadata；可为空/null； key/value 均不得包含 '=' 与换行，值取 {@code
   *     String.valueOf}）
   */
  Map<String, Object> takeBootstrapSnapshot(Path snapshotDirectory);

  /**
   * 拍摄合并镜像内容（分区删除迁移）：把本分区（即将离开）需要迁移的全部数据写入 {@code snapshotDirectory}。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   * @return 业务信息键值清单（随镜像持久化到 snapshot.metadata；可为空/null； key/value 均不得包含 '=' 与换行，值取 {@code
   *     String.valueOf}）
   */
  Map<String, Object> takeMergeSnapshot(Path snapshotDirectory);
}
