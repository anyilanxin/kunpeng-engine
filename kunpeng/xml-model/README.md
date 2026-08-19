# kunpeng-xml-model Change Notes

[中文](./README.zh-CN.md)

The code in this module originates from the xml-model module (generic XML model API written in Java, the foundation used by the BPMN/DMN model APIs) of the Camunda 7 platform:

- Upstream repository: <https://github.com/camunda/camunda-bpm-platform>
- Source commit at import time (last upstream commit included): <https://github.com/camunda/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>

## Why This Fork

The upstream camunda-bpm-platform repository is no longer actively maintained, while this project needs to iterate quickly. To avoid being blocked on an unmaintained upstream, the required model modules were imported into this repository so they can be maintained and evolved here.

## Changes in This Repository

1. The module was imported as-is: the upstream `org.camunda.*` packages and the Maven `pom.xml` are preserved unchanged at this stage.
2. Integration into this repository's unified Gradle build, package renames and other adjustments will be done in follow-up commits.

## Copyright and License Notes

- The original copyright and license headers from upstream must be preserved; do not modify or remove them.
- For newly added files, the applicable license is determined by the license header and copyright notice of each file.
