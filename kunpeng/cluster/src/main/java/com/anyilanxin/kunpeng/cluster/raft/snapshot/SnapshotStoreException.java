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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

/** 快照存储异常族 */
public class SnapshotStoreException extends RuntimeException {

  public SnapshotStoreException(final String message) {
    super(message);
  }

  public SnapshotStoreException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /** 目标快照已存在（或已有更新） */
  public static final class AlreadyExists extends SnapshotStoreException {
    public AlreadyExists(final String message) {
      super(message);
    }
  }

  /** 快照不存在 */
  public static final class NotFound extends SnapshotStoreException {
    public NotFound(final String message) {
      super(message);
    }
  }

  /** 校验不符/内容损坏 */
  public static final class Corrupted extends SnapshotStoreException {
    public Corrupted(final String message) {
      super(message);
    }
  }

  /** 磁盘/IO 失败 */
  public static final class WriteFailure extends SnapshotStoreException {
    public WriteFailure(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
