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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateCatchEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 中间捕获事件转换器：事件载荷已由 {@link CatchEventTransformer}（CatchEvent 层级）装配，本阶段预留以承接后续按上下文调整的逻辑。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class IntermediateCatchEventTransformer
    implements ElementTransformer<IntermediateCatchEvent> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<IntermediateCatchEvent> getType() {
    return IntermediateCatchEvent.class;
  }

  /**
   * 当前为空实现：运行时元素在阶段二已完整装配。
   *
   * @param element 中间捕获事件模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final IntermediateCatchEvent element, final BpmnTransformContext context) {
    // 事件载荷装配见 CatchEventTransformer，此处保留阶段三扩展点
  }
}
