English | [简体中文](CHANGELOG.zh-CN.md)

# Changelog

All notable changes to this project are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/), and entries follow the repository's commit convention (`feat` / `fix` / `refactor` / `perf`).

## [Unreleased]

## [2026.9.0-alpha1] - 2026-10-20

Initial public alpha: a microservices orchestration engine built from scratch — BPMN 2.0 execution, partitioned event logs, Raft replication, gateway/broker separation, no relational database, and sink-based export.

### Added

**Engine & process execution**

- Full engine processing chain: protocol records and the storage layer (structpack key-id self-describing frames, RocksDB persistence)
- Engine-side expression evaluation command chain
- Inter-partition command dispatch with parallel/serial pipelines (cluster dispatch subsystem)
- Startup recovery chain: seed eventlog recovery from the persisted committed position and replay committed events on engine and dispatch startup
- Data surgery phase one: single-copy freeze and drain on both sides with idempotent state migration
- DMN engine and the script engine module
- Resource-level business extended data for deployments

**Job system**

- Distributed job push/pull dispatching: stable-session aggregated subscriptions, competing consumption, broker-side by-type rotation, deadline bookkeeping, and a `/actuator/jobstreams` observability endpoint
- Default stream job worker

**Cluster & messaging**

- Cluster messaging migrated onto Aeron (UDP unicast, three-plane port model), with the business plane on a dedicated port instance plus a node port registry, and short-circuiting of self-addressed messages via Aeron IPC
- Unified SWIM member entities and the metadata channel piggybacking on membership dissemination
- Leader-authoritative metadata reads (mode-based reads on raft partitions, leader-routed read facade, inter-partition commands routed via the unified topology service)
- Cluster messaging observability for dropped broadcasts and unhandled subjects

**Snapshot & bootstrap**

- Cross-partition bootstrap snapshot transfer chain (infoSent state machine, three-layer verification, per-chunk CRC)
- Unified snapshot stores under bootstrap/merge/raft packages with merge result notification and raft snapshots taken first
- Raft snapshot metadata carrying process/sink positions and snapshot time, with real position suppliers wired into snapshot providers

**Journal**

- Journal hardening: segment preallocation via FilePreallocator, CRC32c checksum on the hot path, bounded retry on segment re-creation after leftover files

**Gateway & API**

- Complete gateway REST API with Spring Framework 7 native API versioning (`/api/{version}`), and the embedded gateway as the default form (conditional REST wiring)
- Gateway gRPC port derived from the cluster start-port anchor

**Sink & backpressure**

- Sink export subsystem: RecordSink SPI lifecycle, position acknowledgement and retry
- RDBMS sink: TableName-registry naming, columns aligned to protocol record keys, authoritative `SINK_POSITION` table, sequence-flow instance rows, `tableNameCase`, and auto-DDL table remarks
- Sink-side partition-local query engine (`SinkQueryService`)
- Sink uber-jar packaging and integration guide
- Sink-lag backpressure gate on business command admission

**Observability & docs**

- Monitor stack (Prometheus + Grafana, single six-section dashboard) with a metric-to-panel coverage gate
- Bilingual user guide (startup forms & port model), README overview diagrams, sink integration guide, and the BPMN 2.0.2 specification PDFs

**Ecosystem**

- Connector tree migrated onto the engine and enabled in the build (REST and script connectors out of the box, runtime, element-template generator)
- Client SDK (client-java + Spring Boot starter)

### Changed

- broker-client merged into a single module (core/business/admin) with a unified envelope and SBE transport schema
- Network configuration split into cluster, broker and gateway planes; dist assembly profiles tightened
- RDBMS sink stops pushing positions back to the engine (the authoritative position table takes over)
- structpack id management switched to smallest-free-id allocation (marker ledger removed)
- BPMN element/type enums extracted into bpm-common; DMN engine reworked
- Client SDK packages migrated to `com.anyilanxin.kunpeng`
- cluster-netty removed after the three-framework messaging benchmark settled on Aeron
- Dependency refresh across the BOMs

### Fixed

- Gateway bind/advertise split: wildcard bind by default with an explicitly routable advertised host (fixes "no reachable gateway")
- Shutdown chain: drive TO_INACTIVE layer-by-layer and grant the checker shutdown authority (fixes the JVM crash when timers touched released RocksDB transactions during shutdown)
- Long-poll job fetches no longer misreported as failures when the client deadline fires before the poll window (DEADLINE_EXCEEDED treated as an empty batch)
- `maxJobsActivate` honored in batch activation scans; failed activate probes surfaced; `streamEnabled` kept when worker properties default
- ActorCell post-sleep recheck no longer scans subscriptions unguarded (ticket-delta plus queue recheck); lane pool shutdown runs on the dispatch actor; drain-batch yield no longer starves actor control commands
- SBE decoding of empty variable-length fields (limit must advance even at length 0) and field ordering in the job stream wire codec
- structpack passthrough for zero-declared containers no longer drops data
- Deployment distribution accounting and retry semantics redesigned (ACKs carry partition ids)
- Sink: startup failures logged at error level, resume position clamped to the log tail
- Snapshot schedule config bounds and entry-trigger threshold validation; missing raft partition tolerated in listener step shutdown
- Business-plane addressing: local-node direct reads and bounded re-resolution when the remote is not yet ready
- Missing TIME_OUT case in the JobLifeCycle wire lookup (replay no longer throws out-of-bounds)
