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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.Managed;

/**
 * 可管理的分区拓扑服务：增加生命周期，并作为分区角色/故障监听器注入分区。
 *
 * <p>实例由 {@code RaftPartition} 构造时注册进 deferred 监听器集合，
 * 分区 server 创建后自动收到角色/故障回调。
 */
public interface ManagedPartitionTopologyService
    extends PartitionTopologyService, Managed<PartitionTopologyService>,
        RaftRoleChangeListener, FailureListener {}
