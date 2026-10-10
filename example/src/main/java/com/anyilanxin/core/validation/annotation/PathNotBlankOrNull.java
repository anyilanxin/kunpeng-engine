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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.anyilanxin.core.validation.validator.PathNotBlankOrNullValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 针对PathVariable参数验证(此时如果不传会默认为undefined)
 *
 * @author zxh
 * @date 2019-06-18 10:43
 * @since 1.0.0
 */
@Target({
  ElementType.METHOD,
  ElementType.FIELD,
  ElementType.ANNOTATION_TYPE,
  ElementType.CONSTRUCTOR,
  ElementType.PARAMETER,
  ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PathNotBlankOrNull.List.class)
@Constraint(validatedBy = {PathNotBlankOrNullValidator.class})
public @interface PathNotBlankOrNull {
  // 默认错误消息
  String message() default "内容不能为空";

  // 分组
  Class<?>[] groups() default {};

  // 负载
  Class<? extends Payload>[] payload() default {};

  // 指定多个时使用
  @Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
  @Retention(RUNTIME)
  @Documented
  @interface List {
    PathNotBlankOrNull[] value();
  }
}
