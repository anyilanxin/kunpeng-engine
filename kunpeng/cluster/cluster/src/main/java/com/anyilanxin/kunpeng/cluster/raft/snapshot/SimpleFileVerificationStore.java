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
import java.util.Map;

/**
 * .sfc 校验文件的读写入口。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface SimpleFileVerificationStore {

  /** 为已就位的镜像目录生成 .sfc 校验文件（先写 .tmp 再 move + force 落盘）。 */
  SimpleFileVerificationInfo newFileVerificationInfo(
      Path snapshotPath, Map<String, SnapshotFileInfo> snapshotFileInfo);

  /** 读取镜像目录对应的 .sfc 校验文件。 */
  SimpleFileVerificationInfo load(Path snapshotPath);

  /** 镜像目录是否存在对应的 .sfc 校验文件。 */
  boolean exists(Path snapshotPath);

  /** 删除镜像目录对应的 .sfc 校验文件。 */
  void delete(Path snapshotPath);
}
