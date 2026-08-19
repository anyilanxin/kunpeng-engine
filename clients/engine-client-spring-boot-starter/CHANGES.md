# kunpeng-camunda-spring-boot-starter Change Notes

[中文](./CHANGES.zh-CN.md)

The code in this module originates from the camunda-spring-boot-starter module (Spring Boot starter
for the Camunda 8 java client) of the Camunda 8 / Zeebe repository:

- Upstream repository: <https://github.com/camunda/camunda>
- Source commit at import time (last upstream commit included): <https://github.com/camunda/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>

## Why This Fork

Importing the module into this repository makes day-to-day maintenance easier, and allows us to modify
and extend it freely — adjusting the code to our own needs and adding our own files — without being
constrained by the upstream release cadence.

## Changes in This Repository

1. The module was imported as-is: the upstream `io.camunda.client.*` packages and the Maven
   `pom.xml` are preserved unchanged at this stage.
2. Integration into this repository's unified Gradle build, package renames and other adjustments
   will be done in follow-up commits.

## Copyright and License Notes

- The original copyright and license headers from upstream must be preserved; do not modify or
  remove them.
- For newly added files, the applicable license is determined by the license header and copyright
  notice of each file.
