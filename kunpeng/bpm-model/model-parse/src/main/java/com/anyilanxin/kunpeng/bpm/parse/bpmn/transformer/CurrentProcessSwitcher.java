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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 流程上下文切换转换器：在后续阶段遍历到流程元素时把该流程恢复为当前流程。
 *
 * <p>多流程模型中，阶段二起的遍历顺序可能离开当前流程，本转换器确保流程内元素转换时上下文指向正确的流程。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class CurrentProcessSwitcher implements ElementTransformer<Process> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<Process> getType() {
    return Process.class;
  }

  /**
   * 把遍历到的流程设为当前流程。
   *
   * @param element 流程模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Process element, final BpmnTransformContext context) {
    final BpmnProcess process = context.getProcess(element.getId());
    context.setCurrentProcess(process);
  }
}
