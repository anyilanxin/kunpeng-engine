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

package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.utils.AtomixRuntimeException;

/** Top level exception for Store failures. */
public class PrimitiveException extends AtomixRuntimeException {
  public PrimitiveException() {}

  public PrimitiveException(final String message) {
    super(message);
  }

  public PrimitiveException(final Throwable t) {
    super(t);
  }

  /** Store is temporarily unavailable. */
  public static class Unavailable extends PrimitiveException {
    public Unavailable() {}

    public Unavailable(final String message) {
      super(message);
    }
  }

  /** Store operation timeout. */
  public static class Timeout extends PrimitiveException {}

  /** Primitive service exception. */
  public static class ServiceException extends PrimitiveException {
    public ServiceException() {}

    public ServiceException(final String message) {
      super(message);
    }

    public ServiceException(final Throwable cause) {
      super(cause);
    }
  }

  /** Command failure exception. */
  public static class CommandFailure extends PrimitiveException {
    public CommandFailure() {}

    public CommandFailure(final String message) {
      super(message);
    }
  }

  /** Query failure exception. */
  public static class QueryFailure extends PrimitiveException {
    public QueryFailure() {}

    public QueryFailure(final String message) {
      super(message);
    }
  }

  /** Unknown client exception. */
  public static class UnknownClient extends PrimitiveException {
    public UnknownClient() {}

    public UnknownClient(final String message) {
      super(message);
    }
  }

  /** Unknown service exception. */
  public static class UnknownService extends PrimitiveException {
    public UnknownService() {}

    public UnknownService(final String message) {
      super(message);
    }
  }
}
