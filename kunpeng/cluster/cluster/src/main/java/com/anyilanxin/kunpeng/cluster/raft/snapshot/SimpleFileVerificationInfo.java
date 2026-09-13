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

import java.nio.file.Path;

/**
 * .sfc 校验文件：SFV 行式的逐文件校验清单，与镜像目录同名、位于同级， 兼作镜像"提交完成"标记——启动时缺失标记的目录视为未完整提交并清除。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface SimpleFileVerificationInfo extends SimpleFileVerificationChecksums {

  /** .sfc 文件路径。 */
  Path getSfvPath();

  /**
   * 校验镜像目录与本校验集是否一致：仅核对清单内文件，目录中多出的文件不参与校验； 非元数据文件的校验信息由拍摄该镜像时的 {@link SnapshotFileInfoProvider}
   * 重新计算比对（算法与拍摄对称）， {@code snapshot.metadata} 固定走默认内容 CRC32。
   */
  boolean verify(Path snapshotDirectory, SnapshotFileInfoProvider provider);
}
