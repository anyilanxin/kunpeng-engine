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
package com.anyilanxin.kunpeng.protocol.common.encoding;

import com.anyilanxin.kunpeng.protocol.common.member.CommPortType;
import com.anyilanxin.kunpeng.protocol.common.member.PartitionHealth;
import com.anyilanxin.kunpeng.protocol.common.member.PartitionRole;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link BrokerInfo} 实体 SBE + Base64 属性载体往返与读-改-写端口帮助方法测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class BrokerInfoTest {

  @Test
  void shouldRoundTripPortsAndPartitions() {
    final BrokerInfo info = new BrokerInfo();
    info.addPort(CommPortType.BUSINESS, 2028);

    final BrokerInfo.PartitionEntry leader = new BrokerInfo.PartitionEntry();
    leader.partitionId = 1;
    leader.groupName = "business";
    leader.role = PartitionRole.LEADER;
    leader.health = PartitionHealth.HEALTHY;
    leader.term = 3L;
    leader.sourceId = 7;
    leader.agentSourceIds.add(11);
    leader.agentSourceIds.add(12);
    info.addPartition(leader);

    final BrokerInfo.PartitionEntry follower = new BrokerInfo.PartitionEntry();
    follower.partitionId = 2;
    follower.groupName = "business";
    follower.role = PartitionRole.FOLLOWER;
    follower.health = PartitionHealth.UNHEALTHY;
    follower.term = 5L;
    follower.sourceId = -1;
    info.addPartition(follower);

    final Properties properties = new Properties();
    info.writeIntoProperties(properties);
    assertNotNull(properties.getProperty(BrokerInfo.PROPERTY_NAME));

    final BrokerInfo decoded = BrokerInfo.fromProperties(properties);
    assertNotNull(decoded);
    assertEquals(2028, decoded.getPorts().get(CommPortType.BUSINESS));
    assertEquals(2, decoded.getPartitions().size());

    final BrokerInfo.PartitionEntry decodedLeader = decoded.getPartitions().get(0);
    assertEquals(1, decodedLeader.partitionId);
    assertEquals("business", decodedLeader.groupName);
    assertEquals(PartitionRole.LEADER, decodedLeader.role);
    assertEquals(PartitionHealth.HEALTHY, decodedLeader.health);
    assertEquals(3L, decodedLeader.term);
    assertEquals(7, decodedLeader.sourceId);
    assertEquals(2, decodedLeader.agentSourceIds.size());

    final BrokerInfo.PartitionEntry decodedFollower = decoded.getPartitions().get(1);
    assertEquals(PartitionRole.FOLLOWER, decodedFollower.role);
    assertEquals(PartitionHealth.UNHEALTHY, decodedFollower.health);
  }

  @Test
  void shouldReturnNullForMissingOrInvalidProperty() {
    assertNull(BrokerInfo.fromProperties(new Properties()));

    final Properties invalid = new Properties();
    invalid.setProperty(BrokerInfo.PROPERTY_NAME, Base64.getEncoder().encodeToString("junk".getBytes()));
    assertNull(BrokerInfo.fromProperties(invalid));
  }
}
