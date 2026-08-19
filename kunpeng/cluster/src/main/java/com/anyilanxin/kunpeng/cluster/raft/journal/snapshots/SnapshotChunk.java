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
package com.anyilanxin.kunpeng.cluster.raft.journal.snapshots;


/** 快照传输块（线上单元，SBE 序列化） */
public interface SnapshotChunk {

  /** 所属快照标识（目录名） */
  String getSnapshotId();

  /** 该快照总块数 */
  int getTotalCount();

  /** 块标识 "{fileName}:{offset}" */
  String getChunkName();

  /** 块内容 CRC */
  long getChecksum();

  /** 快照综合校验 */
  long getSnapshotChecksum();

  /** 块字节内容 */
  byte[] getContent();
}
