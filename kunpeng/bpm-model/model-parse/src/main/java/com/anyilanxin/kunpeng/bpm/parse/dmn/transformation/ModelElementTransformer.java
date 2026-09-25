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
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformation;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DmnModelElementInstance;

/**
 * DMN 模型元素转换器接口：每种实现负责一种 DMN 元素（见 {@link #getType()}），由 {@link TransformationVisitor} 按元素类型分发调用，将
 * bpm-model 的 XML 模型实例转换为 dmn/element 包的运行时元素。
 *
 * @param <T> 本转换器处理的 DMN 模型元素类型
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ModelElementTransformer<T extends DmnModelElementInstance> {

  /** 返回本转换器处理的 DMN 模型元素类型。 */
  Class<T> getType();

  /**
   * 转换指定的 DMN 模型元素，并将转换产物注册到上下文或挂到决策需求图上。
   *
   * @param element 待转换的 DMN 模型元素实例
   * @param context 转换上下文，用于读取已转换元素与注册新元素
   */
  void transform(T element, TransformContext context);
}
