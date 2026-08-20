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
package com.anyilanxin.kunpeng.cluster.cluster.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;

/** 测试用临时端口分配器 */
public final class EphemeralPort {

  private EphemeralPort() {}

  /** 从临时端口范围中挑一个可用 TCP 端口 */
  public static InetSocketAddress nextAddress() {
    try (final ServerSocket socket = new ServerSocket()) {
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress("localhost", 0));
      return (InetSocketAddress) socket.getLocalSocketAddress();
    } catch (final IOException e) {
      throw new IllegalStateException("无法分配临时端口", e);
    }
  }

  /** 下一个可用地址 */
  public static java.net.InetSocketAddress getNextAddress() {
    return nextAddress();
  }

  public static int nextPort() {
    try (final ServerSocket socket = new ServerSocket()) {
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress("localhost", 0));
      return socket.getLocalPort();
    } catch (final IOException e) {
      throw new IllegalStateException("无法分配临时端口", e);
    }
  }
}
