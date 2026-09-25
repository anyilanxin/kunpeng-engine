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
package com.anyilanxin.kunpeng.cluster.utils.net;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import org.junit.Test;

/** Address test. */
public class AddressTest {
  @Test
  public void testIPv4Address() throws Exception {
    final Address address = Address.from("127.0.0.1:5000");
    assertThat(address.host()).isEqualTo("127.0.0.1");
    assertThat(address.port()).isEqualTo(5000);
    assertThat(address.tryResolveAddress().getHostName()).isEqualTo("localhost");
    assertThat(address.toString()).isEqualTo("127.0.0.1:5000");
  }

  @Test
  public void testIPv6Address() throws Exception {
    final Address address = Address.from("[fe80:cd00:0000:0cde:1257:0000:211e:729c]:5000");
    assertThat(address.host()).isEqualTo("fe80:cd00:0000:0cde:1257:0000:211e:729c");
    assertThat(address.port()).isEqualTo(5000);
    assertThat(address.tryResolveAddress().getHostName())
        .isEqualTo("fe80:cd00:0:cde:1257:0:211e:729c");
    assertThat(address.toString()).isEqualTo("[fe80:cd00:0000:0cde:1257:0000:211e:729c]:5000");
  }

  @Test
  public void testResolveAddress() throws Exception {
    final Address address = Address.from("localhost", 5000);
    assertThat(address.tryResolveAddress().getHostAddress())
        .isEqualTo(InetAddress.getLoopbackAddress().getHostAddress());
    assertThat(address.port()).isEqualTo(5000);
  }
}
