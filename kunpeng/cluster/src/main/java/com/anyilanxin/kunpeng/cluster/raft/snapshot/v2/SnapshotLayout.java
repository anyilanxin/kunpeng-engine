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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.v2;

/** 快照磁盘布局常量（落档目录名 = SnapshotRef 六段字符串） */
public final class SnapshotLayout {

  /** 主快照目录 */
  public static final String SNAPSHOTS_DIR = "snapshots";

  /** bootstrap 副本缓存目录 */
  public static final String BOOTSTRAP_DIR = "bootstrap-snapshots";

  /** merge 副本缓存目录 */
  public static final String MERGE_DIR = "merge-snapshots";

  /** merge 运行时目录 */
  public static final String MERGE_RUNTIME_DIR = "merge-runtime";

  /** 暂存目录前缀（写入中/接收中） */
  public static final String STAGING_PREFIX = "staging-";

  /** 目录内清单文件名（无此文件 = 部分写，扫描时删除） */
  public static final String MANIFEST_FILE = "manifest.bin";

  /** 目录内元数据文件名 */
  public static final String METADATA_FILE = "snapshot.metadata";

  private SnapshotLayout() {}
}
