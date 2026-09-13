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
package com.anyilanxin.kunpeng.cluster.config;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionHealth;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionRole;
import com.anyilanxin.kunpeng.cluster.config.topology.SwimPartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespaces;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import com.anyilanxin.kunpeng.cluster.utils.serializer.serializers.Int2ObjectHashMapSerializer;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.agrona.collections.Int2ObjectHashMap;

/**
 * 集群管理模块的序列化器，基于 Namespace 注册基础类型与集群管理相关类型（成员、分区、配置等）供元数据编解码使用。
 *
 * @author zxuanhong
 * @since
 */
public class ClusterAdminSerializer {
  public static final Serializer SERIALIZER =
      Serializer.using(
          new Namespace.Builder()
              .register(Namespaces.BASIC)
              // JDK 不可变集合未注册时 encode 抛异常且会被 actor 回调吞掉（静默丢数据），按形态逐一注册
              .register(Set.of(1).getClass())
              .register(Set.of(1, 2, 3).getClass())
              .register(List.of(1).getClass())
              .register(List.of(1, 2, 3).getClass())
              .register(Map.of("a", 1).getClass())
              .register(Map.of("a", 1, "b", 2).getClass())
              .register(MemberId.class)
              .register(LocalDateTime.class)
              .register(PartitionId.class)
              .register(LocalDate.class)
              .register(LocalTime.class)
              .register(MemberId.class)
              .register(PartitionMetadata.class)
              .register(ClusterConfiguration.class)
              .register(ClusterAdminConfiguration.class)
              .register(ClusterRaftConfiguration.class)
              .register(ClusterNodeConfiguration.class)
              .register(PartitionMemberInfo.class)
              .register(PartitionHealth.class)
              .register(PartitionRole.class)
              .register(SwimPartitionMemberInfo.class)
              .register(PartitionExecutionType.class)
              .register(PartitionInfoMetadata.class)
              .register(PartitionType.class)
              .register(Int2ObjectHashMapSerializer.class, Int2ObjectHashMap.class)
              .register(DispatchMeta.class)
              .register(Void.class)
              .name("ClusterAdmin")
              .build());

  private ClusterAdminSerializer() {}
}
