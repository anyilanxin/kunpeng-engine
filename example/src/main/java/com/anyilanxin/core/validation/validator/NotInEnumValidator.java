/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
