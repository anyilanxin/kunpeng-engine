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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;

/** Event which represents the evaluation of a decision with a literal expression. */
public interface DmnDecisionLiteralExpressionEvaluationEvent
    extends DmnDecisionLogicEvaluationEvent {

  /**
   * @return the output name of the evaluated expression
   */
  String getOutputName();

  /**
   * @return the value of the evaluated expression
   */
  TypedValue getOutputValue();
}
