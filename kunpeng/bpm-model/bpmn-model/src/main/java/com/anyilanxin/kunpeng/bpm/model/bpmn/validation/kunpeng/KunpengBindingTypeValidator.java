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
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KunpengBindingTypeValidator<T extends ModelElementInstance>
    implements ModelElementValidator<T> {

  private static final List<String> ALLOWED_BINDING_TYPES =
      Arrays.stream(KunpengBindingType.values())
          .map(KunpengBindingType::toString)
          .collect(Collectors.toList());

  private final Class<T> elementType;

  public KunpengBindingTypeValidator(final Class<T> elementType) {
    this.elementType = elementType;
  }

  @Override
  public Class<T> getElementType() {
    return elementType;
  }

  @Override
  public void validate(final T element, final ValidationResultCollector validationResultCollector) {
    final String bindingType = element.getAttributeValue(KunpengConstants.ATTRIBUTE_BINDING_TYPE);
    final String versionTag = element.getAttributeValue(KunpengConstants.ATTRIBUTE_VERSION_TAG);
    checkValidBindingTypeValue(validationResultCollector, bindingType);
    checkValidBindingTypeAndVersionTag(validationResultCollector, bindingType, versionTag);
  }

  private static void checkValidBindingTypeValue(
      final ValidationResultCollector validationResultCollector, final String bindingType) {
    if (bindingType != null && !ALLOWED_BINDING_TYPES.contains(bindingType)) {
      final String message =
          String.format(
              "Attribute '%s' must be one of: %s",
              KunpengConstants.ATTRIBUTE_BINDING_TYPE, String.join(", ", ALLOWED_BINDING_TYPES));
      validationResultCollector.addError(0, message);
    }
  }

  private static void checkValidBindingTypeAndVersionTag(
      final ValidationResultCollector validationResultCollector,
      final String bindingType,
      final String versionTag) {
    if (KunpengBindingType.versionTag.name().equals(bindingType) && isBlank(versionTag)) {
      validationResultCollector.addError(
          0,
          String.format(
              "Attribute '%s' must be present and not empty if '%s' is '%s'",
              KunpengConstants.ATTRIBUTE_VERSION_TAG,
              KunpengConstants.ATTRIBUTE_BINDING_TYPE,
              KunpengBindingType.versionTag));
    } else if (!KunpengBindingType.versionTag.name().equals(bindingType) && !isBlank(versionTag)) {
      validationResultCollector.addError(
          0,
          String.format(
              "Attribute '%s' may only be used if '%s' is '%s'",
              KunpengConstants.ATTRIBUTE_VERSION_TAG,
              KunpengConstants.ATTRIBUTE_BINDING_TYPE,
              KunpengBindingType.versionTag));
    }
  }

  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }
}
