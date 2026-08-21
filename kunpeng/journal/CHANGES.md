# kunpeng-journal Change Notes

[中文](./CHANGES.zh-CN.md)

The code in this module originates from the journal (write-ahead log) implementation of Camunda (Zeebe):

- Upstream repository: <https://github.com/camunda/camunda>
- Source commit: <https://github.com/camunda/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c> (pulled from the main branch on Aug 19, 2026)
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>

## Upstream Background

This module contains the segmented journal implementation (package `io.camunda.zeebe.journal`) used by
Zeebe as its write-ahead log to persist state changes durably before they are applied.

## Changes in This Repository

The main motivations for the changes are: **resolving license conflicts** and **making the code easier
to maintain and iterate on for our own use**. On top of the upstream code base, this repository made
the following adjustments:

1. **All code not licensed under Apache-2.0 was removed** (e.g. code distributed under the Camunda
   License 1.0) to resolve license conflicts. All remaining code in this module is licensed under
   Apache-2.0 (see the license headers of the respective files).
2. **Only the code that is really needed is kept**, so the code base stays small and easy to maintain
   and iterate on.
3. The build was migrated from Maven to Gradle and integrated into this repository's unified build.
4. **For new files added from now on, the applicable license is determined by the license header and
   copyright notice of each file**, rather than the Apache license.

## Copyright and License Notes

- The original copyright headers from upstream (Camunda) must be preserved; do not modify or remove
  them.
- For newly added files, the applicable license is determined by the license header and copyright notice of each file.
