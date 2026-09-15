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

package com.anyilanxin.kunpeng.bpm.model.bpmn.impl;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_20_SCHEMA_LOCATION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.parser.AbstractModelParser;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;

/**
 * The parser used when parsing BPMN Files
 *
 * @author Daniel Meyer
 */
public class BpmnParser extends AbstractModelParser {

  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  public BpmnParser() {
    schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA);
    addSchema(BPMN20_NS, createSchema(BPMN_20_SCHEMA_LOCATION, BpmnParser.class.getClassLoader()));
  }

  @Override
  protected void configureFactory(final DocumentBuilderFactory dbf) {
    // XSD 校验统一由基类的 validateModel（schema 构造期编译一次）执行；
    // 不在 factory 上挂 schema 源，否则每次解析都会重新装载编译 XSD（约 19 倍解析耗时）
    super.configureFactory(dbf);
  }

  @Override
  public BpmnModelInstanceImpl parseModelFromStream(final InputStream inputStream) {
    return (BpmnModelInstanceImpl) super.parseModelFromStream(inputStream);
  }

  @Override
  public BpmnModelInstanceImpl getEmptyModel() {
    return (BpmnModelInstanceImpl) super.getEmptyModel();
  }

  @Override
  protected BpmnModelInstanceImpl createModelInstance(final DomDocument document) {
    return new BpmnModelInstanceImpl(
        (ModelImpl) Bpmn.INSTANCE.getBpmnModel(), Bpmn.INSTANCE.getBpmnModelBuilder(), document);
  }
}
