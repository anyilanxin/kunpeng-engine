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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_PARALLEL_GATEWAY;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ParallelGatewayBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Gateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ParallelGateway;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN parallelGateway element
 *
 * @author Sebastian Menski
 */
public class ParallelGatewayImpl extends GatewayImpl implements ParallelGateway {

  public ParallelGatewayImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ParallelGateway.class, BPMN_ELEMENT_PARALLEL_GATEWAY)
            .namespaceUri(BPMN20_NS)
            .extendsType(Gateway.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<ParallelGateway>() {
                  @Override
                  public ParallelGateway newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new ParallelGatewayImpl(instanceContext);
                  }
                });

    typeBuilder.build();
  }

  @Override
  public ParallelGatewayBuilder builder() {
    return new ParallelGatewayBuilder((BpmnModelInstance) modelInstance, this);
  }
}
