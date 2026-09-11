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

package com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_METHOD;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_TRANSACTION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.TransactionMethod;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Transaction;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

/**
 * @author Thorben Lindhauer
 */
public class TransactionImpl extends SubProcessImpl implements Transaction {

  protected static Attribute<TransactionMethod> methodAttribute;

  public TransactionImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Transaction.class, BPMN_ELEMENT_TRANSACTION)
            .namespaceUri(BPMN20_NS)
            .extendsType(SubProcess.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Transaction>() {
                  @Override
                  public Transaction newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new TransactionImpl(instanceContext);
                  }
                });

    methodAttribute =
        typeBuilder
            .namedEnumAttribute(BPMN_ATTRIBUTE_METHOD, TransactionMethod.class)
            .defaultValue(TransactionMethod.Compensate)
            .build();

    typeBuilder.build();
  }

  @Override
  public TransactionMethod getMethod() {
    return methodAttribute.getValue(this);
  }

  @Override
  public void setMethod(final TransactionMethod method) {
    methodAttribute.setValue(this, method);
  }
}
