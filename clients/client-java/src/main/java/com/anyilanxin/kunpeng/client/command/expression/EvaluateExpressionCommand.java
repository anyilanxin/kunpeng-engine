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
package com.anyilanxin.kunpeng.client.command.expression;

import com.anyilanxin.kunpeng.client.command.CommandWithVariables;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

/**
 * 表达式评估命令：在引擎侧解析并求值表达式，返回评估结果。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface EvaluateExpressionCommand
    extends FinalCommandStep<EvaluateExpressionCommandResponse>,
        CommandWithVariables<EvaluateExpressionCommand> {

  /**
   * 设置待评估的表达式。
   *
   * @param expression 表达式文本
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  EvaluateExpressionCommand expression(String expression);

  /**
   * 设置租户 id。
   *
   * @param tenantId the tenant id
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  EvaluateExpressionCommand tenantId(String tenantId);

  /**
   * 设置评估作用域 key（如元素实例 key），仅用于路由定位分区；为 null 时随机选择分区。
   *
   * @param scopeKey the scope key, nullable
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  EvaluateExpressionCommand scopeKey(Long scopeKey);
}
