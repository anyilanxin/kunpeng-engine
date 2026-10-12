---
name: monitor-dashboards
description: Monitoring panel standard for the monitor/ stack — adding an engine metric is not done until its dashboard panel exists (metric-to-panel parity), plus conventions for editing the single Kunpeng dashboard and the check-panels.py coverage gate.
---

# monitor-dashboards — 监控面板标准

## 铁律：有指标就有面板

**添加指标 ≠ 完成。任何新增/修改引擎指标（MetricsDoc 枚举、`*.builder("name")` 直接注册、`return "name"` 形态）都必须同步在 `monitor/grafana/dashboards/kunpeng.json` 补上/更新对应面板信息，并以覆盖检查归零为验收条件。**

指标与面板一一对应是添加指标的标准流程的一部分，不是事后可选项：

```
① 定义指标（MetricsDoc 枚举 / builder 注册）
② 代码埋点
③ 推导 Prometheus 指标名（规则见 01-面板规范.md）
④ 在 kunpeng.json 对应分节补面板（模板见 01-面板规范.md）
⑤ 跑覆盖检查，退出码 0 才算收口：
   python3 .agents/skills/monitor-dashboards/check-panels.py
```

删除指标同理：删埋点必须连带删面板引用，否则反向检查报"面板引用但未定义"（拼写错误也走这条暴露）。

## 面板结构（修改前必读）

- **全系统一块面板**：`monitor/grafana/dashboards/kunpeng.json`（uid=`kunpeng`），不用多面板拆分（用户定案）。
- 六个分节 row：总览 Overview / Raft 复制与共识 / EventLog 事件日志 / 调度器 Scheduler / Sink 日志外发 / JVM 与运行时。新面板放进语义对应的分节；没有合适分节才新增 row。
- 面板 JSON 全部引用变量 `${DS_PROMETHEUS}` 与 `job=~"$job"`；分区/通道维度的面板再叠加 `$partition_group` / `$partition` / `$sink_id`。
- 布局约定：`gridPos` 以 24 列宽 shelf 排布（`w` 常用 6/8/12/16/24，`h` 默认 8，stat 概览为 4）；`id` 全文件唯一（顺延编号）；改完只需 JSON 合法，Grafana provisioning 约 30s 热加载，**不需要重启栈，也不代测**（监控由用户自测）。
- 部署物与抓取配置（docker-compose、prometheus.yml、Makefile）的修改入口与注意点见 `monitor/README.md`。

## 验收门禁

```bash
python3 .agents/skills/monitor-dashboards/check-panels.py   # 退出码 0 = 指标/面板全对齐
python3 -m json.tool monitor/grafana/dashboards/kunpeng.json > /dev/null   # JSON 合法
```

检查脚本扫描 `kunpeng/**/src/main/**/*MetricsDoc.java`、`*Metrics.java`（`Timer/Counter/Gauge/DistributionSummary.builder("name")` 与 `return "name"` 三种声明形态），推导 Prometheus 名后与面板 JSON 双向对照：
- **未覆盖**（有指标无面板）→ 必须补面板，退出码 1；
- **疑似拼写错误**（面板引用了未定义指标）→ 核对指标名拼写。

## 详细规范

指标名→Prometheus 名映射、Type 后缀规则、tag 键总表、枚举值映射、面板类型选择与常用 PromQL 套路、面板 JSON 模板：见 [01-面板规范.md](./01-面板规范.md)。
