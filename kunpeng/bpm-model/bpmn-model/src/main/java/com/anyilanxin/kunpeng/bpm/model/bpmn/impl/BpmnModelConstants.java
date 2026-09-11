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

import com.anyilanxin.kunpeng.bpm.model.bpmn.util.VersionUtil;

/**
 * Constants used in the BPMN 2.0 Language (DI + Semantic)
 *
 * @author Daniel Meyer
 * @author Falko Menge
 */
public interface BpmnModelConstants {

  /** The XSI namespace */
  String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

  /** The BPMN 2.0 namespace */
  String BPMN20_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

  /** The BPMNDI namespace */
  String BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI";

  /** The DC namespace */
  String DC_NS = "http://www.omg.org/spec/DD/20100524/DC";

  /** The DI namespace */
  String DI_NS = "http://www.omg.org/spec/DD/20100524/DI";

  /** The location of the BPMN 2.0 XML schema. */
  String BPMN_20_SCHEMA_LOCATION = "BPMN20.xsd";

  /** Xml Schema is the default type language */
  String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";

  String XPATH_NS = "http://www.w3.org/1999/XPath";

  /** Kunpeng namespace */
  String KUNPENG_NS = "http://anyilanxin.com/schema/kunpeng/1.0";

  /** Kunpeng namespace */
  String MODELER_NS = "http://anyilanxin.com/schema/modeler/1.0";

  /** Kunpeng version */
  String KUNPENG_VERSION = VersionUtil.getVersion();

  /** BPMN Exporter */
  String BPMN_EXPORTER = "Kunpeng BPMN Model";

  /** BPMN Execution Platform */
  String BPMN_EXECUTION_PLATFORM = "Kunpeng Cloud";

  // elements ////////////////////////////////////////

