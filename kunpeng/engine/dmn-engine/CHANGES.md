# kunpeng-dmn-engine Change Notes

[中文](./CHANGES.zh-CN.md)

The code in this module originates from the dmn-engine module (standalone DMN
decision engine that parses and executes DMN decision models) of the Camunda 7
platform:

- Upstream repository: <https://github.com/camunda/camunda-bpm-platform>
- Source commit at import time (last upstream commit included): <https://github.com/camunda/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/camunda-bpm-platform/commit/ee4826e5e76c2348a1510ef46a2f4ccd3b080e48>

## Why This Fork

The upstream camunda-bpm-platform repository is no longer actively maintained,
while this project needs to iterate quickly. To avoid being blocked on an
unmaintained upstream, the required engine modules were imported into this
repository so they can be maintained and evolved here.

## Changes in This Repository

1. Packages renamed from the upstream `org.camunda.bpm.dmn.*` to
   `com.anyilanxin.kunpeng.engine.dmn.*`.
2. The upstream Maven `pom.xml` was dropped; the module is built by this
   repository's unified Gradle build (`build.gradle`).
3. The original copyright and license headers of the upstream files are
   preserved, with new copyright lines appended for modifications made in this
   repository.

## Copyright and License Notes

- The original copyright and license headers from upstream must be preserved;
  do not modify or remove them.
- For newly added files, the applicable license is determined by the license
  header and copyright notice of each file (Apache-2.0 and MPL-2.0 texts are
  kept under `licenses/`).
