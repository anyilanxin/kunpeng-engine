/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.script;

import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author zxuanhong
 * @date 2025-10-10 23:52
 * @since
 */
public interface EvaluationResult {
  /**
   * @return the (raw) expression as string
   */
  String getExpression();

  /**
   * @return {@code true} if the evaluation was not successful
   */
  boolean isFailure();

  /**
   * Returns the reason why the evaluation failed. Use {@link #isFailure()} to check if the
   * evaluation failed or not.
   *
   * @return the failure message if the evaluation failed, otherwise {@code null}
   */
  String getFailureMessage();

  Object getObject();

  String getString();

  Boolean getBoolean();

  Number getNumber();

  Duration getDuration();

  Period getPeriod();

  ZonedDateTime getDateTime();

  List<String> getListOfStrings();

  Map<String, Object> getMapOfObject();
}
