/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
package io.camunda.connector.runtime.core.testutil.command;

import com.anyilanxin.kunpeng.client.command.KunpengClientFutureImpl;
import com.anyilanxin.kunpeng.client.command.expression.EvaluateExpressionCommand;
import io.camunda.connector.runtime.core.testutil.response.EvaluateExpressionResponseDummy;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Records every {@link EvaluateExpressionCommand} call and answers {@link #send()} with a response
 * whose result is produced by the given resolver applied to the last recorded expression.
 */
public class EvaluateExpressionCommandDummy implements EvaluateExpressionCommand {

  private final Function<String, Object> resultResolver;
  private final List<String> expressions = new ArrayList<>();
  private final List<String> tenantIds = new ArrayList<>();
  private final List<Long> scopeKeys = new ArrayList<>();
  private final List<Map<String, Object>> variablesList = new ArrayList<>();

  public EvaluateExpressionCommandDummy(Function<String, Object> resultResolver) {
    this.resultResolver = resultResolver;
  }

  public List<String> expressions() {
    return expressions;
  }

  public List<String> tenantIds() {
    return tenantIds;
  }

  public List<Long> scopeKeys() {
    return scopeKeys;
  }

  public List<Map<String, Object>> variablesList() {
    return variablesList;
  }

  @Override
  public EvaluateExpressionCommand expression(String expression) {
    expressions.add(expression);
    return this;
  }

  @Override
  public EvaluateExpressionCommand tenantId(String tenantId) {
    tenantIds.add(tenantId);
    return this;
  }

  @Override
  public EvaluateExpressionCommand scopeKey(Long scopeKey) {
    scopeKeys.add(scopeKey);
    return this;
  }

  @Override
  public EvaluateExpressionCommand variables(String variables) {
    return this;
  }

  @Override
  public EvaluateExpressionCommand variables(Object variables) {
    return this;
  }

  @Override
  public EvaluateExpressionCommand variables(InputStream variables) {
    return this;
  }

  @Override
  public EvaluateExpressionCommand variables(Map<String, Object> variables) {
    variablesList.add(variables);
    return this;
  }

  @Override
  public EvaluateExpressionCommand variable(String key, Object value) {
    return this;
  }

  @Override
  public EvaluateExpressionCommand requestTimeout(Duration requestTimeout) {
    return this;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public KunpengClientFutureImpl send() {
    Object result = resultResolver.apply(expressions.getLast());
    KunpengClientFutureImpl future = new KunpengClientFutureImpl<>();
    future.complete(new EvaluateExpressionResponseDummy(result));
    return future;
  }
}
