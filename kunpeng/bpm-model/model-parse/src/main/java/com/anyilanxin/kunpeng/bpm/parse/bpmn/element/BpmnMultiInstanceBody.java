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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.element;

/**
 * 多实例活动体的运行时模型：包裹内部活动并承载循环特征，与内部活动共用同一元素 id。
 *
 * <p>运行期流程执行到多实例活动时以本对象为节点（边界事件、顺序流均挂接到本对象），每个实例再执行内部活动。
 */
public final class BpmnMultiInstanceBody extends BpmnActivity {
  /** 循环特征 */
  private final BpmnLoopCharacteristics loopCharacteristics;

  /** 被包裹的内部活动 */
  private final BpmnActivity innerActivity;

  /**
   * 构造多实例活动体。
   *
   * @param id 元素 id（与内部活动一致）
   * @param loopCharacteristics 循环特征
   * @param innerActivity 内部活动
   */
  public BpmnMultiInstanceBody(
      final String id,
      final BpmnLoopCharacteristics loopCharacteristics,
      final BpmnActivity innerActivity) {
    super(id);
    this.loopCharacteristics = loopCharacteristics;
    this.innerActivity = innerActivity;
    setElementType(BpmnElementType.MULTI_INSTANCE_BODY);
  }

  /** 获取循环特征。 */
  public BpmnLoopCharacteristics getLoopCharacteristics() {
    return loopCharacteristics;
  }

  /** 获取内部活动。 */
  public BpmnActivity getInnerActivity() {
    return innerActivity;
  }
}
