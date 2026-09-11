/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation.zeebe;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class ActivityValidator implements ModelElementValidator<Activity> {
  @Override
  public Class<Activity> getElementType() {
    return Activity.class;
  }

  @Override
  public void validate(
      final Activity element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    ModelUtil.verifyNoDuplicatedBoundaryEvents(
        element, error -> validationResultCollector.addError(0, error));
  }
}
