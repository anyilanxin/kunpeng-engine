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
import { theme } from 'antd';
import type { ThemeConfig } from 'antd';
import type { ThemeMode } from './ThemeModeContext';

/** 明暗共用的品牌与形状 token（浅色值即历史现值，暗色仅换表面与文字基色） */
const sharedToken = {
  colorPrimary: '#246bfd',
  colorInfo: '#246bfd',
  colorSuccess: '#13a57a',
  colorWarning: '#ee9b32',
  colorError: '#ef6461',
  borderRadius: 12,
  borderRadiusLG: 18,
  fontFamily: 'Inter, "SF Pro Display", "PingFang SC", "Microsoft YaHei", sans-serif',
};

const lightTheme: ThemeConfig = {
  token: {
    ...sharedToken,
    colorText: '#17233c',
    colorTextSecondary: '#73819b',
    colorBgLayout: '#f3f7fd',
    colorBorderSecondary: '#e8eef7',
    boxShadowTertiary: '0 10px 36px rgba(38, 82, 147, 0.08)',
  },
  components: {
    Button: {
      controlHeight: 38,
      primaryShadow: '0 7px 16px rgba(36, 107, 253, 0.2)',
      fontWeight: 500,
    },
    Card: {
      borderRadiusLG: 18,
      boxShadowTertiary: '0 10px 36px rgba(38, 82, 147, 0.07)',
    },
    Input: {
      controlHeight: 38,
      activeShadow: '0 0 0 3px rgba(36, 107, 253, 0.08)',
    },
    Select: {
      controlHeight: 38,
    },
    Menu: {
      itemHeight: 46,
      itemBorderRadius: 12,
      itemSelectedBg: '#eaf1ff',
      itemSelectedColor: '#246bfd',
      itemColor: '#66758f',
      iconSize: 18,
    },
    Table: {
      headerBg: '#f7faff',
      headerColor: '#73819b',
      headerSplitColor: 'transparent',
      rowHoverBg: '#f7faff',
      cellPaddingBlock: 16,
    },
    Tabs: {
      inkBarColor: '#246bfd',
      itemSelectedColor: '#246bfd',
      itemHoverColor: '#246bfd',
      titleFontSize: 14,
    },
    Drawer: {
      borderRadiusLG: 18,
    },
  },
};

const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    ...sharedToken,
    colorText: '#e6ecf8',
    colorTextSecondary: '#9fb0c9',
    colorBgLayout: '#0c1424',
    // 容器底统一用面板深蓝，避免 AntD 暗色默认 #141414 中性黑与深蓝面板撞色
    colorBgContainer: '#14203a',
    colorBorderSecondary: '#23334f',
    boxShadowTertiary: '0 10px 36px rgba(0, 0, 0, 0.45)',
  },
  components: {
    Button: {
      controlHeight: 38,
      primaryShadow: '0 7px 16px rgba(36, 107, 253, 0.35)',
      fontWeight: 500,
    },
    Card: {
      borderRadiusLG: 18,
      boxShadowTertiary: '0 10px 36px rgba(0, 0, 0, 0.4)',
    },
    Input: {
      controlHeight: 38,
      activeShadow: '0 0 0 3px rgba(77, 141, 255, 0.25)',
    },
    Select: {
      controlHeight: 38,
    },
    Menu: {
      itemHeight: 46,
      itemBorderRadius: 12,
      itemSelectedBg: 'rgba(77, 141, 255, 0.16)',
      itemSelectedColor: '#6ea3ff',
      itemColor: '#8b9bb5',
      iconSize: 18,
    },
    Table: {
      colorBgContainer: 'transparent',
      headerBg: '#14203a',
      headerColor: '#8b9bb5',
      headerSplitColor: 'transparent',
      rowHoverBg: '#16233e',
      cellPaddingBlock: 16,
    },
    Tabs: {
      inkBarColor: '#4d8dff',
      itemSelectedColor: '#4d8dff',
      itemHoverColor: '#4d8dff',
      titleFontSize: 14,
    },
    Drawer: {
      borderRadiusLG: 18,
    },
  },
};

/** 按明暗模式构建 AntD 主题：浅色保持历史现值，暗色走 darkAlgorithm + 定制 token */
export function buildTheme(mode: ThemeMode): ThemeConfig {
  return mode === 'dark' ? darkTheme : lightTheme;
}
