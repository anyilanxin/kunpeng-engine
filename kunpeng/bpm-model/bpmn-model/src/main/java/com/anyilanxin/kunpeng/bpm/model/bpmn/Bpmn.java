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

package com.anyilanxin.kunpeng.bpm.model.bpmn;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_EXECUTION_PLATFORM;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_EXPORTER;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.MODELER_NS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.ZEEBE_VERSION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ProcessBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnParser;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ActivationConditionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ActivityImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.AdHocSubProcessImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ArtifactImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.AssignmentImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.AssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.AuditingImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.BaseElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.BoundaryEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.BusinessRuleTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CallActivityImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CallConversationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CallableElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CancelEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CatchEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CategoryImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CategoryValueImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CategoryValueRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ChildLaneSet;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CollaborationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CompensateEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CompletionConditionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ComplexBehaviorDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ComplexGatewayImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConditionExpressionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConditionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConditionalEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConversationAssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConversationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConversationLinkImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ConversationNodeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CorrelationKeyImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CorrelationPropertyBindingImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CorrelationPropertyImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CorrelationPropertyRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CorrelationPropertyRetrievalExpressionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.CorrelationSubscriptionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataAssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataInputAssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataInputImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataInputRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataObjectImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataObjectReferenceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataOutputAssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataOutputImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataOutputRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataPath;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataStateImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataStoreImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DataStoreReferenceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DefinitionsImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.DocumentationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EndEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EndPointImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EndPointRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ErrorEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ErrorImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ErrorRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EscalationEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EscalationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EventBasedGatewayImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EventDefinitionRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.EventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ExclusiveGatewayImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ExpressionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ExtensionElementsImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ExtensionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.FlowElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.FlowNodeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.FlowNodeRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.FormalExpressionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.From;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.GatewayImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.GlobalConversationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.GroupImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.HumanPerformerImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ImportImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InMessageRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InclusiveGatewayImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.Incoming;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InnerParticipantRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InputDataItemImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InputSetImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InputSetRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InteractionNodeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InterfaceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.InterfaceRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.IntermediateCatchEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.IoBindingImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.IoSpecificationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ItemAwareElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ItemDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LaneImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LaneSetImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LinkEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LoopCardinalityImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LoopCharacteristicsImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LoopDataInputRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.LoopDataOutputRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ManualTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MessageEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MessageFlowAssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MessageFlowImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MessageFlowRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MessageImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MessagePath;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MonitoringImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.MultiInstanceLoopCharacteristicsImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OperationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OperationRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OptionalInputRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OptionalOutputRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OutMessageRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OuterParticipantRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.Outgoing;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OutputDataItemImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OutputSetImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.OutputSetRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ParallelGatewayImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ParticipantAssociationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ParticipantImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ParticipantMultiplicityImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ParticipantRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.PartitionElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.PerformerImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.PotentialOwnerImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ProcessImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.PropertyImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ReceiveTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.RelationshipImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.RenderingImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ResourceAssignmentExpressionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ResourceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ResourceParameterBindingImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ResourceParameterImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ResourceRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ResourceRoleImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.RootElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ScriptImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ScriptTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SendTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SequenceFlowImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SignalEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SignalImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.Source;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SourceRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.StartEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SubConversationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SubProcessImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.SupportedInterfaceRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.Supports;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.Target;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TargetRef;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TerminateEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TextAnnotationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TextImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.ThrowEventImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TimeCycleImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TimeDateImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TimeDurationImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TimerEventDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.To;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.TransactionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.Transformation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.UserTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.WhileExecutingInputRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.WhileExecutingOutputRefs;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi.BpmnDiagramImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi.BpmnEdgeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi.BpmnLabelImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi.BpmnLabelStyleImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi.BpmnPlaneImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.bpmndi.BpmnShapeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.dc.BoundsImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.dc.FontImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.dc.PointImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.DiagramElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.DiagramImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.EdgeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.LabelImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.LabeledEdgeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.LabeledShapeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.NodeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.PlaneImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.ShapeImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.StyleImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.WaypointImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeAdHocImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeAgentDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeAssignmentDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeCalledDecisionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeCalledElementImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeConditionalFilterImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeExecutionListenerImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeExecutionListenersImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeFormDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeHeaderImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeInputImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeIoMappingImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeJobPriorityDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeLinkedResourceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeLinkedResourcesImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeLoopCharacteristicsImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeOutputImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebePriorityDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebePropertiesImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebePropertyImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebePublishMessageImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeScriptImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeSubscriptionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeTaskDefinitionImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeTaskHeadersImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeTaskListenerImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeTaskListenersImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeTaskScheduleImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeUserTaskFormImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeUserTaskImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe.ZeebeVersionTagImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelParseException;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelValidationException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.IoUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides access to the camunda BPMN model api.
 *
 * @author Daniel Meyer
 */