  String BPMN_ELEMENT_BASE_ELEMENT = "baseElement";
  String BPMN_ELEMENT_DEFINITIONS = "definitions";
  String BPMN_ELEMENT_DOCUMENTATION = "documentation";
  String BPMN_ELEMENT_EXTENSION = "extension";
  String BPMN_ELEMENT_EXTENSION_ELEMENTS = "extensionElements";
  String BPMN_ELEMENT_IMPORT = "import";
  String BPMN_ELEMENT_RELATIONSHIP = "relationship";
  String BPMN_ELEMENT_SOURCE = "source";
  String BPMN_ELEMENT_TARGET = "target";
  String BPMN_ELEMENT_ROOT_ELEMENT = "rootElement";
  String BPMN_ELEMENT_AUDITING = "auditing";
  String BPMN_ELEMENT_MONITORING = "monitoring";
  String BPMN_ELEMENT_CATEGORY_VALUE = "categoryValue";
  String BPMN_ELEMENT_FLOW_ELEMENT = "flowElement";
  String BPMN_ELEMENT_FLOW_NODE = "flowNode";
  String BPMN_ELEMENT_CATEGORY_VALUE_REF = "categoryValueRef";
  String BPMN_ELEMENT_EXPRESSION = "expression";
  String BPMN_ELEMENT_CONDITION_EXPRESSION = "conditionExpression";
  String BPMN_ELEMENT_SEQUENCE_FLOW = "sequenceFlow";
  String BPMN_ELEMENT_INCOMING = "incoming";
  String BPMN_ELEMENT_OUTGOING = "outgoing";
  String BPMN_ELEMENT_DATA_STATE = "dataState";
  String BPMN_ELEMENT_ITEM_DEFINITION = "itemDefinition";
  String BPMN_ELEMENT_ERROR = "error";
  String BPMN_ELEMENT_IN_MESSAGE_REF = "inMessageRef";
  String BPMN_ELEMENT_OUT_MESSAGE_REF = "outMessageRef";
  String BPMN_ELEMENT_ERROR_REF = "errorRef";
  String BPMN_ELEMENT_OPERATION = "operation";
  String BPMN_ELEMENT_IMPLEMENTATION_REF = "implementationRef";
  String BPMN_ELEMENT_OPERATION_REF = "operationRef";
  String BPMN_ELEMENT_DATA_OUTPUT = "dataOutput";
  String BPMN_ELEMENT_FROM = "from";
  String BPMN_ELEMENT_TO = "to";
  String BPMN_ELEMENT_ASSIGNMENT = "assignment";
  String BPMN_ELEMENT_ITEM_AWARE_ELEMENT = "itemAwareElement";
  String BPMN_ELEMENT_DATA_OBJECT = "dataObject";
  String BPMN_ELEMENT_DATA_OBJECT_REFERENCE = "dataObjectReference";
  String BPMN_ELEMENT_DATA_STORE = "dataStore";
  String BPMN_ELEMENT_DATA_STORE_REFERENCE = "dataStoreReference";
  String BPMN_ELEMENT_DATA_INPUT = "dataInput";
  String BPMN_ELEMENT_FORMAL_EXPRESSION = "formalExpression";
  String BPMN_ELEMENT_DATA_ASSOCIATION = "dataAssociation";
  String BPMN_ELEMENT_SOURCE_REF = "sourceRef";
  String BPMN_ELEMENT_TARGET_REF = "targetRef";
  String BPMN_ELEMENT_TRANSFORMATION = "transformation";
  String BPMN_ELEMENT_DATA_INPUT_ASSOCIATION = "dataInputAssociation";
  String BPMN_ELEMENT_DATA_OUTPUT_ASSOCIATION = "dataOutputAssociation";
  String BPMN_ELEMENT_INPUT_SET = "inputSet";
  String BPMN_ELEMENT_OUTPUT_SET = "outputSet";
  String BPMN_ELEMENT_DATA_INPUT_REFS = "dataInputRefs";
  String BPMN_ELEMENT_OPTIONAL_INPUT_REFS = "optionalInputRefs";
  String BPMN_ELEMENT_WHILE_EXECUTING_INPUT_REFS = "whileExecutingInputRefs";
  String BPMN_ELEMENT_OUTPUT_SET_REFS = "outputSetRefs";
  String BPMN_ELEMENT_DATA_OUTPUT_REFS = "dataOutputRefs";
  String BPMN_ELEMENT_OPTIONAL_OUTPUT_REFS = "optionalOutputRefs";
  String BPMN_ELEMENT_WHILE_EXECUTING_OUTPUT_REFS = "whileExecutingOutputRefs";
  String BPMN_ELEMENT_INPUT_SET_REFS = "inputSetRefs";
  String BPMN_ELEMENT_CATCH_EVENT = "catchEvent";
  String BPMN_ELEMENT_THROW_EVENT = "throwEvent";
  String BPMN_ELEMENT_END_EVENT = "endEvent";
  String BPMN_ELEMENT_IO_SPECIFICATION = "ioSpecification";
  String BPMN_ELEMENT_LOOP_CHARACTERISTICS = "loopCharacteristics";
  String BPMN_ELEMENT_RESOURCE_PARAMETER = "resourceParameter";
  String BPMN_ELEMENT_RESOURCE = "resource";
  String BPMN_ELEMENT_RESOURCE_PARAMETER_BINDING = "resourceParameterBinding";
  String BPMN_ELEMENT_RESOURCE_ASSIGNMENT_EXPRESSION = "resourceAssignmentExpression";
  String BPMN_ELEMENT_RESOURCE_ROLE = "resourceRole";
  String BPMN_ELEMENT_RESOURCE_REF = "resourceRef";
  String BPMN_ELEMENT_PERFORMER = "performer";
  String BPMN_ELEMENT_HUMAN_PERFORMER = "humanPerformer";
  String BPMN_ELEMENT_POTENTIAL_OWNER = "potentialOwner";
  String BPMN_ELEMENT_ACTIVITY = "activity";
  String BPMN_ELEMENT_IO_BINDING = "ioBinding";
  String BPMN_ELEMENT_INTERFACE = "interface";
  String BPMN_ELEMENT_EVENT = "event";
  String BPMN_ELEMENT_MESSAGE = "message";
  String BPMN_ELEMENT_START_EVENT = "startEvent";
  String BPMN_ELEMENT_PROPERTY = "property";
  String BPMN_ELEMENT_EVENT_DEFINITION = "eventDefinition";
  String BPMN_ELEMENT_EVENT_DEFINITION_REF = "eventDefinitionRef";
  String BPMN_ELEMENT_MESSAGE_EVENT_DEFINITION = "messageEventDefinition";
  String BPMN_ELEMENT_CANCEL_EVENT_DEFINITION = "cancelEventDefinition";
  String BPMN_ELEMENT_COMPENSATE_EVENT_DEFINITION = "compensateEventDefinition";
  String BPMN_ELEMENT_CONDITIONAL_EVENT_DEFINITION = "conditionalEventDefinition";
  String BPMN_ELEMENT_CONDITION = "condition";
  String BPMN_ELEMENT_ERROR_EVENT_DEFINITION = "errorEventDefinition";
  String BPMN_ELEMENT_LINK_EVENT_DEFINITION = "linkEventDefinition";
  String BPMN_ELEMENT_SIGNAL_EVENT_DEFINITION = "signalEventDefinition";
  String BPMN_ELEMENT_TERMINATE_EVENT_DEFINITION = "terminateEventDefinition";
  String BPMN_ELEMENT_TIMER_EVENT_DEFINITION = "timerEventDefinition";
  String BPMN_ELEMENT_SUPPORTED_INTERFACE_REF = "supportedInterfaceRef";
  String BPMN_ELEMENT_CALLABLE_ELEMENT = "callableElement";
  String BPMN_ELEMENT_PARTITION_ELEMENT = "partitionElement";
  String BPMN_ELEMENT_FLOW_NODE_REF = "flowNodeRef";
  String BPMN_ELEMENT_CHILD_LANE_SET = "childLaneSet";
  String BPMN_ELEMENT_LANE_SET = "laneSet";
  String BPMN_ELEMENT_LANE = "lane";
  String BPMN_ELEMENT_ARTIFACT = "artifact";
  String BPMN_ELEMENT_CORRELATION_PROPERTY_RETRIEVAL_EXPRESSION =
      "correlationPropertyRetrievalExpression";
  String BPMN_ELEMENT_MESSAGE_PATH = "messagePath";
  String BPMN_ELEMENT_DATA_PATH = "dataPath";
  String BPMN_ELEMENT_CALL_ACTIVITY = "callActivity";
  String BPMN_ELEMENT_CORRELATION_PROPERTY_BINDING = "correlationPropertyBinding";
  String BPMN_ELEMENT_CORRELATION_PROPERTY = "correlationProperty";
  String BPMN_ELEMENT_CORRELATION_PROPERTY_REF = "correlationPropertyRef";
  String BPMN_ELEMENT_CORRELATION_KEY = "correlationKey";
  String BPMN_ELEMENT_CORRELATION_SUBSCRIPTION = "correlationSubscription";
  String BPMN_ELEMENT_SUPPORTS = "supports";
  String BPMN_ELEMENT_PROCESS = "process";
  String BPMN_ELEMENT_TASK = "task";
  String BPMN_ELEMENT_SEND_TASK = "sendTask";
  String BPMN_ELEMENT_SERVICE_TASK = "serviceTask";
  String BPMN_ELEMENT_SCRIPT_TASK = "scriptTask";
  String BPMN_ELEMENT_USER_TASK = "userTask";
  String BPMN_ELEMENT_RECEIVE_TASK = "receiveTask";
  String BPMN_ELEMENT_BUSINESS_RULE_TASK = "businessRuleTask";
  String BPMN_ELEMENT_MANUAL_TASK = "manualTask";
  String BPMN_ELEMENT_SCRIPT = "script";
  String BPMN_ELEMENT_RENDERING = "rendering";
  String BPMN_ELEMENT_BOUNDARY_EVENT = "boundaryEvent";
  String BPMN_ELEMENT_SUB_PROCESS = "subProcess";
  String BPMN_ELEMENT_TRANSACTION = "transaction";
  String BPMN_ELEMENT_GATEWAY = "gateway";
  String BPMN_ELEMENT_PARALLEL_GATEWAY = "parallelGateway";
  String BPMN_ELEMENT_EXCLUSIVE_GATEWAY = "exclusiveGateway";
  String BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT = "intermediateCatchEvent";
  String BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT = "intermediateThrowEvent";
  String BPMN_ELEMENT_END_POINT = "endPoint";
  String BPMN_ELEMENT_PARTICIPANT_MULTIPLICITY = "participantMultiplicity";
  String BPMN_ELEMENT_PARTICIPANT = "participant";
  String BPMN_ELEMENT_PARTICIPANT_REF = "participantRef";
  String BPMN_ELEMENT_INTERFACE_REF = "interfaceRef";
  String BPMN_ELEMENT_END_POINT_REF = "endPointRef";
  String BPMN_ELEMENT_MESSAGE_FLOW = "messageFlow";
  String BPMN_ELEMENT_MESSAGE_FLOW_REF = "messageFlowRef";
  String BPMN_ELEMENT_CONVERSATION_NODE = "conversationNode";
  String BPMN_ELEMENT_CONVERSATION = "conversation";
  String BPMN_ELEMENT_SUB_CONVERSATION = "subConversation";
  String BPMN_ELEMENT_GLOBAL_CONVERSATION = "globalConversation";
  String BPMN_ELEMENT_CALL_CONVERSATION = "callConversation";
  String BPMN_ELEMENT_PARTICIPANT_ASSOCIATION = "participantAssociation";
  String BPMN_ELEMENT_INNER_PARTICIPANT_REF = "innerParticipantRef";
  String BPMN_ELEMENT_OUTER_PARTICIPANT_REF = "outerParticipantRef";
  String BPMN_ELEMENT_CONVERSATION_ASSOCIATION = "conversationAssociation";
  String BPMN_ELEMENT_MESSAGE_FLOW_ASSOCIATION = "messageFlowAssociation";
  String BPMN_ELEMENT_CONVERSATION_LINK = "conversationLink";
  String BPMN_ELEMENT_COLLABORATION = "collaboration";
  String BPMN_ELEMENT_ASSOCIATION = "association";
  String BPMN_ELEMENT_SIGNAL = "signal";
  String BPMN_ELEMENT_TIME_DATE = "timeDate";
  String BPMN_ELEMENT_TIME_DURATION = "timeDuration";
  String BPMN_ELEMENT_TIME_CYCLE = "timeCycle";
  String BPMN_ELEMENT_ESCALATION = "escalation";
  String BPMN_ELEMENT_ESCALATION_EVENT_DEFINITION = "escalationEventDefinition";
  String BPMN_ELEMENT_ACTIVATION_CONDITION = "activationCondition";
  String BPMN_ELEMENT_COMPLEX_GATEWAY = "complexGateway";
  String BPMN_ELEMENT_EVENT_BASED_GATEWAY = "eventBasedGateway";
  String BPMN_ELEMENT_INCLUSIVE_GATEWAY = "inclusiveGateway";
  String BPMN_ELEMENT_TEXT_ANNOTATION = "textAnnotation";
  String BPMN_ELEMENT_TEXT = "text";
  String BPMN_ELEMENT_COMPLEX_BEHAVIOR_DEFINITION = "complexBehaviorDefinition";
  String BPMN_ELEMENT_MULTI_INSTANCE_LOOP_CHARACTERISTICS = "multiInstanceLoopCharacteristics";
  String BPMN_ELEMENT_LOOP_CARDINALITY = "loopCardinality";
  String BPMN_ELEMENT_COMPLETION_CONDITION = "completionCondition";
  String BPMN_ELEMENT_OUTPUT_DATA_ITEM = "outputDataItem";
  String BPMN_ELEMENT_INPUT_DATA_ITEM = "inputDataItem";
  String BPMN_ELEMENT_LOOP_DATA_OUTPUT_REF = "loopDataOutputRef";
  String BPMN_ELEMENT_LOOP_DATA_INPUT_REF = "loopDataInputRef";
  String BPMN_ELEMENT_IS_SEQUENTIAL = "isSequential";
  String BPMN_ELEMENT_BEHAVIOR = "behavior";
  String BPMN_ELEMENT_ONE_BEHAVIOR_EVENT_REF = "oneBehaviorEventRef";
  String BPMN_ELEMENT_NONE_BEHAVIOR_EVENT_REF = "noneBehaviorEventRef";
  String BPMN_ELEMENT_GROUP = "group";
  String BPMN_ELEMENT_CATEGORY = "category";
  String BPMN_ELEMENT_AD_HOC_SUB_PROCESS = "adHocSubProcess";

