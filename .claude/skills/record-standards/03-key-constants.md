# 03 — Key 常量使用

## 核心原则

> **不要往 `RecordConstant.java` 里加新常量。**

这是项目里反复强调的红线。原因：

1. `RecordConstant` 是**协议级常量**，跨模块使用，改动影响面大
2. 每个 Record 自己的 key 大多是**局部使用**，没必要提到全局
3. 历史上加新常量经常导致用户回退修改

## 决策表

| 场景 | 做法 |
|------|------|
| 字段名是跨 Record 通用的（`TENANT_ID`、`DEPLOYMENT_ID`、`RESOURCE_ID`、`VERSION_TAG`、`PROCESS_DEFINITION_ID`、`START_TIME`、`END_TIME`、`STATE`、`LIFE_CYCLE` 等） | ✅ 用 `RecordConstant` 常量 |
| 字段名是这个 Record 特有的（`DECISION_DEFINITION_KEY`、`HISTORY_TIME_TO_LIVE`、`CANDIDATE_STARTER_GROUPS` 等） | ✅ 直接写字面量字符串 |
| 想新加一个常量 | ❌ 不要加，写字面量 |

## 现有常量清单（截至 2026-07）

位于 `kunpeng/protocol/protocol-impl/src/main/java/com/anyilanxin/kunpeng/protocol/impl/RecordConstant.java`：

```java
// 公共
String VERSION = "REV";
String LIFE_CYCLE = "LIFE_CYCLE";
String START_TIME = "START_TIME";
String END_TIME = "END_TIME";
String DURATION = "DURATION";
String TENANT_ID = "TENANT_ID";
String PRIORITY = "PRIORITY";
String STATE = "STATE";
String VARIABLES = "VARIABLES";
String LOCAL_VARIABLES = "LOCAL_VARIABLES";

// 流程定义
String PROCESS_DEFINITION_KEY = "PROCESS_DEFINITION_KEY";
String PROCESS_DEFINITION_VERSION = "PROCESS_DEFINITION_VERSION";
String PROCESS_DEFINITION_NAME = "PROCESS_DEFINITION_NAME";
String ACTIVITY_DEFINITION_KEY = "ACTIVITY_DEFINITION_KEY";
String ACTIVITY_DEFINITION_NAME = "ACTIVITY_DEFINITION_NAME";
String ACTIVITY_DEFINITION_TYPE = "ACTIVITY_DEFINITION_TYPE";
String PROCESS_DEFINITION_ID = "PROCESS_DEFINITION_ID";

// 流程实例
String ADDITIONS = "ADDITIONS";
String LISTENER_TYPE = "LISTENER_TYPE";
String LISTENER_INDEX = "LISTENER_INDEX";
String BUSINESS_KEY = "BUSINESS_KEY";
String PROCESS_INSTANCE_ID = "PROCESS_INSTANCE_ID";
String PARENT_PROCESS_INSTANCE_ID = "PARENT_PROCESS_INSTANCE_ID";
String ROOT_PROCESS_INSTANCE_ID = "ROOT_PROCESS_INSTANCE_ID";
String ACTIVITY_INSTANCE_ID = "ACTIVITY_INSTANCE_ID";
String REFERENCE_ACTIVITY_INSTANCE_ID = "REFERENCE_ACTIVITY_INSTANCE_ID";
String PARENT_ACTIVITY_INST_ID = "PARENT_ACTIVITY_INST_ID";

// 用户任务
String TASK_ID = "TASK_ID";
String PARENT_TASK_ID = "PARENT_TASK_ID";
String ASSIGNEE = "ASSIGNEE";

// 部署 / 资源通用
String DEPLOYMENT_ID = "DEPLOYMENT_ID";
String RESOURCE_ID = "RESOURCE_ID";
String VERSION_TAG = "VERSION_TAG";
```

## 用法

通过静态导入使用：

```java


// 或精确导入
import static com.anyilanxin.kunpeng.protocol.admin.impl.RecordConstant.TENANT_ID;
        import static com.anyilanxin.kunpeng.protocol.impl.RecordConstant.DEPLOYMENT_ID;
```

然后写：

```java
private final LongProperty deploymentIdProp = new LongProperty(DEPLOYMENT_ID, -1);
private final LongProperty resourceIdProp = new LongProperty(RESOURCE_ID, -1);
private final StringProperty versionTagProp = new StringProperty(VERSION_TAG, "");
private final StringProperty tenantIdProp =
    new StringProperty(TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
```

字面量：

```java
private final StringProperty decisionDefinitionKeyProp =
    new StringProperty("DECISION_DEFINITION_KEY", "");
private final IntegerProperty historyTimeToLiveProp =
    new IntegerProperty("HISTORY_TIME_TO_LIVE", -1);
private final BooleanProperty suspensionProp = new BooleanProperty("SUSPENSION", true);
```

## 命名规则

key 字符串统一 `UPPER_SNAKE_CASE`，跟字段含义对齐：

| 字段（驼峰） | key 字符串 |
|------------|-----------|
| `processDefinitionId` | `PROCESS_DEFINITION_ID` |
| `decisionDefinitionKey` | `DECISION_DEFINITION_KEY` |
| `historyTimeToLive` | `HISTORY_TIME_TO_LIVE` |
| `suspension` | `SUSPENSION` |
| `startable` | `STARTABLE` |
| `candidateStarterGroups` | `CANDIDATE_STARTER_GROUPS` |
| `resourceType` | `RESOURCE_TYPE` |

⚠️ 一旦定下来某个 Record 用某个 key 字符串，**不要改**（会破坏序列化兼容）。

## 反例（不要做）

```java
// ❌ 不要往 RecordConstant 加常量
// 在 RecordConstant.java 里加：
//   String HISTORY_TIME_TO_LIVE = "HISTORY_TIME_TO_LIVE";
//   String DECISION_DEFINITION_KEY = "DECISION_DEFINITION_KEY";

// ❌ 不要用魔法数字 / 缩写
new LongProperty("ddid", -1)             // 看不懂
new StringProperty("ddk", "")            // 模糊

// ❌ 不要混用风格
new LongProperty("deploymentId", -1)     // camelCase，跟其他不统一
```
