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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public final class ExtensionElementsValidator<T extends BaseElement, E extends ModelElementInstance>
    implements ModelElementValidator<T> {

  private final Class<T> elementType;
  private final Class<E> extensionElement;
  private final String extensionElementName;

  private ExtensionElementsValidator(
      final Class<T> elementType,
      final Class<E> extensionElement,
      final String extensionElementName) {
    this.elementType = elementType;
    this.extensionElement = extensionElement;
    this.extensionElementName = extensionElementName;
  }

  @Override
  public Class<T> getElementType() {
    return elementType;
  }

  @Override
  public void validate(final T element, final ValidationResultCollector validationResultCollector) {

    final ExtensionElements extensionElements = element.getExtensionElements();

    if (extensionElements == null
        || extensionElements.getChildElementsByType(extensionElement).size() != 1) {

      validationResultCollector.addError(
          0,
          String.format(
              "Must have exactly one 'kunpeng:%s' extension element", extensionElementName));
    }
  }

  public static <T extends BaseElement> Builder<T> verifyThat(final Class<T> elementType) {
    return new Builder<>(elementType);
  }

  public static class Builder<T extends BaseElement> {

    private final Class<T> elementType;

    public Builder(final Class<T> elementType) {
      this.elementType = elementType;
    }

    public <E extends ModelElementInstance>
        ExtensionElementsValidator<T, E> hasSingleExtensionElement(
            final Class<E> extensionElement, final String name) {

      return new ExtensionElementsValidator<>(elementType, extensionElement, name);
    }
  }
}
