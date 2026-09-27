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
package com.anyilanxin.core.validation.validator;

import com.anyilanxin.core.validation.annotation.NotBlankOrNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * 空或者null判断
 *
 * @author zxh
 * @date 2019-06-18 10:44
 * @since 1.0.0
 */
public class NotBlankOrNullValidator implements ConstraintValidator<NotBlankOrNull, Object> {
  @Override
  public void initialize(final NotBlankOrNull constraintAnnotation) {}

  @Override
  public boolean isValid(
      final Object value, final ConstraintValidatorContext constraintValidatorContext) {
    if (Objects.isNull(value)) {
      return false;
    }
    return !(value instanceof String) || !StringUtils.isBlank(value.toString());
  }
}
