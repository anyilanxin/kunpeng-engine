# kunpeng-connectors Change Notes

[中文](./CHANGES.zh-CN.md)

The code in this directory originates from the Camunda Connectors repository (connector SDK, runtime
and out-of-the-box connectors):

- Upstream repository: <https://github.com/camunda/connectors>
- Source commit at import time (last upstream commit included): <https://github.com/camunda/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/connectors/commit/6bdd060a30968ce176bfaa15e55ab7f015fffbbb>

## Why This Fork

Importing the project into this repository makes day-to-day maintenance easier, and allows us to modify
and extend it freely — adjusting the code to our own needs and adding our own files — without being
constrained by the upstream release cadence.

## Changes in This Repository

1. The full content of the upstream repository root was extracted into the `connectors/` directory.
   The upstream Maven engineering is preserved as-is at this stage: the root `pom.xml`, `parent/pom.xml`,
   the Maven wrapper (`.mvn/`, `mvnw`), `.github/` workflows and QA configs. All files licensed under
   the Camunda License 1.0 were removed at import time.
2. Follow-up adjustments after import: license headers were unified to the standard Apache 2.0 header
   with the anyilanxin copyright notice appended; generated `element-templates/versioned/` artifacts
   were removed; code style was unified; a few documentation and version references were touched up.
3. **Pruned to a source-only layout**: upstream CI/QA (`.github/`, `.ci/`), the e2e test suites, upstream
   docs, community files and the Maven reactor root (root/parent `pom.xml`, Maven wrapper) were removed;
   `LICENSE`, `licenses/` (Apache-2.0 / MPL-2.0) were added and `NOTICE` was updated.
4. `connectors/` is kept as source only — it is NOT part of this repository's unified Gradle build and is
   no longer built with Maven (the Maven root project was removed).

## Copyright and License Notes

- The upstream project is licensed under Apache 2.0. The license headers now carry the anyilanxin
  copyright notice appended to the original one; do not modify or remove them.
- For newly added files, the applicable license is determined by the license header and copyright
  notice of each file.
