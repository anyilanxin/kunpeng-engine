# Engine for Microservices Orchestration

Kunpeng provides visibility into and control over business processes that span multiple microservices.

## How it works

```mermaid
flowchart LR
  CLI["Client<br/>applications · SDK"]

  subgraph GWS["Gateways · stateless · dynamically load-balanced"]
    direction TB
    GW1["Gateway"]
    GW2["Gateway"]
    GW3["Gateway"]
  end

  subgraph CLUSTER["Broker cluster · Raft-replicated partitions · unified coordination"]
    direction LR
    subgraph B1["Broker 1"]
      direction LR
      RG["Partition<br/>Raft group"] --> ENG["BPMN 2.0<br/>process engine"] --> EL["Event log"]
    end
    B2["Broker 2"]
    B3["Broker 3"]
    B1 <-.->|"Raft"| B2
    B2 <-.->|"Raft"| B3
  end

  S1["Sink"] --> R1[("RDBMS")]
  S2["Sink"] --> R2[("Elasticsearch")]
  S3["Sink"] --> R3[("Message queue")]

  CLI ==> GW1
  CLI ==> GW2
  CLI ==> GW3
  GW1 ==> CLUSTER
  GW2 ==> CLUSTER
  GW3 ==> CLUSTER
  EL ==> S1
  EL ==> S2
  EL ==> S3

  classDef cli fill:#EFF6FF,stroke:#2563EB,color:#1E40AF
  classDef gw fill:#DBEAFE,stroke:#2563EB,color:#1E40AF
  classDef part fill:#DCFCE7,stroke:#059669,color:#065F46
  classDef peer fill:#FFFFFF,stroke:#94A3B8,stroke-dasharray:6 4,color:#475569
  classDef log fill:#F5F3FF,stroke:#7C3AED,color:#5B21B6
  classDef ext fill:#F8FAFC,stroke:#64748B,color:#334155
  class CLI cli
  class GW1,GW2,GW3 gw
  class CLUSTER,B1,RG,ENG part
  class B2,B3 peer
  class EL log
  class S1,S2,S3,R1,R2,R3 ext
```

Clients spread load dynamically across stateless gateways, which route each request to the leading partition of a Raft group. There the BPMN 2.0 engine executes processes and appends every step to the partition's event log; the same log fans out through multiple sinks into multiple external stores — resumable, and off the execution path.

**Why Kunpeng?**

* Define processes visually in [BPMN 2.0](https://www.omg.org/spec/BPMN/2.0.2/)
* Choose your programming language
* Deploy with [Docker](https://www.docker.com/) and [Kubernetes](https://kubernetes.io/)
* Build processes that react to messages from [Kafka](https://kafka.apache.org/) and other message queues
* Scale horizontally to handle very high throughput
* Fault tolerance (no relational database required)
* Export process data for monitoring and analysis
* Engage with an active community

## Licenses

### 1. Use License

1. [APACHE-2.0](./licenses/APACHE-2.0.txt)
2. [GNU-AGPL-3.0](./licenses/GNU-AGPL-3.0.txt)
3. [MPL 2.0](./licenses/MPL-2.0.txt)

### 2. License Notice

1. For details about the file license, see the file header
2. Please do not touch the copyright headers, we need to keep their copyright on their files. On new files we create, we
   will have our License headers with our copyright.
3. Where agreement compatibility permits, most of the files have been added with our own copyright header(We made
   extensive adjustments to the file to allow for quick iteration).
