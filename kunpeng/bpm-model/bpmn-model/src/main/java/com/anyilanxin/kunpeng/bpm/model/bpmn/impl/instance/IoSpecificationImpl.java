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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_IO_SPECIFICATION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataInput;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataOutput;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.InputSet;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IoSpecification;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.OutputSet;
import java.util.Collection;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN IoSpecification element
 *
 * @author Sebastian Menski
 */
public class IoSpecificationImpl extends BaseElementImpl implements IoSpecification {

  protected static ChildElementCollection<DataInput> dataInputCollection;
  protected static ChildElementCollection<DataOutput> dataOutputCollection;
  protected static ChildElementCollection<InputSet> inputSetCollection;
  protected static ChildElementCollection<OutputSet> outputSetCollection;

  public IoSpecificationImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(IoSpecification.class, BPMN_ELEMENT_IO_SPECIFICATION)
            .namespaceUri(BPMN20_NS)
            .extendsType(BaseElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<IoSpecification>() {
                  @Override
                  public IoSpecification newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new IoSpecificationImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataInputCollection = sequenceBuilder.elementCollection(DataInput.class).build();

    dataOutputCollection = sequenceBuilder.elementCollection(DataOutput.class).build();

    inputSetCollection = sequenceBuilder.elementCollection(InputSet.class).required().build();

    outputSetCollection = sequenceBuilder.elementCollection(OutputSet.class).required().build();

    typeBuilder.build();
  }

  @Override
  public Collection<DataInput> getDataInputs() {
    return dataInputCollection.get(this);
  }

  @Override
  public Collection<DataOutput> getDataOutputs() {
    return dataOutputCollection.get(this);
  }

  @Override
  public Collection<InputSet> getInputSets() {
    return inputSetCollection.get(this);
  }

  @Override
  public Collection<OutputSet> getOutputSets() {
    return outputSetCollection.get(this);
  }
}