public class Bpmn {

  public static final Bpmn INSTANCE = new BpmnImpl();

  /** the parser used by the Bpmn implementation. */
  private final BpmnParser bpmnParser = new BpmnParser();

  private final ModelBuilder bpmnModelBuilder;

  /** The {@link Model} */
  private Model bpmnModel;

  /** Register known types of the BPMN model */
  protected Bpmn() {
    bpmnModelBuilder = ModelBuilder.createInstance("BPMN Model");
    doRegisterTypes(bpmnModelBuilder);
    bpmnModel = bpmnModelBuilder.build();
  }

  /**
   * Allows reading a {@link BpmnModelInstance} from a File.
   *
   * @param file the {@link File} to read the {@link BpmnModelInstance} from
   * @return the model read
   * @throws BpmnModelException if the model cannot be read
   */
  public static BpmnModelInstance readModelFromFile(final File file) {
    return INSTANCE.doReadModelFromFile(file);
  }

  /**
   * Allows reading a {@link BpmnModelInstance} from an {@link InputStream}
   *
   * @param stream the {@link InputStream} to read the {@link BpmnModelInstance} from
   * @return the model read
   * @throws ModelParseException if the model cannot be read
   */
  public static BpmnModelInstance readModelFromStream(final InputStream stream) {
    return INSTANCE.doReadModelFromInputStream(stream);
  }

  /**
   * Allows writing a {@link BpmnModelInstance} to a File. It will be validated before writing.
   *
   * @param file the {@link File} to write the {@link BpmnModelInstance} to
   * @param modelInstance the {@link BpmnModelInstance} to write
   * @throws BpmnModelException if the model cannot be written
   * @throws ModelValidationException if the model is not valid
   */
  public static void writeModelToFile(final File file, final BpmnModelInstance modelInstance) {
    INSTANCE.doWriteModelToFile(file, modelInstance);
  }

  /**
   * Allows writing a {@link BpmnModelInstance} to an {@link OutputStream}. It will be validated
   * before writing.
   *
   * @param stream the {@link OutputStream} to write the {@link BpmnModelInstance} to
   * @param modelInstance the {@link BpmnModelInstance} to write
   * @throws ModelException if the model cannot be written
   * @throws ModelValidationException if the model is not valid
   */
  public static void writeModelToStream(
      final OutputStream stream, final BpmnModelInstance modelInstance) {
    INSTANCE.doWriteModelToOutputStream(stream, modelInstance);
  }

  /**
   * Allows the conversion of a {@link BpmnModelInstance} to an {@link String}. It will be validated
   * before conversion.
   *
   * @param modelInstance the model instance to convert
   * @return the XML string representation of the model instance
   */
  public static String convertToString(final BpmnModelInstance modelInstance) {
    return INSTANCE.doConvertToString(modelInstance);
  }

