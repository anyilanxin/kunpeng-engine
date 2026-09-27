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

import com.anyilanxin.core.validation.annotation.NotInEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

/**
 * 判断是否是某个枚举值
 *
 * @author zxh
 * @date 2021-07-11 10:52
 * @since 1.0.0
 */
public class NotInEnumValidator implements ConstraintValidator<NotInEnum, Object> {
  private Class<? extends Enum<?>> enumClass;
  private String enumMethod;

  private boolean autoMessage;

  private String messageMethod;

  private String message;

  @Override
  public void initialize(final NotInEnum notInEnum) {
    enumMethod = notInEnum.enumMethod();
    enumClass = notInEnum.enumClass();
    autoMessage = notInEnum.autoMessage();
    messageMethod = notInEnum.messageMethod();
    message = notInEnum.message();
  }

  @Override
  public boolean isValid(
      final Object value, final ConstraintValidatorContext constraintValidatorContext) {
    ConstraintValidatorContextImpl context = null;
    if (constraintValidatorContext instanceof ConstraintValidatorContextImpl) {
      context = (ConstraintValidatorContextImpl) constraintValidatorContext;
    }
    if (Objects.isNull(value)) {
      return Boolean.TRUE;
    }
    if (Objects.isNull(enumClass) || Objects.isNull(enumMethod)) {
      return Boolean.TRUE;
    }
    final Class<?> valueClass = value.getClass();
    try {
      final Method method = enumClass.getMethod(enumMethod, valueClass);
      if (!Boolean.TYPE.equals(method.getReturnType())
          && !Boolean.class.equals(method.getReturnType())) {
        throw new RuntimeException(
            String.format("%s 方法返回值不是boolean类型 %s class", enumMethod, enumClass));
      }
      if (!Modifier.isStatic(method.getModifiers())) {
        throw new RuntimeException(
            String.format("%s 当前指定枚举校验方法不是静态方法 %s class", enumMethod, enumClass));
      }
      final Boolean result = (Boolean) method.invoke(null, value);
      // 动态解析消息
      if (Objects.nonNull(context) && autoMessage) {
        final Method messageMethod = enumClass.getMethod(this.messageMethod);
        final String message = (String) messageMethod.invoke(null);
        context.addMessageParameter("ANYILANXIN_VALIDATE_MESSAGE", this.message + message);
      }
      return result != null && result;
    } catch (final IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (final NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(
          String.format("This %s(%s) 方法不存在 %s", enumMethod, valueClass, enumClass), e);
    }
  }
}
