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
package com.anyilanxin.kunpeng.bpm.parse.bpmn;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResult;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultFormatter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 校验结果格式化器：按「元素定位链 + 结果明细」逐条输出，超出上限时输出省略计数。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnValidationErrorFormatter implements ValidationResultFormatter {

  /** 省略结果的后缀模板 */
  public static final String OMITTED_COUNT_SUFFIX_TEMPLATE = "and %d more errors and/or warnings";

  /** 元素定位链的连接符 */
  private static final String ELEMENT_SEPARATOR = " > ";

  /** 格式化元素定位（自最近的具 id 祖先到出错元素的链路）。 */
  @Override
  public void formatElement(final StringWriter writer, final ModelElementInstance element) {
    writer.append("- Element: ").append(buildElementLocation(element)).append('\n');
  }

  /** 格式化单条校验结果。 */
  @Override
  public void formatResult(final StringWriter writer, final ValidationResult result) {
    writer
        .append("    - ")
        .append(result.getType().toString())
        .append(": ")
        .append(result.getMessage())
        .append('\n');
  }

  /** 格式化省略结果的计数后缀。 */
  @Override
  public void formatSuffixWithOmittedResultsCount(final StringWriter writer, final int count) {
    writer.append(OMITTED_COUNT_SUFFIX_TEMPLATE.formatted(count));
  }

  /** 省略结果计数后缀的长度（用于结果条数上限估算）。 */
  @Override
  public int getFormattedSuffixWithOmittedResultsSize(final int count) {
    return OMITTED_COUNT_SUFFIX_TEMPLATE.formatted(count).getBytes().length;
  }

  /** 构造元素定位标识：从最近的具 id 祖先到出错元素的链路，具 id 节点显示 id、其余显示元素类型名。 */
  private String buildElementLocation(final ModelElementInstance element) {
    final List<ModelElementInstance> chain = new ArrayList<>(4);
    ModelElementInstance current = element;
    while (current != null) {
      chain.addFirst(current);
      current =
          current instanceof BaseElement && ((BaseElement) current).getId() != null
              ? null
              : current.getParentElement();
    }
    final StringBuilder identifier = new StringBuilder(64);
    for (int i = 0; i < chain.size(); i++) {
      final ModelElementInstance chainElement = chain.get(i);
      if (chainElement instanceof final BaseElement baseElement && baseElement.getId() != null) {
        identifier.append(baseElement.getId());
      } else {
        identifier.append(chainElement.getElementType().getTypeName());
      }
      if (i < chain.size() - 1) {
        identifier.append(ELEMENT_SEPARATOR);
      }
    }
    return identifier.toString();
  }
}
