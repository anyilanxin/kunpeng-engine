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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import java.nio.file.Path;

/** 快照磁盘布局常量（落档目录名 = SnapshotRef 六段字符串） */
public final class SnapshotLayout {

  /** 快照根目录（三个区在其下一层） */
  public static final String SNAPSHOTS_DIR = "snapshots";

  /** 主快照区（{@code onTakeSnapshot} 常规快照落档） */
  public static final String SNAPSHOT_DIR = "snapshot";

  /** bootstrap 副本区 */
  public static final String BOOTSTRAP_DIR = "bootstrap";

  /** merge 副本区 */
  public static final String MERGE_DIR = "merge";

  /** merge 运行时目录 */
  public static final String MERGE_RUNTIME_DIR = "merge-runtime";

  /** 暂存目录前缀（写入中/接收中） */
  public static final String STAGING_PREFIX = "staging-";

  /** SFV 清单文件后缀（与快照目录同级的同名文件，无此文件 = 部分写，扫描时删除） */
  public static final String MANIFEST_SUFFIX = ".sfv";

  /** 目录内元数据文件名 */
  public static final String METADATA_FILE = "snapshot.metadata";

  /** 快照目录对应的同级 SFV 清单文件（&lt;快照目录名&gt;.sfv） */
  public static Path manifestPath(final Path snapshotDirectory) {
    return snapshotDirectory.resolveSibling(
        snapshotDirectory.getFileName() + MANIFEST_SUFFIX);
  }

  private SnapshotLayout() {}
}
