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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 业务知识函数（BusinessKnowledgeFunction）的内存模型，封装知识逻辑的实现表达式及其形式参数列表。 */
@Getter
@Setter
@ToString
public class DmnBusinessKnowledgeFunctionImpl implements DmnElement {
  private String key;

  /** 知识函数的实现表达式 */
  protected DmnExpressionImpl expression;

  /** 形式参数（FormalParameter）列表 */
  protected List<DmnFormalParameterImpl> parameters = new ArrayList<>();

  @Override
  public String getKey() {
    return key;
  }

  /** 元素类型，当前未定义对应的 {@link ElementType}，恒返回 null */
  @Override
  public ElementType getType() {
    return null;
  }
}
