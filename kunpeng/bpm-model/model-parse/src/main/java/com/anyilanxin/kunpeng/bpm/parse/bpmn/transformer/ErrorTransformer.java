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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Error;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnError;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 错误转换器：解析错误码表达式，静态错误码在部署期提前求值为常量。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ErrorTransformer implements ElementTransformer<Error> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<Error> getType() {
    return Error.class;
  }

  /**
   * 创建错误运行时元素并注册到上下文（未声明错误码时以空串兜底）。
   *
   * @param element 错误模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Error element, final BpmnTransformContext context) {
    final BpmnError error = new BpmnError(element.getId());
    final String errorCode = element.getErrorCode() == null ? "" : element.getErrorCode();
    error.setErrorCodeExpression(context.parseExpression(errorCode));
    if (error.getErrorCodeExpression().isStatic()) {
      error.setErrorCode(errorCode);
    }
    context.addError(error);
  }
}
