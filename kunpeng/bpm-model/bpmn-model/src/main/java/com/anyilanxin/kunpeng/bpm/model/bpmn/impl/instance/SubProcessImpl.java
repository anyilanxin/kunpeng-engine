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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TRIGGERED_BY_EVENT;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_SUB_PROCESS;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.SubProcessBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Artifact;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.LaneSet;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import java.util.Collection;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN subProcess element
 *
 * @author Sebastian Menski
 */
public class SubProcessImpl extends ActivityImpl implements SubProcess {

  protected static Attribute<Boolean> triggeredByEventAttribute;
  protected static ChildElementCollection<LaneSet> laneSetCollection;
  protected static ChildElementCollection<FlowElement> flowElementCollection;
  protected static ChildElementCollection<Artifact> artifactCollection;

  public SubProcessImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(SubProcess.class, BPMN_ELEMENT_SUB_PROCESS)
            .namespaceUri(BPMN20_NS)
            .extendsType(Activity.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<SubProcess>() {
                  @Override
                  public SubProcess newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new SubProcessImpl(instanceContext);
                  }
                });

    triggeredByEventAttribute =
        typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_TRIGGERED_BY_EVENT).defaultValue(false).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    laneSetCollection = sequenceBuilder.elementCollection(LaneSet.class).build();

    flowElementCollection = sequenceBuilder.elementCollection(FlowElement.class).build();

    artifactCollection = sequenceBuilder.elementCollection(Artifact.class).build();

    typeBuilder.build();
  }

  @Override
  public SubProcessBuilder builder() {
    return new SubProcessBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public boolean triggeredByEvent() {
    return triggeredByEventAttribute.getValue(this);
  }

  @Override
  public void setTriggeredByEvent(final boolean triggeredByEvent) {
    triggeredByEventAttribute.setValue(this, triggeredByEvent);
  }

  @Override
  public Collection<LaneSet> getLaneSets() {
    return laneSetCollection.get(this);
  }

  @Override
  public Collection<FlowElement> getFlowElements() {
    return flowElementCollection.get(this);
  }

  @Override
  public Collection<Artifact> getArtifacts() {
    return artifactCollection.get(this);
  }
}
