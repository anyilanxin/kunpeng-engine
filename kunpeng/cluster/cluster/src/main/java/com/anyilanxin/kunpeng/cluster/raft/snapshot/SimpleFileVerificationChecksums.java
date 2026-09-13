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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.util.Map;

/**
 * 镜像逐文件校验集的只读视图。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface SimpleFileVerificationChecksums {

  /** 文件名 → 校验信息（size + CRC32）。 */
  Map<String, SnapshotFileInfo> getFileInfos();

  /** 全部文件校验信息合并出的整体校验和。 */
  long getCombinedChecksum();

  /** 与另一份校验集逐文件比对。 */
  default boolean sameChecksums(final SimpleFileVerificationChecksums other) {
    return other != null && getFileInfos().equals(other.getFileInfos());
  }
}
