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
