# Connector FEEL utilities

This module provides the `FeelEngineWrapper` API - a wrapper around
[QLExpress4](https://github.com/alibaba/QLExpress) that enables the Connector SDK and Runtime
components to evaluate expressions. The module keeps its Connector-specific functions
(`bpmnError`, `jobError`, `ignoreError`, `backoff`, `createDocument`) and the null-tolerant
evaluation semantics (missing paths evaluate to `null`).
