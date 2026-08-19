# kunpeng-connector Change Notes

[中文](./README.zh-CN.md)

This directory contains the code imported from the Camunda Connectors repository:

- Upstream repository: <https://github.com/camunda/connectors>
- Source commit at import time (last upstream commit included): <https://github.com/camunda/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>

The import covers the Connector SDK, the out-of-the-box connectors, the Connector Runtime, the
element-template generators, secret providers, apps and the e2e tests, together with the Maven build.

## Why This Fork

Importing the connectors into this repository makes our own iteration easier and allows adapting
them to the kunpeng engine — modifying and extending the code freely, without being constrained by
the upstream release cadence.

## Changes in This Repository

1. **All code distributed under the Camunda License 1.0 was removed** during the import. The
   remaining code in this directory is licensed under Apache-2.0 (see the license headers of the
   respective files).
2. Otherwise the import is kept as-is at this stage: upstream `io.camunda.connector.*` packages,
   Maven poms and module layout are preserved unchanged.
3. Integration into this repository's unified Gradle build and adaptation to the kunpeng engine
   will be done in follow-up commits.
4. **For new files added from now on, the applicable license is determined by the license header
   and copyright notice of each file**, rather than the Apache license.

## Copyright and License Notes

- The original copyright headers from upstream must be preserved; do not modify or remove them.
- For newly added files, the applicable license is determined by the license header and copyright
  notice of each file.
- The original upstream README (Camunda Connectors project documentation) is superseded by these
  change notes and remains reachable via the archived upstream commit linked above.
