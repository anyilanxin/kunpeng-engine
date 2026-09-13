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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.clusterleader;

import static com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent.Type.METADATA_CHANGED;

import com.anyilanxin.kunpeng.cluster.business.ClusterRaftLoggers;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.MemberLeaderType;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.scheduler.Actor;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class DefaultClusterLeaderManageService extends Actor
    implements ClusterLeaderManageService, RaftRoleChangeListener {
  private volatile MemberId leader;
  public static final Logger LOGGER = ClusterRaftLoggers.CLUSTER_RAFT;
  private final Member localMember;
  private final ClusterLeaderFoundService leaderFoundService;

  public DefaultClusterLeaderManageService(
      final ClusterMembershipService membershipService,
      final ClusterLeaderFoundService leaderFoundService) {
    localMember = membershipService.getLocalMember();
    this.leaderFoundService = leaderFoundService;
  }

  @Override
  public void onNewRole(final RaftServer.Role newRole, final long newTerm) {
    actor.submit(
        () -> {
          if (newRole == RaftServer.Role.LEADER) {
            setLeader();
          } else {
            setNotLeader();
          }
          eventNotice();
        });
  }

  private void setLeader() {
    leader = localMember.id();
    LOGGER.info("Discover cluster leader node {}", localMember.id());
    localMember
        .properties()
        .setProperty(
            MemberLeaderType.MEMBER_LEADER_PROPERTY_KEY, MemberLeaderType.LEADER.getType());
  }

  private void setNotLeader() {
    localMember.properties().remove(MemberLeaderType.MEMBER_LEADER_PROPERTY_KEY);
  }

  private void eventNotice() {
    final ClusterMembershipEvent membershipEvent =
        new ClusterMembershipEvent(METADATA_CHANGED, localMember);
    leaderFoundService.event(membershipEvent);
  }

  @Override
  public void close() {
    setNotLeader();
  }
}
