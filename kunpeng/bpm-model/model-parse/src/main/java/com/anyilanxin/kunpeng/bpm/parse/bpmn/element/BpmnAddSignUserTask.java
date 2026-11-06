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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.bpm.parse.bpmn.element;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;

/**
 * 加签用户任务容器的运行时模型：每个加签组派生一个组活动实例挂本元素，加签任务为容器名下的工作项。
 *
 * <p>解析期由声明 {@code kunpeng:addSign} 的原用户任务合成（id + "-addsign"），不对应 XSD 元素、不参与流程边；
 * 元素/实例分离——同一原元素的多轮加签与多层级联都是本元素的不同实例。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnAddSignUserTask extends BpmnUserTask {

  /**
   * 以元素 id 构造加签容器（原定义 key + 后缀）。
   *
   * @param id 元素唯一标识
   */
  public BpmnAddSignUserTask(final String id) {
    super(id);
    setAddSignVirtual(true);
    setElementType(BpmnElementType.ADD_SIGN_USER_TASK);
  }
}
