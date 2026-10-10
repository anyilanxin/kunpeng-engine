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

/**
 * The output for a evaluated decision.
 *
 * <p>In a decision table implementation an output can have a human readable name and a name which
 * can be used to reference the output value in the decision result.
 *
 * <p>The human readable name is the {@code label} attribute of the DMN XML {@code output} element.
 * You can access this name by the {@link #getName()} getter.
 *
 * <p>The output name to reference the output value in the decision result is the {@code name}
 * attribute of the DMN XML {@code output} element. You can access this output name by the {@link
 * #getOutputName()} getter.
 *
 * <p>The {@code id} and {@code value} of the evaluated decision table output entry can be access by
 * the {@link #getId()} and {@link #getValue()} getter.
 */
public interface DmnEvaluatedOutput {

  /**
   * @return the id of the evaluated output or null if not set
   */
  String getId();

  /**
   * @return the name of the evaluated output or null if not set
   */
  String getName();

  /**
   * @return the output name of the evaluated output or null if not set
   */
  String getOutputName();

  /**
   * @return the value of the evaluated output or null if non set
   */
  TypedValue getValue();
}
