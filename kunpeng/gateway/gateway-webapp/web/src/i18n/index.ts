import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zhCN from './locales/zh-CN.json';
import zhTW from './locales/zh-TW.json';
import en from './locales/en.json';

/** 控制台支持的语言（初期：英文 / 简体中文 / 繁体中文） */
export type AppLanguage = 'zh-CN' | 'zh-TW' | 'en';

export const LANG_STORAGE_KEY = 'webapp-lang';

/** 切换器条目（label 各自用本语言展示，不参与翻译） */
export const LANGUAGES: { value: AppLanguage; label: string }[] = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'zh-TW', label: '繁體中文' },
  { value: 'en', label: 'English' },
];

const APP_LANGUAGES: AppLanguage[] = ['zh-CN', 'zh-TW', 'en'];

/** 浏览器语言标签 → 控制台语言：zh-Hant/TW/HK/MO→繁中、其余 zh→简中、en→英文 */
function fromBrowserTag(tag: string): AppLanguage | undefined {
  const lower = tag.toLowerCase();
  if (lower.startsWith('zh')) {
    return lower.includes('hant') || /[-_](tw|hk|mo)/.test(lower) ? 'zh-TW' : 'zh-CN';
  }
  if (lower.startsWith('en')) {
    return 'en';
  }
  return undefined;
}

function detectLanguage(): AppLanguage {
  try {
    const stored = localStorage.getItem(LANG_STORAGE_KEY);
    if (stored && (APP_LANGUAGES as string[]).includes(stored)) {
      return stored as AppLanguage;
    }
    for (const tag of navigator.languages ?? []) {
      const matched = fromBrowserTag(tag);
      if (matched) {
        return matched;
      }
    }
  } catch {
    // localStorage/Navigator 不可用时落默认语言
  }
  return 'zh-CN';
}

void i18n.use(initReactI18next).init({
  resources: {
    'zh-CN': { translation: zhCN },
    'zh-TW': { translation: zhTW },
    en: { translation: en },
  },
  lng: detectLanguage(),
  fallbackLng: 'zh-CN',
  interpolation: { escapeValue: false },
});

export { i18n };

/** 切换语言：i18next 热切换 + localStorage 持久化 + <html lang> 同步 */
export function changeLanguage(language: AppLanguage): void {
  void i18n.changeLanguage(language);
  try {
    localStorage.setItem(LANG_STORAGE_KEY, language);
  } catch {
    // 存储不可用时仅本次会话生效
  }
  document.documentElement.lang = language;
}

/** AntD 内置组件 locale 与 i18next 语言枚举对齐用 */
export function toAntdLocaleKey(language: string): keyof typeof LOCALE_KEYS {
  return (LOCALE_KEYS as Record<string, keyof typeof LOCALE_KEYS>)[language] ?? 'zh-CN';
}

const LOCALE_KEYS = { 'zh-CN': 'zh-CN', 'zh-TW': 'zh-TW', en: 'en' } as const;
