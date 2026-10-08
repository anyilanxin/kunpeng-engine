# kunpeng-script-connector Change Notes

[中文](./CHANGES.zh-CN.md)

The code in this directory originates from the Camunda Community Hub script-connector project —
anything without a specific source note comes from there; parts originating from other projects are
the exception and carry their own separate source notes in their directories:

- Upstream repository: <https://github.com/camunda-community-hub/script-connector>
- Source commit at import time (last upstream commit included): <https://github.com/camunda-community-hub/script-connector/commit/7acc5cb4ba6c0750f7b366df38f112f30ecf8578>
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/script-connector/commit/7acc5cb4ba6c0750f7b366df38f112f30ecf8578>

This directory is an exception to the default source of the parent `connector/` tree (the Camunda
Connectors repository) — see [../CHANGES.md](../CHANGES.md).

## Why This Fork

Importing the project into this repository makes day-to-day maintenance easier, and allows us to modify
and extend it freely — adjusting the code to our own needs and adding our own files — without being
constrained by the upstream release cadence.

## Changes in This Repository

1. The full content of the upstream repository root was extracted into this directory; the upstream
   Maven engineering was preserved as-is at import time (root `pom.xml`, the `connector` and
   `runtime` modules, `.github/` workflows and community files).
2. Follow-up adjustments after import: pruned to the `connector` module only (the `runtime` sample
   application, `.github/` workflows, docker and community files were removed) and flattened to a
   single-module layout (`src/`, `element-templates/` at the directory root); the Maven build files
   were replaced by a Gradle `build.gradle` translated from the module `pom.xml` (internal
   `io.camunda.connector` artifacts map to local `project(...)` references; versions live in this
   repository's dependency BOM). All `pom.xml` files have been removed.

## Copyright and License Notes

- The upstream project is licensed under Apache 2.0.
- For newly added files, the applicable license is determined by the license header and copyright
  notice of each file.
