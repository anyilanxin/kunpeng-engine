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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapArray;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToObject;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.expression.EvaluateExpressionResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 表达式评估结果 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class EvaluateExpressionResponseRecord
    extends UnifiedRecordValue<EvaluateExpressionResponseRecord>
    implements EvaluateExpressionResponseRecordValue {

  private final DocumentProperty resultProperty = new DocumentProperty(1, "RESULT");

  public EvaluateExpressionResponseRecord() {
    super(1);
    declareProperty(resultProperty);
  }

  @Override
  public Object getResult() {
    return convertToObject(resultProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResultBuffer() {
    return resultProperty.getValue();
  }

  public EvaluateExpressionResponseRecord setResult(final Object result) {
    resultProperty.setValue(wrapArray(convertToMsgPack(result)));
    return this;
  }

  public EvaluateExpressionResponseRecord setResult(final DirectBuffer result) {
    resultProperty.setValue(result);
    return this;
  }

  @Override
  protected EvaluateExpressionResponseRecord newRecord() {
    return new EvaluateExpressionResponseRecord();
  }
}
