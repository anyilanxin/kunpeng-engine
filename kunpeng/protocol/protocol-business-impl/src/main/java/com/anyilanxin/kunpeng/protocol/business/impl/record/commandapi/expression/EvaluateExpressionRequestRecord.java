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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.expression;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.VARIABLES;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapArray;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.expression.EvaluateExpressionRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 表达式评估请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class EvaluateExpressionRequestRecord
    extends UnifiedRecordValue<EvaluateExpressionRequestRecord>
    implements EvaluateExpressionRequestRecordValue {

  private final StringProperty expressionProp = new StringProperty(1, "EXPRESSION", "");
  private final DocumentProperty variablesProperty = new DocumentProperty(2, VARIABLES);
  private final StringProperty tenantIdProp =
      new StringProperty(3, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public EvaluateExpressionRequestRecord() {
    super(3);
    // formatting:off
    declareProperty(expressionProp)
        .declareProperty(variablesProperty)
        .declareProperty(tenantIdProp);
    // formatting:on
  }

  @Override
  public String getExpression() {
    return bufferAsString(expressionProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getExpressionBuffer() {
    return expressionProp.getValue();
  }

  public EvaluateExpressionRequestRecord setExpression(final String expression) {
    expressionProp.setValue(wrapString(expression));
    return this;
  }

  public EvaluateExpressionRequestRecord setExpression(final DirectBuffer expression) {
    expressionProp.setValue(expression);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public EvaluateExpressionRequestRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public EvaluateExpressionRequestRecord setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public EvaluateExpressionRequestRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public EvaluateExpressionRequestRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected EvaluateExpressionRequestRecord newRecord() {
    return new EvaluateExpressionRequestRecord();
  }
}
