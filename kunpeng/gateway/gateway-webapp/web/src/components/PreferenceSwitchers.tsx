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
import { Button, Dropdown } from 'antd';
import { CheckOutlined, GlobalOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { flushSync } from 'react-dom';
import { changeLanguage, LANGUAGES } from '../i18n';
import type { AppLanguage } from '../i18n';
import { useThemeMode } from '../theme/ThemeModeContext';

interface ViewTransitionLike {
  ready: Promise<void>;
  skipTransition: () => void;
}

type DocumentWithViewTransition = Document & {
  startViewTransition?: (updateCallback: () => void) => ViewTransitionLike;
};

/** 明暗切换（AntD Button + 日/月图标）：点击处圆形扩散过渡（View Transition API，参考 anyi-platform），降级为直接切换 */
export function ThemeToggle() {
  const { mode, toggleMode } = useThemeMode();
  const { t } = useTranslation();

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    const doc = document as DocumentWithViewTransition;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!doc.startViewTransition || reducedMotion) {
      toggleMode();
      return;
    }
    const x = event.clientX;
    const y = event.clientY;
    const endRadius = Math.hypot(
      Math.max(x, window.innerWidth - x),
      Math.max(y, window.innerHeight - y),
    );
    const transition = doc.startViewTransition(() => {
      flushSync(() => toggleMode());
    });
    const toDark = mode === 'light';
    void transition.ready.then(() => {
      const clipPath = [
        `circle(0px at ${x}px ${y}px)`,
        `circle(${endRadius}px at ${x}px ${y}px)`,
      ];
      const animate = document.documentElement.animate(
        {
          clipPath: toDark ? [...clipPath].reverse() : clipPath,
        },
        {
          duration: 450,
          easing: 'ease-in',
          pseudoElement: toDark ? '::view-transition-old(root)' : '::view-transition-new(root)',
        },
      );
      animate.onfinish = () => transition.skipTransition();
    });
  };

  const ariaLabel = t(mode === 'dark' ? 'app.header.themeToLight' : 'app.header.themeToDark');
  return (
    <Button
      aria-label={ariaLabel}
      className="header-icon-button"
      icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
      onClick={handleClick}
      title={ariaLabel}
      type="text"
    />
  );
}

/** 语言切换：AntD Dropdown 单选菜单，当前语言打勾 */
export function LanguageToggle() {
  const { t, i18n } = useTranslation();
  return (
    <Dropdown
      menu={{
        items: LANGUAGES.map((lang) => ({
          key: lang.value,
          label: (
            <span className="pref-lang-item">
              <span>{lang.label}</span>
              {lang.value === i18n.language && <CheckOutlined />}
            </span>
          ),
        })),
        selectable: true,
        selectedKeys: [i18n.language],
        onClick: ({ key }) => changeLanguage(key as AppLanguage),
      }}
      placement="bottomRight"
      trigger={['click']}
    >
      <Button aria-label={t('app.header.language')} className="header-icon-button" icon={<GlobalOutlined />} title={t('app.header.language')} type="text" />
    </Dropdown>
  );
}

/** 切换器组合（登录页/顶栏共用）：明暗 + 语言 */
export function PreferenceSwitchers() {
  return (
    <div className="pref-switchers">
      <ThemeToggle />
      <LanguageToggle />
    </div>
  );
}
