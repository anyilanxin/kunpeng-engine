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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnBusinessKnowledgeLogic;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link DmnBusinessKnowledgeLogic} 的默认实现，封装业务知识逻辑的输出变量与业务知识函数。
 */
@Getter
@Setter
@ToString
public class DmnBusinessKnowledgeLogicImpl implements DmnBusinessKnowledgeLogic {
  /** 业务知识逻辑的输出变量 */
  protected DmnVariableImpl variable;
  /** 封装的业务知识函数 */
  protected DmnBusinessKnowledgeFunctionImpl knowledgeFunction;
}
