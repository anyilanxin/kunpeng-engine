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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;

/**
 * BPMN 模型元素转换器：负责一种模型元素类型到运行时元素的转换。
 *
 * @param <T> 本转换器处理的模型元素类型
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ElementTransformer<T extends BpmnModelElementInstance> {

  /** 返回本转换器处理的 BPMN 模型元素类型。 */
  Class<T> getType();

  /**
   * 转换指定的 BPMN 模型元素，并将转换产物注册到上下文或挂接到流程元素上。
   *
   * @param element 待转换的 BPMN 模型元素实例
   * @param context 转换上下文，用于读取已转换元素与注册新元素
   */
  void transform(T element, BpmnTransformContext context);
}
