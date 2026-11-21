import type { ThemeConfig } from 'antd';

export const appTheme: ThemeConfig = {
  token: {
    colorPrimary: '#246bfd',
    colorInfo: '#246bfd',
    colorSuccess: '#13a57a',
    colorWarning: '#ee9b32',
    colorError: '#ef6461',
    colorText: '#17233c',
    colorTextSecondary: '#73819b',
    colorBgLayout: '#f3f7fd',
    colorBorderSecondary: '#e8eef7',
    borderRadius: 12,
    borderRadiusLG: 18,
    fontFamily: 'Inter, "SF Pro Display", "PingFang SC", "Microsoft YaHei", sans-serif',
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
