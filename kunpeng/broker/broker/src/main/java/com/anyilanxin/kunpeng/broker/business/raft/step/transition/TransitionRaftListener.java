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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition;

import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;

/**
 * Raft 角色迁移监听器：分区 leader/follower 变化、分区移除与 source 元数据变化时回调。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface TransitionRaftListener {

  void onBecomingLeader(PartitionSourceMetadata sourceMetadata, EventLog logStream, long term);

  void onBecomingFollower(PartitionSourceMetadata sourceMetadata, long term);

  void onPartitionRemove(int partitionId);

  void onSourceMetadataChanged(PartitionSourceMetadata sourceMetadata);
}
