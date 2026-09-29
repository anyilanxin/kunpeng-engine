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
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * {@link MemberNodeInfo} 端口注册表的编解码与属性读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class MemberNodeInfoTest {

  @Test
  void updateAndReadBackPort() {
    final Properties properties = new Properties();
    MemberNodeInfo.updatePort(properties, CommPortType.BUSINESS, 25288);
    assertEquals(25288, MemberNodeInfo.readPort(properties, CommPortType.BUSINESS));
  }

  @Test
  void missingPropertyYieldsNull() {
    final Properties properties = new Properties();
    assertNull(MemberNodeInfo.readPort(properties, CommPortType.BUSINESS));
  }

  @Test
  void unknownTypeCodeIsIgnoredOnDecode() {
    final Properties properties = new Properties();
    // 手工写入一个未登记的类型编码(模拟对端新版本节点广播的新端口类型), 解码须安全忽略
    properties.setProperty(
        MemberNodeInfo.PROPERTY_KEY,
        MemberNodeInfoTest.encodeRaw(99, 12345));
    assertNull(MemberNodeInfo.readPort(properties, CommPortType.BUSINESS));
    // 后续正常更新不受脏数据影响(整体覆写)
    MemberNodeInfo.updatePort(properties, CommPortType.BUSINESS, 1);
    assertEquals(1, MemberNodeInfo.readPort(properties, CommPortType.BUSINESS));
  }

  @Test
  void removePortDropsKeyWhenRegistryEmpty() {
    final Properties properties = new Properties();
    MemberNodeInfo.updatePort(properties, CommPortType.BUSINESS, 25288);
    MemberNodeInfo.removePort(properties, CommPortType.BUSINESS);
    assertNull(properties.getProperty(MemberNodeInfo.PROPERTY_KEY));
    assertNull(MemberNodeInfo.readPort(properties, CommPortType.BUSINESS));
  }

  private static String encodeRaw(final int rawTypeCode, final int port) {
    final Properties properties = new Properties();
    MemberNodeInfo.updatePort(properties, CommPortType.BUSINESS, port);
    // 借用合法编码产物, 把类型编码字节替换为未登记值(布局: header + 组头 + [type, port]x N)
    final byte[] bytes = java.util.Base64.getDecoder()
        .decode(properties.getProperty(MemberNodeInfo.PROPERTY_KEY));
    bytes[bytes.length - 3] = (byte) rawTypeCode; // 末组第一字段 portType(uint8), 其后 port(uint16)
    return java.util.Base64.getEncoder().encodeToString(bytes);
  }
}
