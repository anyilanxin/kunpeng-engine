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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.concurrent.CompletableFuture;

/**
 * 管理 Raft 客户端服务的默认实现，基于集群消息服务向目标成员发送管理分区的扩容、缩容请求。
 *
 * @author zxuanhong
 * @since
 */
public class DefaultAdminRaftClientService extends Actor implements AdminRaftClientService {
  private final MessagingService messagingService;

  public DefaultAdminRaftClientService(final MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  @Override
  public CompletableFuture<Void> adminScaleUp(final MemberId memberId, final int expectScaleUpNum) {
    return null;
  }

  @Override
  public CompletableFuture<Void> adminScaleDown(
      final MemberId memberId, final int expectScaleDownNum) {
    return null;
  }
}
