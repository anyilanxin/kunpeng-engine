# 06 — 典型示例

三种最常见 Record 的完整代码模板。

## 示例 1：纯标量 Record

参照 `DecisionRequirementDefinitionRecord.java`。

```java
package com.anyilanxin.kunpeng.protocol.impl.record.command.deployment;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.dispatch.TenantOwned;
import com.anyilanxin.kunpeng.protocol.dispatch.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.dispatch.record.command.deployment.DecisionRequirementDefinitionRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

@AutoDeclareProperties
public class DecisionRequirementDefinitionRecord
  extends UnifiedRecordValue<DecisionRequirementDefinitionRecord>
  implements DecisionRequirementDefinitionRecordValue {

  private final LongProperty decisionRequirementDefinitionIdProp =
    new LongProperty(1, "DECISION_REQUIREMENT_DEFINITION_ID", -1);
  private final StringProperty decisionRequirementDefinitionKeyProp =
    new StringProperty(2, "DECISION_REQUIREMENT_DEFINITION_KEY", "");
  private final StringProperty decisionRequirementDefinitionNameProp =
    new StringProperty(3, "DECISION_REQUIREMENT_DEFINITION_NAME", "");
  private final IntegerProperty decisionRequirementsVersionProp =
    new IntegerProperty(4, "DECISION_REQUIREMENTS_VERSION", 0);
  private final LongProperty deploymentIdProp = new LongProperty(5, DEPLOYMENT_ID, -1);
  private final LongProperty resourceIdProp = new LongProperty(6, RESOURCE_ID, -1);
  private final BinaryProperty checksumProp = new BinaryProperty(7, "CHECKSUM", new UnsafeBuffer());
  private final BinaryProperty resourceProp = new BinaryProperty(8, "RESOURCE", new UnsafeBuffer());
  private final StringProperty tenantIdProp =
    new StringProperty(9, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DecisionRequirementDefinitionRecord() {
    super(9);
    declareProperty(decisionRequirementDefinitionIdProp)
      .declareProperty(decisionRequirementDefinitionKeyProp)
      .declareProperty(decisionRequirementDefinitionNameProp)
      .declareProperty(decisionRequirementsVersionProp)
      .declareProperty(deploymentIdProp)
      .declareProperty(resourceIdProp)
      .declareProperty(checksumProp)
      .declareProperty(resourceProp)
      .declareProperty(tenantIdProp);
  }

  // 各 getter/setter（略，按 04 章节模板写）

  @Override
  protected DecisionRequirementDefinitionRecord newRecord() {
    return new DecisionRequirementDefinitionRecord();
  }
}
```

## 示例 2：含 ArrayProperty 集合 + xxx() 暴露（模式 B）

参照 `DeploymentRecord.java`。

```java
private final ArrayProperty<ResourceDefinitionRecord> resourceDefinitionsProp =
    new ArrayProperty<>(7, "RESOURCES_DEFINITIONS", ResourceDefinitionRecord::new);
private final ArrayProperty<ProcessDefinitionRecord> processDefinitionsProp =
    new ArrayProperty<>(8, "PROCESS_DEFINITIONS", ProcessDefinitionRecord::new);

public DeploymentRecord() {
  super(9);
  // ... 其他 declareProperty
  declareProperty(resourceDefinitionsProp)
      .declareProperty(processDefinitionsProp)
      // ...
}

/** 资源列表(原始数组访问,引擎处理器使用) */
public ArrayProperty<ResourceDefinitionRecord> resourceDefinitions() {
  return resourceDefinitionsProp;
}

@Override
public List<ResourceDefinitionValue> getResourceDefinitions() {
  final List<ResourceDefinitionValue> list = new ArrayList<>(resourceDefinitionsProp.size());
  for (final ResourceDefinitionRecord record : resourceDefinitionsProp) {
    list.add(record);
  }
  return list;
}
```

调用方（引擎处理器）：

```java
deploymentRecord.resourceDefinitions()
    .add()
    .setResourceId(...)
    .setResourceName(...)
    .setChecksum(...);
```

## 示例 3：Response Record + addXxx 封装（模式 C）

参照 `DeploymentCreateResponseRecord.java`。

```java
@AutoDeclareProperties
public class DeploymentCreateResponseRecord
    extends UnifiedRecordValue<DeploymentCreateResponseRecord>
    implements DeploymentCreateResponseRecordValue {

  private final ArrayProperty<DeploymentProcessDefinitionResponseRecord>
      processDefinitionsProp =
          new ArrayProperty<>(
              1, "PROCESS_DEFINITIONS", DeploymentProcessDefinitionResponseRecord::new);

  // ...

  /**
   * 追加一个流程定义(从命令侧 ProcessDefinitionRecord 复制字段)
   */
  public DeploymentCreateResponseRecord addProcessDefinition(
      final ProcessDefinitionRecord record) {
    final DirectBuffer checksumBuffer = record.getChecksumBuffer();
    processDefinitionsProp
        .add()
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionVersion(record.getProcessDefinitionVersion())
        .setSuspension(record.isSuspension())
        .setStartable(record.isStartable())
        .setHistoryTimeToLive(record.getHistoryTimeToLive())
        .setCandidateStarterGroups(record.getCandidateStarterGroups())
        .setCandidateStarterUsers(record.getCandidateStarterUsers())
        .setVersionTag(record.getVersionTagBuffer())
        .setResourceId(record.getResourceId())
        .setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
    return this;
  }

  @Override
  public List<DeploymentProcessDefinitionResponseRecordValue> getProcessDefinitions() {
    final List<DeploymentProcessDefinitionResponseRecordValue> list =
        new ArrayList<>(processDefinitionsProp.size());
    for (final DeploymentProcessDefinitionResponseRecord record : processDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  @Override
  protected DeploymentCreateResponseRecord newRecord() {
    return new DeploymentCreateResponseRecord();
  }
}
```

调用方（引擎处理器）：

```java
response.addProcessDefinition(processDefinitionRecord);   // 一行
```

## 示例 4：Set<String> 集合字段

```java
private final ArrayProperty<StringValue> candidateGroupsProp =
    new ArrayProperty<>(10, "CANDIDATE_STARTER_GROUPS", StringValue::new);

@Override
public Set<String> getCandidateStarterGroups() {
  final Set<String> set = new HashSet<>();
  if (candidateGroupsProp.hasValue()) {
    for (final StringValue v : candidateGroupsProp) {
      set.add(bufferAsString(v.getValue()));
    }
  }
  return set;
}

public XxxRecord setCandidateStarterGroups(Set<String> groups) {
  candidateGroupsProp.reset();
  if (groups == null) {
    return this;
  }
  for (final String g : groups) {
    candidateGroupsProp.add().wrap(wrapString(g));
  }
  return this;
}
```

## 验证

每次写完一个 Record，必须跑：

```bash
./gradlew :kunpeng:protocol:protocol-impl:compileJava --no-daemon
```

`BUILD SUCCESSFUL` 即可，immutables 相关 15 个 warning 忽略。