  /** DC */
  String DC_ELEMENT_FONT = "Font";

  String DC_ELEMENT_POINT = "Point";
  String DC_ELEMENT_BOUNDS = "Bounds";

  /** DI */
  String DI_ELEMENT_DIAGRAM_ELEMENT = "DiagramElement";

  String DI_ELEMENT_DIAGRAM = "Diagram";
  String DI_ELEMENT_EDGE = "Edge";
  String DI_ELEMENT_EXTENSION = "extension";
  String DI_ELEMENT_LABELED_EDGE = "LabeledEdge";
  String DI_ELEMENT_LABEL = "Label";
  String DI_ELEMENT_LABELED_SHAPE = "LabeledShape";
  String DI_ELEMENT_NODE = "Node";
  String DI_ELEMENT_PLANE = "Plane";
  String DI_ELEMENT_SHAPE = "Shape";
  String DI_ELEMENT_STYLE = "Style";
  String DI_ELEMENT_WAYPOINT = "waypoint";

  /** BPMNDI */
  String BPMNDI_ELEMENT_BPMN_DIAGRAM = "BPMNDiagram";

  String BPMNDI_ELEMENT_BPMN_PLANE = "BPMNPlane";
  String BPMNDI_ELEMENT_BPMN_LABEL_STYLE = "BPMNLabelStyle";
  String BPMNDI_ELEMENT_BPMN_SHAPE = "BPMNShape";
  String BPMNDI_ELEMENT_BPMN_LABEL = "BPMNLabel";
  String BPMNDI_ELEMENT_BPMN_EDGE = "BPMNEdge";

