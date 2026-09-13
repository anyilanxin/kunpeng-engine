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

import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import org.junit.jupiter.api.Test;

/** {@link PartitionHealth#of(RaftServer.Role)} 角色推导健康规则测试：参与选举与复制为 HEALTHY，其余为 DEAD。 */
class PartitionHealthTest {

  @Test
  void shouldDeriveHealthyFromActiveRoles() {
    assertThat(PartitionHealth.of(RaftServer.Role.LEADER)).isEqualTo(PartitionHealth.HEALTHY);
    assertThat(PartitionHealth.of(RaftServer.Role.FOLLOWER)).isEqualTo(PartitionHealth.HEALTHY);
    assertThat(PartitionHealth.of(RaftServer.Role.CANDIDATE)).isEqualTo(PartitionHealth.HEALTHY);
  }

  @Test
  void shouldDeriveUnhealthyFromNonActiveRoles() {
    assertThat(PartitionHealth.of(RaftServer.Role.INACTIVE)).isEqualTo(PartitionHealth.UNHEALTHY);
    assertThat(PartitionHealth.of(RaftServer.Role.PASSIVE)).isEqualTo(PartitionHealth.UNHEALTHY);
    assertThat(PartitionHealth.of(RaftServer.Role.PROMOTABLE)).isEqualTo(PartitionHealth.UNHEALTHY);
    assertThat(PartitionHealth.of((RaftServer.Role) null)).isEqualTo(PartitionHealth.DEAD);
  }

  @Test
  void shouldMapHealthReportStatus() {
    assertThat(PartitionHealth.of(HealthReport.Status.HEALTHY)).isEqualTo(PartitionHealth.HEALTHY);
    assertThat(PartitionHealth.of(HealthReport.Status.UNHEALTHY))
        .isEqualTo(PartitionHealth.UNHEALTHY);
    assertThat(PartitionHealth.of(HealthReport.Status.DEAD)).isEqualTo(PartitionHealth.DEAD);
    assertThat(PartitionHealth.of((HealthReport.Status) null)).isEqualTo(PartitionHealth.UNKNOWN);
  }

  @Test
  void shouldDefaultRoleAndHealthToUnknown() {
    final PartitionMemberInfo info = new PartitionMemberInfo();
    assertThat(info.getRole()).isEqualTo(PartitionRole.UNKNOWN);
    assertThat(info.getHealth()).isEqualTo(PartitionHealth.UNKNOWN);
  }
}
