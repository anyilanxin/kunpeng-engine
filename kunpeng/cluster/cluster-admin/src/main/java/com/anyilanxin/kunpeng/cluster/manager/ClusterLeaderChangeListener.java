package com.anyilanxin.kunpeng.cluster.manager;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;

/**
 * 集群 leader 变更监听器，在 leader 产生或丢失时回调对应成员的 MemberId。
 *
 * @author zxuanhong
 * @since
 */
public interface ClusterLeaderChangeListener {
  void foundLeader(MemberId memberId);

  void loseLeader(MemberId memberId);
}