  /**
   * Validate model DOM document
   *
   * @param modelInstance the {@link BpmnModelInstance} to validate
   * @throws ModelValidationException if the model is not valid
   */
  public static void validateModel(final BpmnModelInstance modelInstance) {
    INSTANCE.doValidateModel(modelInstance);
  }

  /**
   * Allows creating an new, empty {@link BpmnModelInstance}.
   *
   * @return the empty model.
   */
  public static BpmnModelInstance createEmptyModel() {
    return INSTANCE.doCreateEmptyModel();
  }

  public static ProcessBuilder createProcess() {
    final BpmnModelInstance modelInstance = INSTANCE.doCreateEmptyModel();
    final Definitions definitions = createModelDefinition(modelInstance);
    modelInstance.setDefinitions(definitions);
    final Process process = modelInstance.newInstance(Process.class);
    definitions.addChildElement(process);

    final BpmnDiagram bpmnDiagram = modelInstance.newInstance(BpmnDiagram.class);

    final BpmnPlane bpmnPlane = modelInstance.newInstance(BpmnPlane.class);
    bpmnPlane.setBpmnElement(process);

    bpmnDiagram.addChildElement(bpmnPlane);
    definitions.addChildElement(bpmnDiagram);

    return process.builder();
  }

  public static ProcessBuilder createProcess(final String processId) {
    return createProcess().id(processId);
  }

  public static ProcessBuilder createExecutableProcess() {
    return createProcess().executable();
  }

  public static ProcessBuilder createExecutableProcess(final String processId) {
    return createProcess(processId).executable();
  }

  protected BpmnModelInstance doReadModelFromFile(final File file) {
    InputStream is = null;
    try {
      is = new FileInputStream(file);
      return doReadModelFromInputStream(is);

    } catch (final FileNotFoundException e) {
      throw new BpmnModelException(
          "Cannot read model from file " + file + ": file does not exist.");

    } finally {
      IoUtil.closeSilently(is);
    }
  }

  protected BpmnModelInstance doReadModelFromInputStream(final InputStream is) {
    return bpmnParser.parseModelFromStream(is);
  }

