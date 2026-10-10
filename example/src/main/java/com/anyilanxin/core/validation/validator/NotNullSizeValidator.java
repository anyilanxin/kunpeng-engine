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

import com.anyilanxin.core.validation.annotation.NotNullSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import org.springframework.util.CollectionUtils;

/**
 * 空或者数量判断(用于list)
 *
 * @author zxh
 * @date 2019-06-18 10:44
 * @since 1.0.0
 */
public class NotNullSizeValidator implements ConstraintValidator<NotNullSize, Collection<?>> {
  private int min;
  private int max;

  @Override
  public void initialize(final NotNullSize constraintAnnotation) {
    min = constraintAnnotation.min();
    max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(
      final Collection value, final ConstraintValidatorContext constraintValidatorContext) {
    if (CollectionUtils.isEmpty(value)) {
      return false;
    }
    final int size = value.size();
    return size >= min && size <= max;
  }
}