  // attributes //////////////////////////////////////

  /** XSI attributes * */
  String XSI_ATTRIBUTE_TYPE = "type";

  /** BPMN attributes * */
  String BPMN_ATTRIBUTE_EXPORTER = "exporter";

  String BPMN_ATTRIBUTE_EXPORTER_VERSION = "exporterVersion";
  String BPMN_ATTRIBUTE_EXPRESSION_LANGUAGE = "expressionLanguage";
  String BPMN_ATTRIBUTE_ID = "id";
  String BPMN_ATTRIBUTE_NAME = "name";
  String BPMN_ATTRIBUTE_TARGET_NAMESPACE = "targetNamespace";
  String BPMN_ATTRIBUTE_TYPE_LANGUAGE = "typeLanguage";
  String BPMN_ATTRIBUTE_NAMESPACE = "namespace";
  String BPMN_ATTRIBUTE_LOCATION = "location";
  String BPMN_ATTRIBUTE_IMPORT_TYPE = "importType";
  String BPMN_ATTRIBUTE_TEXT_FORMAT = "textFormat";
  String BPMN_ATTRIBUTE_PROCESS_TYPE = "processType";
  String BPMN_ATTRIBUTE_IS_CLOSED = "isClosed";
  String BPMN_ATTRIBUTE_IS_EXECUTABLE = "isExecutable";
  String ATTRIBUTE_HISTORY_TIME_TO_LIVE = "historyTimeToLive";
  String BPMN_ATTRIBUTE_MESSAGE_REF = "messageRef";
  String BPMN_ATTRIBUTE_DEFINITION = "definition";
  String BPMN_ATTRIBUTE_MUST_UNDERSTAND = "mustUnderstand";
  String BPMN_ATTRIBUTE_TYPE = "type";
  String BPMN_ATTRIBUTE_DIRECTION = "direction";
  String BPMN_ATTRIBUTE_SOURCE_REF = "sourceRef";
  String BPMN_ATTRIBUTE_TARGET_REF = "targetRef";
  String BPMN_ATTRIBUTE_IS_IMMEDIATE = "isImmediate";
  String BPMN_ATTRIBUTE_VALUE = "value";
  String BPMN_ATTRIBUTE_STRUCTURE_REF = "structureRef";
  String BPMN_ATTRIBUTE_IS_COLLECTION = "isCollection";
  String BPMN_ATTRIBUTE_ITEM_KIND = "itemKind";
  String BPMN_ATTRIBUTE_ITEM_REF = "itemRef";
  String BPMN_ATTRIBUTE_ITEM_SUBJECT_REF = "itemSubjectRef";
  String BPMN_ATTRIBUTE_ERROR_CODE = "errorCode";
  String BPMN_ATTRIBUTE_LANGUAGE = "language";
  String BPMN_ATTRIBUTE_EVALUATES_TO_TYPE_REF = "evaluatesToTypeRef";
  String BPMN_ATTRIBUTE_PARALLEL_MULTIPLE = "parallelMultiple";
  String BPMN_ATTRIBUTE_IS_INTERRUPTING = "isInterrupting";
  String BPMN_ATTRIBUTE_IS_REQUIRED = "isRequired";
  String BPMN_ATTRIBUTE_PARAMETER_REF = "parameterRef";
  String BPMN_ATTRIBUTE_IS_FOR_COMPENSATION = "isForCompensation";
  String BPMN_ATTRIBUTE_START_QUANTITY = "startQuantity";
  String BPMN_ATTRIBUTE_COMPLETION_QUANTITY = "completionQuantity";
  String BPMN_ATTRIBUTE_DEFAULT = "default";
  String BPMN_ATTRIBUTE_OPERATION_REF = "operationRef";
  String BPMN_ATTRIBUTE_INPUT_DATA_REF = "inputDataRef";
  String BPMN_ATTRIBUTE_OUTPUT_DATA_REF = "outputDataRef";
  String BPMN_ATTRIBUTE_IMPLEMENTATION_REF = "implementationRef";
  String BPMN_ATTRIBUTE_PARTITION_ELEMENT_REF = "partitionElementRef";
  String BPMN_ATTRIBUTE_CORRELATION_PROPERTY_REF = "correlationPropertyRef";
  String BPMN_ATTRIBUTE_CORRELATION_KEY_REF = "correlationKeyRef";
  String BPMN_ATTRIBUTE_IMPLEMENTATION = "implementation";
  String BPMN_ATTRIBUTE_SCRIPT_FORMAT = "scriptFormat";
  String BPMN_ATTRIBUTE_INSTANTIATE = "instantiate";
  String BPMN_ATTRIBUTE_CANCEL_ACTIVITY = "cancelActivity";
  String BPMN_ATTRIBUTE_ATTACHED_TO_REF = "attachedToRef";
  String BPMN_ATTRIBUTE_TRIGGERED_BY_EVENT = "triggeredByEvent";
  String BPMN_ATTRIBUTE_GATEWAY_DIRECTION = "gatewayDirection";
  String BPMN_ATTRIBUTE_CALLED_ELEMENT = "calledElement";
  String BPMN_ATTRIBUTE_MINIMUM = "minimum";
  String BPMN_ATTRIBUTE_MAXIMUM = "maximum";
  String BPMN_ATTRIBUTE_PROCESS_REF = "processRef";
  String BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF = "calledCollaborationRef";
  String BPMN_ATTRIBUTE_INNER_CONVERSATION_NODE_REF = "innerConversationNodeRef";
  String BPMN_ATTRIBUTE_OUTER_CONVERSATION_NODE_REF = "outerConversationNodeRef";
  String BPMN_ATTRIBUTE_INNER_MESSAGE_FLOW_REF = "innerMessageFlowRef";
  String BPMN_ATTRIBUTE_OUTER_MESSAGE_FLOW_REF = "outerMessageFlowRef";
  String BPMN_ATTRIBUTE_ASSOCIATION_DIRECTION = "associationDirection";
  String BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION = "waitForCompletion";
  String BPMN_ATTRIBUTE_ACTIVITY_REF = "activityRef";
  String BPMN_ATTRIBUTE_ERROR_REF = "errorRef";
  String BPMN_ATTRIBUTE_SIGNAL_REF = "signalRef";
  String BPMN_ATTRIBUTE_ESCALATION_CODE = "escalationCode";
  String BPMN_ATTRIBUTE_ESCALATION_REF = "escalationRef";
  String BPMN_ATTRIBUTE_EVENT_GATEWAY_TYPE = "eventGatewayType";
  String BPMN_ATTRIBUTE_DATA_OBJECT_REF = "dataObjectRef";
  String BPMN_ATTRIBUTE_DATA_STORE_REF = "dataStoreRef";
  String BPMN_ATTRIBUTE_METHOD = "method";
  String BPMN_ATTRIBUTE_CAPACITY = "capacity";
  String BPMN_ATTRIBUTE_IS_UNLIMITED = "isUnlimited";
  String BPMN_ATTRIBUTE_CATEGORY_VALUE_REF = "categoryValueRef";
  String BPMN_ATTRIBUTE_CANCEL_REMAINING_INSTANCES = "cancelRemainingInstances";

