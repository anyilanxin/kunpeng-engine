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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.expression;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.BpmnFactory;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.expression.EvaluateExpressionRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.expression.EvaluateExpressionResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.expression.CommandApiExpressionValueLifeCycle;
import com.anyilanxin.kunpeng.utils.Either;

/**
 * 表达式评估 API 处理器：解析并求值表达式，评估结果以响应直接回写，不落状态存储。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ExpressionEvaluateApiProcessor
    implements LogEventProcessorSingleState<EvaluateExpressionRequestRecord> {
  private final LogEventWriter writer;
  private final ScriptEngine scriptEngine;

  public ExpressionEvaluateApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    scriptEngine = BpmnFactory.createExpressionLanguage(writer.getBeanFactory());
  }

  @Override
  public void processRecord(final BusinessLogRecord<EvaluateExpressionRequestRecord> record) {
    final EvaluateExpressionRequestRecord value = record.getValue();
    final ScriptExpression expression = scriptEngine.parse(value.getExpression());
    if (!expression.isValid()) {
      writer.adErrorResponse(
          record.getRequestId(), -1, "Expression parse failed: " + expression.getFailureMessage());
      return;
    }
    final Either<String, Object> result = expression.evaluateObject(() -> value.getVariables());
    if (result.isLeft()) {
      writer.adErrorResponse(
          record.getRequestId(), -1, "Expression evaluation failed: " + result.getLeft());
      return;
    }
    final EvaluateExpressionResponseRecord response = new EvaluateExpressionResponseRecord();
    response.setResult(result.get());
    writer.adResponse(
        CommandApiExpressionValueLifeCycle.EVALUATE_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public ValueType valueType() {
    return ValueType.EXPRESSION_API;
  }

  @Override
  public CommandApiExpressionValueLifeCycle valueLifeCycle() {
    return CommandApiExpressionValueLifeCycle.EVALUATE_REQUEST;
  }
}
