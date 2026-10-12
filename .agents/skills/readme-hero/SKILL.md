---
name: readme-hero
description: README hero image (front-page panel) design system for docs/kunpeng-hero-*.svg (light/dark × Chinese/English, four synchronized SVGs). Covers the layout grid with exact coordinates, light/dark palettes, typography ladder, zh-en copy table, four-file sync rules, render verification workflow (rsvg-convert + analyze_image CDN trap), and the present-first push-on-command delivery rhythm.
---

# README 首图面板设计系统

README 首图（`docs/kunpeng-hero-*.svg`，简单线条风引擎总览面板）的设计规范与修改流程。**改首图前必读本技能**——首图是用户 9 轮反馈定稿的产物，有自己的审美红线，改错风格会被打回。

## 文件矩阵与同步铁律

| 文件 | 语言 | 底色 | 挂载处 |
|------|------|------|--------|
| `docs/kunpeng-hero-light.svg` | 中文 | 白 | `README.zh-CN.md`（`<picture>` 浅色分支的 `<img src>`） |
| `docs/kunpeng-hero-dark.svg` | 中文 | 深 | `README.zh-CN.md`（`<picture>` dark `srcset`） |
| `docs/kunpeng-hero-light-en.svg` | 英文 | 白 | `README.md` |
| `docs/kunpeng-hero-dark-en.svg` | 英文 | 深 | `README.md` |

- 四文件**布局、配色、字号严格同源**，唯一差异是注解语言（对照表见 01-设计规范.md §6）
- **改任意一个必须同步其余三个**，漏一个 = 明暗或中英不一致
- README 挂载写法：`<picture>` + `prefers-color-scheme`，GitHub 按浏览主题自动切换

## 修改流程（六步，顺序执行）

1. **定位改动点**：先 Read 四个文件的目标区域，确认坐标上下文再 Edit（Edit 门禁要求先读）
2. **四文件同步修改**：布局/配色/字号默认不动，只改任务要求的元素；加新元素先按 01-设计规范.md §3 坐标网格找位
3. **宽度自检**：中文每字 ≈ 字号 px；英文每字符 ≈ 0.55 × 字号；要求 文字宽 < 框宽 − 16（居中文本尤其严格）
4. **渲染验证**：`rsvg-convert -w 1150 docs/kunpeng-hero-light.svg -o /tmp/xx.png`（首选；qlmanage 备选但出方形 letterbox）
5. **视觉检查**：渲染结果转 JPEG（`sips -s format jpeg`）+ **唯一文件名**后再 Read 上传、`analyze_image` 检查溢出/重叠/残留字符——CDN 会按同路径/同哈希复用旧对象，同名上传会检查到旧图（详见下方陷阱）
6. **呈现后推送**：先把渲染效果给用户看，**用户说"推送"才** 执行 根目录 `./gradlew spotlessApply` → 路径限定 commit → push

## 视觉检查 CDN 陷阱（重要）

Read 工具上传图片到 CDN 时按内容哈希/路径去重：同路径的旧对象会被复用返回，`analyze_image` 可能读到**上一次**的图。规避：

- 上传前 `sips -s format jpeg -s formatOptions 95 in.png --out 唯一名.jpeg`（换编码 + 换名 = 强制新对象）
- 检查结果与预期不符时，先怀疑缓存再看图——曾因此误判"改动未生效"

## 交付节奏（用户定案）

改完先呈现（给文件路径 + 渲染说明），用户确认说"推送"才 commit + push。不要自作主张直接推。

## 详细规范

布局分区、全量元素坐标表、明暗配色对照、字号阶梯、中英文案对照表、审美红线：见 [01-设计规范.md](./01-设计规范.md)。
