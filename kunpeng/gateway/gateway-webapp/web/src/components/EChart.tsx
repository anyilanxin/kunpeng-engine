/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { useEffect, useRef } from 'react';
import * as echarts from 'echarts/core';
import { BarChart, GaugeChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsCoreOption } from 'echarts/core';
import { useThemeMode } from '../theme/ThemeModeContext';

/** 明暗两档图表基础样式：文字/提示框底色随主题翻转（业务 option 在其后合并可覆盖） */
const CHART_BASE = {
  light: {
    textStyle: { color: '#51637e', fontFamily: 'inherit' },
    tooltip: {
      backgroundColor: '#ffffff',
      borderColor: '#dbe6f5',
      textStyle: { color: '#243b57' },
    },
  },
  dark: {
    textStyle: { color: '#9fb0c9', fontFamily: 'inherit' },
    tooltip: {
      backgroundColor: '#1a2540',
      borderColor: '#2a3c5e',
      textStyle: { color: '#dbe6f5' },
    },
  },
} as const;

echarts.use([
  BarChart,
  GaugeChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer,
]);

/** ECharts 轻封装：默认样式随明暗模式、随容器自适应、option 变更即重绘 */
export default function EChart({
  option,
  height = 260,
}: {
  option: EChartsCoreOption;
  height?: number;
}) {
  const { mode } = useThemeMode();
  const container = useRef<HTMLDivElement>(null);
  const chart = useRef<echarts.ECharts>();

  useEffect(() => {
    if (!container.current) {
      return;
    }
    const instance = echarts.init(container.current);
    chart.current = instance;
    const observer = new ResizeObserver(() => instance.resize());
    observer.observe(container.current);
    return () => {
      observer.disconnect();
      instance.dispose();
      chart.current = undefined;
    };
  }, []);

  useEffect(() => {
    chart.current?.setOption(
      {
        ...CHART_BASE[mode],
        ...option,
      },
      { notMerge: true },
    );
  }, [option, mode]);

  return <div ref={container} style={{ width: '100%', height }} />;
}
