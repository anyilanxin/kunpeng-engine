import { useEffect, useRef } from 'react';
import * as echarts from 'echarts/core';
import { BarChart, GaugeChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsCoreOption } from 'echarts/core';

echarts.use([
  BarChart,
  GaugeChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer,
]);

/** ECharts 轻封装：浅色控制台默认样式、随容器自适应、option 变更即重绘 */
export default function EChart({
  option,
  height = 260,
}: {
  option: EChartsCoreOption;
  height?: number;
}) {
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
        textStyle: { color: '#51637e', fontFamily: 'inherit' },
        tooltip: {
          backgroundColor: '#ffffff',
          borderColor: '#dbe6f5',
          textStyle: { color: '#243b57' },
        },
        ...option,
      },
      { notMerge: true },
    );
  }, [option]);

  return <div ref={container} style={{ width: '100%', height }} />;
}
