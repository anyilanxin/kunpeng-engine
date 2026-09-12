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

/**
 * 镜像模块异常基类。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public class SnapshotException extends RuntimeException {

  public SnapshotException(final String message) {
    super(message);
  }

  public SnapshotException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public SnapshotException(final Throwable cause) {
    super(cause);
  }

  /** 相同或更新的镜像已存在。 */
  public static final class SnapshotAlreadyExistsException extends SnapshotException {

    public SnapshotAlreadyExistsException(final String message) {
      super(message);
    }

    public SnapshotAlreadyExistsException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /** 镜像不存在（可能已被删除）。 */
  public static final class SnapshotNotFoundException extends SnapshotException {

    public SnapshotNotFoundException(final String message) {
      super(message);
    }
  }

  /** 镜像内容损坏（校验失败）。 */
  public static final class CorruptedSnapshotException extends SnapshotException {

    public CorruptedSnapshotException(final String message) {
      super(message);
    }

    public CorruptedSnapshotException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
