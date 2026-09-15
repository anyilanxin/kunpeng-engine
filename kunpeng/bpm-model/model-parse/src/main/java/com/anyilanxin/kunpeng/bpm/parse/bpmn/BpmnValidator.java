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

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.traversal.ModelWalker;
import com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ValidationVisitor;
import com.anyilanxin.kunpeng.bpm.model.bpmn.validation.kunpeng.KunpengDesignTimeValidators;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.validation.ModelValidationResultsImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResults;
import java.io.StringWriter;

/**
 * BPMN 模型校验器：以设计期校验规则集合遍历模型，存在错误时格式化输出全部校验结果。
 *
 * <p>校验只读模型结构、不依赖表达式引擎；结果条数超出上限时仅输出计数后缀。
 */
public final class BpmnValidator {
  /** 设计期校验访问器（规则集合静态共享，访问器本身带重置能力可重复使用） */
  private final ValidationVisitor designTimeValidator;

  /** 校验结果格式化器 */
  private final BpmnValidationErrorFormatter formatter = new BpmnValidationErrorFormatter();

  /** 校验结果输出的最大条数 */
  private final int maxCollectedResults;

  /**
   * 构造校验器。
   *
   * @param maxCollectedResults 校验结果输出的最大条数
   */
  public BpmnValidator(final int maxCollectedResults) {
    this.designTimeValidator = new ValidationVisitor(KunpengDesignTimeValidators.VALIDATORS);
    this.maxCollectedResults = maxCollectedResults;
  }

  /**
   * 校验 BPMN 模型实例。
   *
   * @param modelInstance 待校验的 BPMN 模型实例
   * @return 存在校验错误时返回格式化的结果文本，否则返回 null
   */
  public String validate(final BpmnModelInstance modelInstance) {
    designTimeValidator.reset();
    new ModelWalker(modelInstance).walk(designTimeValidator);

    final ValidationResults results = designTimeValidator.getValidationResult();
    if (!results.hasErrors()) {
      return null;
    }
    final StringWriter writer = new StringWriter();
    new ModelValidationResultsImpl(results).write(writer, formatter, maxCollectedResults);
    return writer.toString();
  }
}
