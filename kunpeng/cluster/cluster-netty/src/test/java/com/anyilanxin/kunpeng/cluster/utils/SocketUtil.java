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
package com.anyilanxin.kunpeng.cluster.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;

/** Utility to acquire available local addresses/ports for tests. */
public final class SocketUtil {
  private static final InetAddress LOCALHOST = InetAddress.getLoopbackAddress();

  private SocketUtil() {}

  /**
   * Returns a bindable wildcard address with a currently free port. Note that the port is only
   * likely to be free; there is an inherent race when the caller eventually binds to it.
   */
  public static InetSocketAddress getNextAddress() {
    try (final var socket = new ServerSocket()) {
      socket.setReuseAddress(false);
      socket.bind(new InetSocketAddress(LOCALHOST, 0), 1);
      return new InetSocketAddress(LOCALHOST, socket.getLocalPort());
    } catch (final SocketException e) {
      throw new IllegalStateException("A new socket could not be created", e);
    } catch (final IOException e) {
      throw new IllegalStateException("A free port could not be found", e);
    }
  }
}
