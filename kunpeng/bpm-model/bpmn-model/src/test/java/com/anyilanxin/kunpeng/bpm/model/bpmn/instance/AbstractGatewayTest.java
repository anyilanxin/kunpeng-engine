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

package com.anyilanxin.kunpeng.bpm.model.bpmn.instance;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.GatewayDirection;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ReflectUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Before;

import java.io.InputStream;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractGatewayTest<G extends Gateway> extends BpmnModelElementInstanceTest {

    protected G gateway;

    @Override
    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(Gateway.class, false);
    }

    @Override
    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return null;
    }

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return null;
    }

    @Before
    @SuppressWarnings("unchecked")
    public void getGateway() {
        final InputStream inputStream =
                ReflectUtil.getResourceAsStream("com/anyilanxin/kunpeng/bpm/model/bpmn/GatewaysTest.xml");
        final Collection<ModelElementInstance> elementInstances =
                Bpmn.readModelFromStream(inputStream).getModelElementsByType(modelElementType);
        assertThat(elementInstances).hasSize(1);
        gateway = (G) elementInstances.iterator().next();
        assertThat(gateway.getGatewayDirection()).isEqualTo(GatewayDirection.Mixed);
    }
}