  protected void doWriteModelToFile(final File file, final BpmnModelInstance modelInstance) {
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
      doWriteModelToOutputStream(os, modelInstance);
    } catch (final FileNotFoundException e) {
      throw new BpmnModelException("Cannot write model to file " + file + ": file does not exist.");
    } finally {
      IoUtil.closeSilently(os);
    }
  }

  protected void doWriteModelToOutputStream(
      final OutputStream os, final BpmnModelInstance modelInstance) {
    // validate DOM document
    doValidateModel(modelInstance);
    // write XML
    IoUtil.writeDocumentToOutputStream(modelInstance.getDocument(), os);
  }

  protected String doConvertToString(final BpmnModelInstance modelInstance) {
    // validate DOM document
    doValidateModel(modelInstance);
    // convert to XML string
    return IoUtil.convertXmlDocumentToString(modelInstance.getDocument());
  }

  protected void doValidateModel(final BpmnModelInstance modelInstance) {
    bpmnParser.validateModel(modelInstance.getDocument());
  }

  protected BpmnModelInstance doCreateEmptyModel() {
    return bpmnParser.getEmptyModel();
  }

  protected void doRegisterTypes(final ModelBuilder bpmnModelBuilder) {
    ActivationConditionImpl.registerType(bpmnModelBuilder);
    ActivityImpl.registerType(bpmnModelBuilder);
    AdHocSubProcessImpl.registerType(bpmnModelBuilder);
    ArtifactImpl.registerType(bpmnModelBuilder);
    AssignmentImpl.registerType(bpmnModelBuilder);
    AssociationImpl.registerType(bpmnModelBuilder);
    AuditingImpl.registerType(bpmnModelBuilder);
    BaseElementImpl.registerType(bpmnModelBuilder);
    BoundaryEventImpl.registerType(bpmnModelBuilder);
    BusinessRuleTaskImpl.registerType(bpmnModelBuilder);
    CallableElementImpl.registerType(bpmnModelBuilder);
    CallActivityImpl.registerType(bpmnModelBuilder);
    CallConversationImpl.registerType(bpmnModelBuilder);
    CancelEventDefinitionImpl.registerType(bpmnModelBuilder);
    CatchEventImpl.registerType(bpmnModelBuilder);
    CategoryImpl.registerType(bpmnModelBuilder);
    CategoryValueImpl.registerType(bpmnModelBuilder);
    CategoryValueRef.registerType(bpmnModelBuilder);
    ChildLaneSet.registerType(bpmnModelBuilder);
    CollaborationImpl.registerType(bpmnModelBuilder);
    CompensateEventDefinitionImpl.registerType(bpmnModelBuilder);
    ConditionImpl.registerType(bpmnModelBuilder);
    ConditionalEventDefinitionImpl.registerType(bpmnModelBuilder);
    CompletionConditionImpl.registerType(bpmnModelBuilder);
    ComplexBehaviorDefinitionImpl.registerType(bpmnModelBuilder);
    ComplexGatewayImpl.registerType(bpmnModelBuilder);
    ConditionExpressionImpl.registerType(bpmnModelBuilder);
    ConversationAssociationImpl.registerType(bpmnModelBuilder);
    ConversationImpl.registerType(bpmnModelBuilder);
    ConversationLinkImpl.registerType(bpmnModelBuilder);
    ConversationNodeImpl.registerType(bpmnModelBuilder);
    CorrelationKeyImpl.registerType(bpmnModelBuilder);
    CorrelationPropertyBindingImpl.registerType(bpmnModelBuilder);
    CorrelationPropertyImpl.registerType(bpmnModelBuilder);
    CorrelationPropertyRef.registerType(bpmnModelBuilder);
    CorrelationPropertyRetrievalExpressionImpl.registerType(bpmnModelBuilder);
    CorrelationSubscriptionImpl.registerType(bpmnModelBuilder);
    DataAssociationImpl.registerType(bpmnModelBuilder);
    DataInputAssociationImpl.registerType(bpmnModelBuilder);
    DataInputImpl.registerType(bpmnModelBuilder);
    DataInputRefs.registerType(bpmnModelBuilder);
    DataOutputAssociationImpl.registerType(bpmnModelBuilder);
    DataOutputImpl.registerType(bpmnModelBuilder);
    DataOutputRefs.registerType(bpmnModelBuilder);
    DataPath.registerType(bpmnModelBuilder);
    DataStateImpl.registerType(bpmnModelBuilder);
    DataObjectImpl.registerType(bpmnModelBuilder);
    DataObjectReferenceImpl.registerType(bpmnModelBuilder);
    DataStoreImpl.registerType(bpmnModelBuilder);
    DataStoreReferenceImpl.registerType(bpmnModelBuilder);
    DefinitionsImpl.registerType(bpmnModelBuilder);
    DocumentationImpl.registerType(bpmnModelBuilder);
    EndEventImpl.registerType(bpmnModelBuilder);
    EndPointImpl.registerType(bpmnModelBuilder);
    EndPointRef.registerType(bpmnModelBuilder);
    ErrorEventDefinitionImpl.registerType(bpmnModelBuilder);
    ErrorImpl.registerType(bpmnModelBuilder);
    ErrorRef.registerType(bpmnModelBuilder);
    EscalationImpl.registerType(bpmnModelBuilder);
    EscalationEventDefinitionImpl.registerType(bpmnModelBuilder);
    EventBasedGatewayImpl.registerType(bpmnModelBuilder);
    EventDefinitionImpl.registerType(bpmnModelBuilder);
    EventDefinitionRef.registerType(bpmnModelBuilder);
    EventImpl.registerType(bpmnModelBuilder);
    ExclusiveGatewayImpl.registerType(bpmnModelBuilder);
    ExpressionImpl.registerType(bpmnModelBuilder);
    ExtensionElementsImpl.registerType(bpmnModelBuilder);
    ExtensionImpl.registerType(bpmnModelBuilder);
    FlowElementImpl.registerType(bpmnModelBuilder);
    FlowNodeImpl.registerType(bpmnModelBuilder);
    FlowNodeRef.registerType(bpmnModelBuilder);
    FormalExpressionImpl.registerType(bpmnModelBuilder);
    From.registerType(bpmnModelBuilder);
    GatewayImpl.registerType(bpmnModelBuilder);
    GlobalConversationImpl.registerType(bpmnModelBuilder);
    GroupImpl.registerType(bpmnModelBuilder);
    HumanPerformerImpl.registerType(bpmnModelBuilder);
    ImportImpl.registerType(bpmnModelBuilder);
    InclusiveGatewayImpl.registerType(bpmnModelBuilder);
    Incoming.registerType(bpmnModelBuilder);
    InMessageRef.registerType(bpmnModelBuilder);
    InnerParticipantRef.registerType(bpmnModelBuilder);
    InputDataItemImpl.registerType(bpmnModelBuilder);
    InputSetImpl.registerType(bpmnModelBuilder);
    InputSetRefs.registerType(bpmnModelBuilder);
    InteractionNodeImpl.registerType(bpmnModelBuilder);
    InterfaceImpl.registerType(bpmnModelBuilder);
    InterfaceRef.registerType(bpmnModelBuilder);
    IntermediateCatchEventImpl.registerType(bpmnModelBuilder);
    IntermediateThrowEventImpl.registerType(bpmnModelBuilder);
    IoBindingImpl.registerType(bpmnModelBuilder);
    IoSpecificationImpl.registerType(bpmnModelBuilder);
    ItemAwareElementImpl.registerType(bpmnModelBuilder);
    ItemDefinitionImpl.registerType(bpmnModelBuilder);
    LaneImpl.registerType(bpmnModelBuilder);
    LaneSetImpl.registerType(bpmnModelBuilder);
    LinkEventDefinitionImpl.registerType(bpmnModelBuilder);
    LoopCardinalityImpl.registerType(bpmnModelBuilder);
    LoopCharacteristicsImpl.registerType(bpmnModelBuilder);
    LoopDataInputRef.registerType(bpmnModelBuilder);
    LoopDataOutputRef.registerType(bpmnModelBuilder);
    ManualTaskImpl.registerType(bpmnModelBuilder);
    MessageEventDefinitionImpl.registerType(bpmnModelBuilder);
    MessageFlowAssociationImpl.registerType(bpmnModelBuilder);
    MessageFlowImpl.registerType(bpmnModelBuilder);
    MessageFlowRef.registerType(bpmnModelBuilder);
    MessageImpl.registerType(bpmnModelBuilder);
    MessagePath.registerType(bpmnModelBuilder);
    ModelElementInstanceImpl.registerType(bpmnModelBuilder);
    MonitoringImpl.registerType(bpmnModelBuilder);
    MultiInstanceLoopCharacteristicsImpl.registerType(bpmnModelBuilder);
    OperationImpl.registerType(bpmnModelBuilder);
    OperationRef.registerType(bpmnModelBuilder);
    OptionalInputRefs.registerType(bpmnModelBuilder);
    OptionalOutputRefs.registerType(bpmnModelBuilder);
    OuterParticipantRef.registerType(bpmnModelBuilder);
    OutMessageRef.registerType(bpmnModelBuilder);
    Outgoing.registerType(bpmnModelBuilder);
    OutputDataItemImpl.registerType(bpmnModelBuilder);
    OutputSetImpl.registerType(bpmnModelBuilder);
    OutputSetRefs.registerType(bpmnModelBuilder);
    ParallelGatewayImpl.registerType(bpmnModelBuilder);
    ParticipantAssociationImpl.registerType(bpmnModelBuilder);
    ParticipantImpl.registerType(bpmnModelBuilder);
    ParticipantMultiplicityImpl.registerType(bpmnModelBuilder);
    ParticipantRef.registerType(bpmnModelBuilder);
    PartitionElement.registerType(bpmnModelBuilder);
    PerformerImpl.registerType(bpmnModelBuilder);
    PotentialOwnerImpl.registerType(bpmnModelBuilder);
    ProcessImpl.registerType(bpmnModelBuilder);
    PropertyImpl.registerType(bpmnModelBuilder);
    ReceiveTaskImpl.registerType(bpmnModelBuilder);
    RelationshipImpl.registerType(bpmnModelBuilder);
    RenderingImpl.registerType(bpmnModelBuilder);
    ResourceAssignmentExpressionImpl.registerType(bpmnModelBuilder);
    ResourceImpl.registerType(bpmnModelBuilder);
    ResourceParameterBindingImpl.registerType(bpmnModelBuilder);
    ResourceParameterImpl.registerType(bpmnModelBuilder);
    ResourceRef.registerType(bpmnModelBuilder);
    ResourceRoleImpl.registerType(bpmnModelBuilder);
    RootElementImpl.registerType(bpmnModelBuilder);
    ScriptImpl.registerType(bpmnModelBuilder);
    ScriptTaskImpl.registerType(bpmnModelBuilder);
    SendTaskImpl.registerType(bpmnModelBuilder);
    SequenceFlowImpl.registerType(bpmnModelBuilder);
    ServiceTaskImpl.registerType(bpmnModelBuilder);
    SignalEventDefinitionImpl.registerType(bpmnModelBuilder);
    SignalImpl.registerType(bpmnModelBuilder);
    Source.registerType(bpmnModelBuilder);
    SourceRef.registerType(bpmnModelBuilder);
    StartEventImpl.registerType(bpmnModelBuilder);
    SubConversationImpl.registerType(bpmnModelBuilder);
    SubProcessImpl.registerType(bpmnModelBuilder);
    SupportedInterfaceRef.registerType(bpmnModelBuilder);
    Supports.registerType(bpmnModelBuilder);
    Target.registerType(bpmnModelBuilder);
    TargetRef.registerType(bpmnModelBuilder);
    TaskImpl.registerType(bpmnModelBuilder);
    TerminateEventDefinitionImpl.registerType(bpmnModelBuilder);
    TextImpl.registerType(bpmnModelBuilder);
    TextAnnotationImpl.registerType(bpmnModelBuilder);
    ThrowEventImpl.registerType(bpmnModelBuilder);
    TimeCycleImpl.registerType(bpmnModelBuilder);
    TimeDateImpl.registerType(bpmnModelBuilder);
    TimeDurationImpl.registerType(bpmnModelBuilder);
    TimerEventDefinitionImpl.registerType(bpmnModelBuilder);
    To.registerType(bpmnModelBuilder);
    TransactionImpl.registerType(bpmnModelBuilder);
    Transformation.registerType(bpmnModelBuilder);
    UserTaskImpl.registerType(bpmnModelBuilder);
    WhileExecutingInputRefs.registerType(bpmnModelBuilder);
    WhileExecutingOutputRefs.registerType(bpmnModelBuilder);

    /** DC */
    FontImpl.registerType(bpmnModelBuilder);
    PointImpl.registerType(bpmnModelBuilder);
    BoundsImpl.registerType(bpmnModelBuilder);

    /** DI */
    DiagramImpl.registerType(bpmnModelBuilder);
    DiagramElementImpl.registerType(bpmnModelBuilder);
    EdgeImpl.registerType(bpmnModelBuilder);
    com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.di.ExtensionImpl.registerType(
        bpmnModelBuilder);
    LabelImpl.registerType(bpmnModelBuilder);
    LabeledEdgeImpl.registerType(bpmnModelBuilder);
    LabeledShapeImpl.registerType(bpmnModelBuilder);
    NodeImpl.registerType(bpmnModelBuilder);
    PlaneImpl.registerType(bpmnModelBuilder);
    ShapeImpl.registerType(bpmnModelBuilder);
    StyleImpl.registerType(bpmnModelBuilder);
    WaypointImpl.registerType(bpmnModelBuilder);

    /** BPMNDI */
    BpmnDiagramImpl.registerType(bpmnModelBuilder);
    BpmnEdgeImpl.registerType(bpmnModelBuilder);
    BpmnLabelImpl.registerType(bpmnModelBuilder);
    BpmnLabelStyleImpl.registerType(bpmnModelBuilder);
    BpmnPlaneImpl.registerType(bpmnModelBuilder);
    BpmnShapeImpl.registerType(bpmnModelBuilder);

    /* Zeebe stuff */
    ZeebeHeaderImpl.registerType(bpmnModelBuilder);
    ZeebeInputImpl.registerType(bpmnModelBuilder);
    ZeebeIoMappingImpl.registerType(bpmnModelBuilder);
    ZeebeOutputImpl.registerType(bpmnModelBuilder);
    ZeebeSubscriptionImpl.registerType(bpmnModelBuilder);
    ZeebeTaskDefinitionImpl.registerType(bpmnModelBuilder);
    ZeebeTaskHeadersImpl.registerType(bpmnModelBuilder);
    ZeebeLoopCharacteristicsImpl.registerType(bpmnModelBuilder);
    ZeebeCalledElementImpl.registerType(bpmnModelBuilder);
    ZeebeFormDefinitionImpl.registerType(bpmnModelBuilder);
    ZeebeUserTaskFormImpl.registerType(bpmnModelBuilder);
    ZeebeAssignmentDefinitionImpl.registerType(bpmnModelBuilder);
    ZeebeTaskScheduleImpl.registerType(bpmnModelBuilder);
    ZeebeCalledDecisionImpl.registerType(bpmnModelBuilder);
    ZeebePropertyImpl.registerType(bpmnModelBuilder);
    ZeebePropertiesImpl.registerType(bpmnModelBuilder);
    ZeebeScriptImpl.registerType(bpmnModelBuilder);
    ZeebePublishMessageImpl.registerType(bpmnModelBuilder);
    ZeebeUserTaskImpl.registerType(bpmnModelBuilder);
    ZeebeExecutionListenersImpl.registerType(bpmnModelBuilder);
    ZeebeExecutionListenerImpl.registerType(bpmnModelBuilder);
    ZeebeTaskListenersImpl.registerType(bpmnModelBuilder);
    ZeebeTaskListenerImpl.registerType(bpmnModelBuilder);
    ZeebePriorityDefinitionImpl.registerType(bpmnModelBuilder);
    ZeebeJobPriorityDefinitionImpl.registerType(bpmnModelBuilder);
    ZeebeVersionTagImpl.registerType(bpmnModelBuilder);
    ZeebeAdHocImpl.registerType(bpmnModelBuilder);
    ZeebeLinkedResourceImpl.registerType(bpmnModelBuilder);
    ZeebeLinkedResourcesImpl.registerType(bpmnModelBuilder);
    ZeebeConditionalFilterImpl.registerType(bpmnModelBuilder);
    ZeebeAgentDefinitionImpl.registerType(bpmnModelBuilder);
  }

  /**
   * @return the {@link Model} instance to use
   */
  public Model getBpmnModel() {
    return bpmnModel;
  }

  /**
   * @param bpmnModel the bpmnModel to set
   */
  public void setBpmnModel(final Model bpmnModel) {
    this.bpmnModel = bpmnModel;
  }

  public ModelBuilder getBpmnModelBuilder() {
    return bpmnModelBuilder;
  }

  /** Create the default model definition */
  private static Definitions createModelDefinition(final BpmnModelInstance modelInstance) {
    final Definitions definitions = modelInstance.newInstance(Definitions.class);
    definitions.setExporter(BPMN_EXPORTER);
    definitions.setExporterVersion(ZEEBE_VERSION);
    definitions.setTargetNamespace(BPMN20_NS);
    definitions.setAttributeValueNs(
        MODELER_NS, "modeler:executionPlatform", BPMN_EXECUTION_PLATFORM);
    definitions.setAttributeValueNs(MODELER_NS, "modeler:executionPlatformVersion", ZEEBE_VERSION);
    return definitions;
  }
}
