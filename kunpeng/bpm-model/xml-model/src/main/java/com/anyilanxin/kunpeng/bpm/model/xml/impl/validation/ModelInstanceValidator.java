/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.bpm.model.xml.impl.validation;

import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResults;
import java.util.Collection;

/**
 * @author Daniel Meyer
 */
public class ModelInstanceValidator {

  protected ModelInstanceImpl modelInstanceImpl;
  protected Collection<ModelElementValidator<?>> validators;

  public ModelInstanceValidator(
      final ModelInstanceImpl modelInstanceImpl,
      final Collection<ModelElementValidator<?>> validators) {
    this.modelInstanceImpl = modelInstanceImpl;
    this.validators = validators;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public ValidationResults validate() {

    final ValidationResultsCollectorImpl resultsCollector = new ValidationResultsCollectorImpl();

    for (final ModelElementValidator validator : validators) {

      final Class<? extends ModelElementInstance> elementType = validator.getElementType();
      final Collection<? extends ModelElementInstance> modelElementsByType =
          modelInstanceImpl.getModelElementsByType(elementType);

      for (final ModelElementInstance element : modelElementsByType) {

        resultsCollector.setCurrentElement(element);

        try {
          validator.validate(element, resultsCollector);
        } catch (final RuntimeException e) {
          throw new RuntimeException(
              "Validator " + validator + " threw an exception while validating " + element, e);
        }
      }
    }

    return resultsCollector.getResults();
  }
}
