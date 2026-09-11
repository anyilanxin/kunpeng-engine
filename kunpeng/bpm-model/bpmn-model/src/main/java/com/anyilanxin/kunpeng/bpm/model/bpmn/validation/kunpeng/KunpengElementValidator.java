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

import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class KunpengElementValidator<T extends ModelElementInstance>
    implements ModelElementValidator<T> {

  private final Class<T> elementType;
  private final List<AttributeAssertion<T>> singleAttributeAssertions = new ArrayList<>();
  private final List<AttributeAssertion<T>> groupAttributesAssertions = new ArrayList<>();

  private KunpengElementValidator(final Class<T> elementType) {
    this.elementType = elementType;
  }

  @Override
  public Class<T> getElementType() {
    return elementType;
  }

  @Override
  public void validate(final T element, final ValidationResultCollector validationResultCollector) {

    singleAttributeAssertions.forEach(
        assertions -> {
          final String attributeValue = assertions.attributeSupplier.apply(element);
          if (attributeValue == null || attributeValue.isEmpty()) {
            validationResultCollector.addError(
                0,
                String.format(
                    "Attribute '%s' must be present and not empty", assertions.attributeName));
          }
        });

    if (!groupAttributesAssertions.isEmpty()) {
      final long matchCount = countMatchingGroupAttributesAssertions(element);

      if (matchCount != 1) {
        final String attributes = getAttributeNames();
        final String errorMessage =
            String.format(
                "Exactly one of the attributes '%s' must be present and not blank", attributes);
        validationResultCollector.addError(0, errorMessage);
      }
    }
  }

  public KunpengElementValidator<T> hasNonEmptyAttribute(
      final Function<T, String> attributeSupplier, final String attributeName) {
    singleAttributeAssertions.add(new AttributeAssertion<>(attributeSupplier, attributeName));
    return this;
  }

  public KunpengElementValidator<T> hasNonEmptyEnumAttribute(
      final Function<T, ? extends Enum<?>> enumAttributeSupplier, final String attributeName) {
    return hasNonEmptyAttribute(
        enumAttributeSupplier.andThen(e -> e == null ? null : e.name()), attributeName);
  }

  public KunpengElementValidator<T> hasOnlyOneAttributeInGroup(
      final Map<String, Function<T, String>> nameToAttributeSupplier) {
    nameToAttributeSupplier.forEach(
        (attributeName, attributeSupplier) ->
            groupAttributesAssertions.add(
                new AttributeAssertion<>(attributeSupplier, attributeName)));
    return this;
  }

  public static <T extends ModelElementInstance> KunpengElementValidator<T> verifyThat(
      final Class<T> elementType) {
    return new KunpengElementValidator<>(elementType);
  }

  private long countMatchingGroupAttributesAssertions(final T element) {
    return groupAttributesAssertions.stream()
        .filter(
            assertion -> {
              final String attributeValue = assertion.attributeSupplier.apply(element);
              return attributeValue != null && !attributeValue.trim().isEmpty();
            })
        .count();
  }

  private String getAttributeNames() {
    return groupAttributesAssertions.stream()
        .map(assertion -> assertion.attributeName)
        .collect(Collectors.joining(", "));
  }

  private static final class AttributeAssertion<T extends ModelElementInstance> {
    private final Function<T, String> attributeSupplier;
    private final String attributeName;

    private AttributeAssertion(
        final Function<T, String> attributeSupplier, final String attributeName) {
      this.attributeSupplier = attributeSupplier;
      this.attributeName = attributeName;
    }
  }
}