  /** DC */
  String DC_ATTRIBUTE_NAME = "name";

  String DC_ATTRIBUTE_SIZE = "size";
  String DC_ATTRIBUTE_IS_BOLD = "isBold";
  String DC_ATTRIBUTE_IS_ITALIC = "isItalic";
  String DC_ATTRIBUTE_IS_UNDERLINE = "isUnderline";
  String DC_ATTRIBUTE_IS_STRIKE_THROUGH = "isStrikeThrough";
  String DC_ATTRIBUTE_X = "x";
  String DC_ATTRIBUTE_Y = "y";
  String DC_ATTRIBUTE_WIDTH = "width";
  String DC_ATTRIBUTE_HEIGHT = "height";

  /** DI */
  String DI_ATTRIBUTE_ID = "id";

  String DI_ATTRIBUTE_NAME = "name";
  String DI_ATTRIBUTE_DOCUMENTATION = "documentation";
  String DI_ATTRIBUTE_RESOLUTION = "resolution";

  /** BPMNDI */
  String BPMNDI_ATTRIBUTE_BPMN_ELEMENT = "bpmnElement";

  String BPMNDI_ATTRIBUTE_SOURCE_ELEMENT = "sourceElement";
  String BPMNDI_ATTRIBUTE_TARGET_ELEMENT = "targetElement";
  String BPMNDI_ATTRIBUTE_MESSAGE_VISIBLE_KIND = "messageVisibleKind";
  String BPMNDI_ATTRIBUTE_IS_HORIZONTAL = "isHorizontal";
  String BPMNDI_ATTRIBUTE_IS_EXPANDED = "isExpanded";
  String BPMNDI_ATTRIBUTE_IS_MARKER_VISIBLE = "isMarkerVisible";
  String BPMNDI_ATTRIBUTE_IS_MESSAGE_VISIBLE = "isMessageVisible";
  String BPMNDI_ATTRIBUTE_PARTICIPANT_BAND_KIND = "participantBandKind";
  String BPMNDI_ATTRIBUTE_CHOREOGRAPHY_ACTIVITY_SHAPE = "choreographyActivityShape";
  String BPMNDI_ATTRIBUTE_LABEL_STYLE = "labelStyle";
}
