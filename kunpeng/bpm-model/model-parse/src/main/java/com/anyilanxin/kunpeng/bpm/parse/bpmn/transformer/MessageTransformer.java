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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengSubscription;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMessage;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.utils.Either;

/** 消息转换器：解析消息名与订阅关联键表达式；静态消息名在部署期提前求值为常量。 */
public final class MessageTransformer implements ElementTransformer<Message> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<Message> getType() {
    return Message.class;
  }

  /**
   * 创建消息运行时元素并注册到上下文（消息名缺失时不注册，由前置校验拦截）。
   *
   * @param element 消息模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Message element, final BpmnTransformContext context) {
    final String name = element.getName();
    if (name == null) {
      return;
    }
    final BpmnMessage message = new BpmnMessage(element.getId());
    final KunpengSubscription subscription =
        element.getSingleExtensionElement(KunpengSubscription.class);
    if (subscription != null && subscription.getCorrelationKey() != null) {
      message.setCorrelationKeyExpression(
          context.parseExpression(subscription.getCorrelationKey()));
    }
    message.setMessageNameExpression(context.parseExpression(name));
    if (message.getMessageNameExpression().isStatic()) {
      final Either<String, String> result = message.getMessageNameExpression().evaluateString();
      if (result.isRight()) {
        message.setMessageName(result.get());
      }
    }
    context.addMessage(message);
  }
}
