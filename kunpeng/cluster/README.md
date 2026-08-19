# kunpeng-cluster Change Notes

[中文](./README.zh-CN.md)

The code in this module originates from the hard fork of Atomix maintained by Camunda (Zeebe):

- Upstream repository: <https://github.com/camunda/camunda>
- Source commit: <https://github.com/camunda/camunda/commit/d9de58a0cee5a84df87cba1ebc0d4fee64836e7d> (corresponding to the Camunda 8.0.4 tag)
- Archived fork of the upstream repository (used as our code archive): <https://github.com/anyilanxin/camunda/commit/d9de58a0cee5a84df87cba1ebc0d4fee64836e7d>

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

On top of the Camunda 8.0.4 code base, this repository made further trimming and adjustments with the
goal of **easier maintenance**:

1. **Only the atomix and journal code is kept**; all other modules were removed.
2. **Code distributed under the Camunda License 1.0 was removed**. The remaining code in this module is
   licensed under Apache-2.0 (see the license headers of the respective files).
3. The build was migrated from Maven to Gradle and integrated into this repository's unified build.
4. **For new files added from now on, the applicable license is determined by the license header and
   copyright notice of each file**, rather than the Apache license.

## Copyright and License Notes

- The original copyright headers from upstream (Atomix / Camunda) must be preserved; do not modify or
  remove them.
- For newly added files, the applicable license is determined by the license header and copyright notice of each file.
