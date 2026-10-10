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

import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

/**
 * 空或者null判断
 *
 * @author zxh
 * @date 2019-06-18 10:44
 * @since 1.0.0
 */
public class PathNotBlankOrNullValidator
    implements ConstraintValidator<PathNotBlankOrNull, Object> {
  private static final String PATH_NULL_VALUE = "undefined";

  @Override
  public void initialize(final PathNotBlankOrNull constraintAnnotation) {}

  @Override
  public boolean isValid(
      final Object value, final ConstraintValidatorContext constraintValidatorContext) {
    if (Objects.isNull(value)) {
      return false;
    }
    if (value instanceof String) {
      final String strValue = (String) value;
      return !PATH_NULL_VALUE.equalsIgnoreCase(strValue);
    }
    return true;
  }
}
