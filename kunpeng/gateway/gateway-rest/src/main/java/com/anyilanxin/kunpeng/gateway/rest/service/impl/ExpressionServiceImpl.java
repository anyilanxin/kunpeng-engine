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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.service.impl;

import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitValue;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.ExpressionClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.expression.request.EvaluateExpressionRequest;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.EvaluateExpressionDto;
import com.anyilanxin.kunpeng.gateway.rest.service.ExpressionService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.EvaluateExpressionVo;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.expression.EvaluateExpressionResponseRecord;
import com.anyilanxin.kunpeng.structpack.util.DocumentUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 表达式求值业务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class ExpressionServiceImpl implements ExpressionService {
  private final ExpressionClient expression;

  public ExpressionServiceImpl(final BusinessCommandClient clients) {
    this.expression = clients.expression();
  }

  @Override
  public Mono<EvaluateExpressionVo> evaluate(final EvaluateExpressionDto dto) {
    final EvaluateExpressionRequest request =
        new EvaluateExpressionRequest()
            .setExpression(dto.expression())
            .setVariables(ensureJsonSet(dto.variables()))
            .setTenantId(dto.tenantId())
            .setScopeKey(dto.scopeKey());
    return awaitValue(expression.evaluateExpression(request), ExpressionServiceImpl::toVo);
  }

  private static EvaluateExpressionVo toVo(final EvaluateExpressionResponseRecord value) {
    return new EvaluateExpressionVo(DocumentUtil.convertToJson(value.getResultBuffer()));
  }
}
