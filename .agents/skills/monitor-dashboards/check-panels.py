#!/usr/bin/env python3
"""指标↔面板覆盖检查（monitor-dashboards 铁律门禁）。

扫描 kunpeng/**/src/main/**/*MetricsDoc.java 与 *Metrics.java 的三种指标声明形态
（MetricsDoc 枚举构造参数、*.builder("name")、return "name"），按 Micrometer→Prometheus
命名规则推导指标名，与 monitor/grafana/dashboards/kunpeng.json 面板 expr 双向对照：

- 未覆盖（有指标无面板）→ 退出码 1
- 疑似拼写错误（面板引用了未定义指标）→ 退出码 1

用法：python3 .agents/skills/monitor-dashboards/check-panels.py（仓库根目录执行）
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
SKILL_DOC = Path(__file__).resolve().parent
DASHBOARD = REPO / "monitor" / "grafana" / "dashboards" / "kunpeng.json"

# 指标声明形态（MetricsDoc 枚举构造兼容 (name, Type, desc) 与 (name, desc, Type) 两种参数序、换行排版）
ENUM_METRIC = re.compile(
    r'\(\s*"([a-z][a-z0-9._]*)"(?:,\s*"[^"]*")?,\s*Type\.\w+'
)
BUILDER_METRIC = re.compile(
    r'(?:Timer|Counter|Gauge|DistributionSummary)\.builder\("([a-z][a-z0-9._]*)"'
)
RETURN_METRIC = re.compile(r'return "([a-z][a-z0-9._]*[._][a-z0-9._]*)"')

# Prometheus 导出形态后缀（面板引用侧的可能形态）
EXPORT_SUFFIXES = ("", "_total", "_count", "_sum", "_max", "_seconds_count", "_seconds_sum",
                   "_seconds_max")

# 非本仓库代码定义、允许面板直接引用的第三方指标前缀
THIRD_PARTY_PREFIXES = (
    "jvm_", "process_", "system_", "http_", "hikaricp_", "logback_", "netty_", "grpc_",
    "spring_", "tomcat_", "executor_", "cpu_", "disk_", "mem_",
)
# PromQL 关键字/函数/标签，不算指标引用
PROMQL_WORDS = {
    "rate", "sum", "min", "max", "avg", "by", "without", "irate", "increase", "count",
    "count_over_time", "clamp_min", "clamp_max", "on", "group_left", "group_right", "and", "or",
    "unless", "ignoring", "offset", "bool", "topk", "bottomk", "quantile", "histogram_quantile",
    "label_replace", "sort", "sort_desc", "abs", "ceil", "floor", "round", "changes", "delta",
    "deriv", "predict_linear", "resets", "stddev", "stdvar", "absent", "scalar", "time", "vector",
    "job", "instance", "pod", "namespace", "partition", "partition_group", "sink_id",
    "partitionGroupName", "sinkId", "memberId",
    "follower", "intent", "context", "reason", "action", "type", "log", "le", "NaN", "Inf",
}


def collect_defined_metrics() -> dict[str, Path]:
    """源码三种声明形态 → {prometheus 基础名: 声明文件}"""
    defined: dict[str, Path] = {}
    src_root = REPO / "kunpeng"
    files = [p for p in src_root.rglob("*.java")
             if "/src/main/" in str(p) and "/build/" not in str(p)
             and (p.name.endswith("MetricsDoc.java") or p.name.endswith("Metrics.java"))]
    for path in sorted(files):
        text = path.read_text(encoding="utf-8")
        for regex in (ENUM_METRIC, BUILDER_METRIC, RETURN_METRIC):
            for name in regex.findall(text):
                defined.setdefault(name.replace(".", "_"), path)
    return defined


def collect_exprs() -> list[str]:
    text = json.loads(DASHBOARD.read_text(encoding="utf-8"))
    exprs: list[str] = []

    def walk(panels: list[dict]) -> None:
        for panel in panels:
            if panel.get("type") == "row":
                continue
            for target in panel.get("targets", []):
                expr = target.get("expr", "")
                if expr:
                    exprs.append(expr)

    walk(text.get("panels", []))
    return exprs


def referenced_names(expr: str) -> set[str]:
    """expr 中的指标形态标识符（含下划线、非关键字、非第三方前缀）"""
    names = set()
    for token in re.findall(r"[a-zA-Z_][a-zA-Z0-9_]*", expr):
        if token in PROMQL_WORDS or token.startswith(THIRD_PARTY_PREFIXES):
            continue
        if "_" not in token:
            continue
        names.add(token)
    return names


def main() -> int:
    defined = collect_defined_metrics()
    exprs = collect_exprs()
    referenced: set[str] = set()
    for expr in exprs:
        referenced |= referenced_names(expr)

    # 正向：有指标无面板
    uncovered = []
    for base in sorted(defined):
        if not any(base + suffix in referenced for suffix in EXPORT_SUFFIXES):
            uncovered.append((base, str(defined[base])))

    # 反向：面板引用了未定义指标（排除以任一已定义指标为前缀的衍生形态，如 xxx_bucket）
    unknown = set()
    defined_list = sorted(defined, key=len, reverse=True)
    for ref in sorted(referenced):
        matched = False
        for base in defined_list:
            if ref == base or ref.startswith(base + "_"):
                matched = True
                break
        if not matched:
            unknown.add(ref)

    ok = True
    if uncovered:
        ok = False
        print("未覆盖指标（有定义无面板）：")
        for base, path in uncovered:
            print(f"  {base}  <- {path}")
    if unknown:
        ok = False
        print("面板引用了未定义指标（疑似拼写错误或漏定义）：")
        for ref in unknown:
            print(f"  {ref}")
    if ok:
        print(f"指标/面板全对齐：{len(defined)} 个已定义指标，{len(exprs)} 条面板 expr，覆盖归零。")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
