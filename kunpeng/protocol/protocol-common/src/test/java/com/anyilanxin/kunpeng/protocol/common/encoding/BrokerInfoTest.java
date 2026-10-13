/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
