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
package com.anyilanxin.core.validation.annotation;

/**
 * 枚举校验
 *
 * @author zxh
 * @date 2021-07-11 10:51
 * @since 1.0.0
 */
import com.anyilanxin.core.validation.validator.NotInEnumValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotInEnumValidator.class)
public @interface NotInEnum {
  /** 消息 */
  String message() default "当前类型错误";

  /** 是否自动消息，如果是从枚举中提取 */
  boolean autoMessage() default false;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** 校验的枚举 */
  Class<? extends Enum<?>> enumClass();

  /** 枚举调用获取方法 */
  String enumMethod() default "isHaveByType";

  /** 自动消息获取方法 */
  String messageMethod() default "getAllType";
}
