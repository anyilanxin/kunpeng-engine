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
package com.anyilanxin.kunpeng.bpm.model.dmn.impl.instance;

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_CONTEXT;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Context;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.ContextEntry;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Expression;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

public class ContextImpl extends ExpressionImpl implements Context {

  protected static ChildElementCollection<ContextEntry> contextEntryCollection;

  public ContextImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public Collection<ContextEntry> getContextEntries() {
    return contextEntryCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Context.class, DMN_ELEMENT_CONTEXT)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(Expression.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Context>() {
                  public Context newInstance(ModelTypeInstanceContext instanceContext) {
                    return new ContextImpl(instanceContext);
                  }
                });

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    contextEntryCollection = sequenceBuilder.elementCollection(ContextEntry.class).build();

    typeBuilder.build();
  }
}
