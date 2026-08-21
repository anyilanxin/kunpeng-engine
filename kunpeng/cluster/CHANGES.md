# kunpeng-cluster Change Notes

[中文](./CHANGES.zh-CN.md)

The code in this module originates from the hard fork of Atomix maintained by Camunda (Zeebe):

- Upstream repository: <https://github.com/camunda/camunda>
- Source commit: <https://github.com/camunda/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c> (pulled from the main branch on Aug 19, 2026)
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>

## Upstream Background (Camunda's hard fork of Atomix)

This module contains a hard fork of [Atomix](https://github.com/atomix/atomix). Camunda originally used
Atomix as a regular dependency, but due to low upstream activity, PRs that remained unmerged for a long
time, and the upstream project moving to a complete rewrite in Go, Camunda decided to merge the fork
directly into its own repository and reduce it to only the code that was really used and needed, namely:

- a RAFT implementation (with storage)
- a SWIM implementation
- a usable transport implementation
- and some glue code

The benefits of this approach are: a single unified build, a shorter development and test cycle, easier
benchmarking and code-quality checks, and a simpler release process. The trade-offs are flakier tests in
the beginning and longer build times.

## Changes in This Repository

The main motivations for the changes are: **resolving license conflicts** and **making the code easier
to maintain and iterate on for our own use**. On top of the upstream code base, this repository made
the following adjustments:

1. **All code not licensed under Apache-2.0 was removed** (e.g. code distributed under the Camunda
   License 1.0) to resolve license conflicts. All remaining code in this module is licensed under
   Apache-2.0 (see the license headers of the respective files).
2. **Only the code that is really needed is kept** (atomix and journal); all other modules were
   removed, so the code base stays small and easy to maintain and iterate on.
3. The build was migrated from Maven to Gradle and integrated into this repository's unified build.
4. **For new files added from now on, the applicable license is determined by the license header and
   copyright notice of each file**, rather than the Apache license.

## Copyright and License Notes

- The original copyright headers from upstream (Atomix / Camunda) must be preserved; do not modify or
  remove them.
- For newly added files, the applicable license is determined by the license header and copyright notice of each file.
