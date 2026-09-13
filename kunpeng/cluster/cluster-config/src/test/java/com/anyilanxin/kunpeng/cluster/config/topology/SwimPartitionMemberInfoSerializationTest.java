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
package com.anyilanxin.kunpeng.cluster.config.topology;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer;
import com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** {@link SwimPartitionMemberInfo} 经 Kryo + Base64 的 member property 广播编解码往返测试。 */
class SwimPartitionMemberInfoSerializationTest {

  @Test
  void shouldRoundTripThroughKryoAndBase64() {
    final SwimPartitionMemberInfo broadcast = new SwimPartitionMemberInfo();
    broadcast.add(info(1, PartitionRole.LEADER, PartitionHealth.HEALTHY, 3L));
    final PartitionMemberInfo withSource =
        info(2, PartitionRole.FOLLOWER, PartitionHealth.UNHEALTHY, 7L);
    withSource.setSourceId(2);
    withSource.setAgentSourceIds(Set.of(11, 12));
    broadcast.add(withSource);

    final byte[] bytes = ClusterAdminSerializer.SERIALIZER.encode(broadcast);
    final String propertyValue =
        Base64.getEncoder().encodeToString(bytes);
    final SwimPartitionMemberInfo decoded =
        ClusterAdminSerializer.SERIALIZER.decode(
            Base64.getDecoder().decode(propertyValue.getBytes(StandardCharsets.UTF_8)));

    final PartitionMemberInfo p1 = decoded.getInfoMap().get(PartitionId.from("raft-partition", 1));
    assertThat(p1).isNotNull();
    assertThat(p1.getMemberId()).isEqualTo("member-0");
    assertThat(p1.getRole()).isEqualTo(PartitionRole.LEADER);
    assertThat(p1.getHealth()).isEqualTo(PartitionHealth.HEALTHY);
    assertThat(p1.getTerm()).isEqualTo(3L);

    final PartitionMemberInfo p2 = decoded.getInfoMap().get(PartitionId.from("raft-partition", 2));
    assertThat(p2).isNotNull();
    assertThat(p2.getRole()).isEqualTo(PartitionRole.FOLLOWER);
    assertThat(p2.getHealth()).isEqualTo(PartitionHealth.UNHEALTHY);
    assertThat(p2.getTerm()).isEqualTo(7L);
    assertThat(p2.getSourceId()).isEqualTo(2);
    assertThat(p2.getAgentSourceIds()).containsExactlyInAnyOrder(11, 12);

    assertThat(decoded.getInfos()).hasSize(2);
  }

  private PartitionMemberInfo info(
      final int partition, final PartitionRole role, final PartitionHealth health, final long term) {
    final PartitionMemberInfo info = new PartitionMemberInfo();
    info.setMemberId("member-0");
    info.setPartitionId(PartitionId.from("raft-partition", partition));
    info.setRole(role);
    info.setHealth(health);
    info.setTerm(term);
    return info;
  }

  @Test
  void shouldExposePropertyKeyConstant() {
    assertThat(ClusterCommonConstant.TOPOLOGY_PROPERTY_KEY).isNotBlank();
  }
}
