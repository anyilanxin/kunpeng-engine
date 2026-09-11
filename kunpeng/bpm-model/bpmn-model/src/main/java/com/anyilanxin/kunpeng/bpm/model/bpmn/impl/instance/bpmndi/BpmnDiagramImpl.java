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

package com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_ELEMENT_BPMN_DIAGRAM;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_NS;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.DiagramImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnLabelStyle;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.di.Diagram;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

/**
 * The BPMNDI BPMNDiagram element
 *
 * @author Sebastian Menski
 */
public class BpmnDiagramImpl extends DiagramImpl implements BpmnDiagram {

  protected static ChildElement<BpmnPlane> bpmnPlaneChild;
  protected static ChildElementCollection<BpmnLabelStyle> bpmnLabelStyleCollection;

  public BpmnDiagramImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(BpmnDiagram.class, BPMNDI_ELEMENT_BPMN_DIAGRAM)
            .namespaceUri(BPMNDI_NS)
            .extendsType(Diagram.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<BpmnDiagram>() {
                  @Override
                  public BpmnDiagram newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new BpmnDiagramImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    bpmnPlaneChild = sequenceBuilder.element(BpmnPlane.class).required().build();

    bpmnLabelStyleCollection = sequenceBuilder.elementCollection(BpmnLabelStyle.class).build();

    typeBuilder.build();
  }

  @Override
  public BpmnPlane getBpmnPlane() {
    return bpmnPlaneChild.getChild(this);
  }

  @Override
  public void setBpmnPlane(final BpmnPlane bpmnPlane) {
    bpmnPlaneChild.setChild(this, bpmnPlane);
  }

  @Override
  public Collection<BpmnLabelStyle> getBpmnLabelStyles() {
    return bpmnLabelStyleCollection.get(this);
  }
}
