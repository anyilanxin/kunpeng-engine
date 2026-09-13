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

import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 单次镜像拍摄的内容写入器：把内容文件写入 {@code snapshotDirectory}，目录由镜像模块创建与管理， 业务只写文件、不建/删目录；抛出异常即本次拍摄失败，
 * 临时目录会被清理。
 *
 * <p>拍摄入口据此与具体业务 SPI 解耦——常规 raft 镜像传 {@code RaftSnapshotProvider::takeSnapshot}， 引导/合并镜像直接传
 * {@code TransferSnapshotProvider} 对应方法的直写 lambda，无需再做整接口适配。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
@FunctionalInterface
public interface SnapshotContentWriter {

  /**
   * 把本次镜像内容写入给定目录。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   * @return 业务信息键值清单（随镜像持久化到 snapshot.metadata；可为空/null，null 时不写业务元数据； key/value 均不得包含 '='
   *     与换行，值取 {@code String.valueOf}）
   */
  @Nullable
  Map<String, Object> writeTo(Path snapshotDirectory);
}
