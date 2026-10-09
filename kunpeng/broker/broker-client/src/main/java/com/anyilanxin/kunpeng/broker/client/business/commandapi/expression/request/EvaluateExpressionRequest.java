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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.business.commandapi.expression.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiBrokerRequest;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.expression.EvaluateExpressionRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.expression.CommandApiExpressionValueLifeCycle;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 表达式评估请求。
 *
 * <p>路由规则：未指定 scopeKey 时随机分区；指定 scopeKey 后按 scopeKey 路由到对应分区。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class EvaluateExpressionRequest
    extends CommandApiBrokerRequest<EvaluateExpressionRequestRecord> {

  public EvaluateExpressionRequest() {
    super(
        ValueType.EXPRESSION_API,
        CommandApiExpressionValueLifeCycle.EVALUATE_REQUEST,
        new EvaluateExpressionRequestRecord());
    setPartitionId(RANDOM_PARTITION);
  }

  public EvaluateExpressionRequest setExpression(final String expression) {
    getValue().setExpression(expression);
    return this;
  }

  public EvaluateExpressionRequest setVariables(final Map<String, Object> variables) {
    getValue().setVariables(variables);
    return this;
  }

  public EvaluateExpressionRequest setVariables(final DirectBuffer variables) {
    getValue().setVariables(variables);
    return this;
  }

  public EvaluateExpressionRequest setTenantId(final String tenantId) {
    getValue().setTenantId(tenantId);
    return this;
  }

  /**
   * 设置评估作用域 key（如元素实例 key），仅用于路由定位分区；为 null 时保持随机分区。
   *
   * @param scopeKey 作用域 key，可为 null
   */
  public EvaluateExpressionRequest setScopeKey(final Long scopeKey) {
    if (scopeKey != null) {
      setKey(scopeKey);
      setPartitionId(NOT_SPECIFIED_PARTITION);
    }
    return this;
  }
}
