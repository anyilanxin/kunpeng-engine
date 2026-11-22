import React, { useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import { App as AntdApp, ConfigProvider } from 'antd';
import { useTranslation } from 'react-i18next';
import zhCN from 'antd/locale/zh_CN';
import zhTW from 'antd/locale/zh_TW';
import enUS from 'antd/locale/en_US';
import './i18n';
import { toAntdLocaleKey } from './i18n';
import App from './App';
import { buildTheme } from './theme';
import { ThemeModeProvider, useThemeMode } from './theme/ThemeModeContext';
import './styles/global.css';
import './styles/pages.css';
import './styles/login.css';
import './styles/extra.css';

const ANTD_LOCALES = { 'zh-CN': zhCN, 'zh-TW': zhTW, en: enUS } as const;

/** 语言（i18next）+ 模式（ThemeModeContext）→ ConfigProvider 动态 locale/theme；标题与 <html lang> 随语言同步 */
function ThemedRoot() {
  const { mode } = useThemeMode();
  const { t, i18n } = useTranslation();
  const language = toAntdLocaleKey(i18n.language);

  useEffect(() => {
    document.title = t('app.title');
    document.documentElement.lang = language;
  }, [t, language]);

  return (
    <ConfigProvider locale={ANTD_LOCALES[language]} theme={buildTheme(mode)}>
      <AntdApp>
        <HashRouter>
          <App />
        </HashRouter>
      </AntdApp>
    </ConfigProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeModeProvider>
      <ThemedRoot />
    </ThemeModeProvider>
  </React.StrictMode>,
);
