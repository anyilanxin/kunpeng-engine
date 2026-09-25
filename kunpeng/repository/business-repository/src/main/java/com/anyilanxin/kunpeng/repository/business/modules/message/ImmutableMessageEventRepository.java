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
package com.anyilanxin.kunpeng.repository.business.modules.message;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.repository.business.ResourceDataSplit;
import java.util.Optional;

/**
 * 消息域只读仓储接口：订阅与分发关联的查询。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ImmutableMessageEventRepository extends ResourceDataSplit {
  Optional<MessageSubscriptionRecord> query(long key);

  Optional<MessageDistributeCorrelateRecord> queryCorrelate(long key);

  MessageSubscriptionRecord correlationMessage(
      String messageName, String correlationKey, String tenantId);

  MessageSubscriptionRecord correlationStartMessage(String messageName, String tenantId);

  void visitorStartMessageByTenantAndProcessDefinitionKey(
      String tenantId, String processDefinitionKey, MessageVisitor visitor);

  void visitorActivityMessageByActivityInstanceId(long activityInstanceId, MessageVisitor visitor);

  @FunctionalInterface
  interface MessageVisitor {
    boolean visit(MessageSubscriptionRecord record);
  }
}
