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
import { defineConfig } from 'vite';
import type { Plugin } from 'vite';
import react from '@vitejs/plugin-react';

// 产物直接落模块 static/webapp（进程内静态挂载 /webapp/**）；hash 路由无服务端 fallback 需要
/** 构建产物协议头：全部 js chunk 与 css 产物头部注入 AGPL 版权声明（index.html 的头随源文件保留） */
function licenseHeaderPlugin(): Plugin {
  return {
    name: 'license-header',
    generateBundle(_, bundle) {
      for (const file of Object.values(bundle)) {
        if (file.type === 'chunk' && file.fileName.endsWith('.js')) {
          file.code = LICENSE_BANNER + '\n' + file.code;
        } else if (file.type === 'asset' && file.fileName.endsWith('.css')) {
          file.source = LICENSE_BANNER + '\n' + String(file.source);
        }
      }
    },
  };
}
const LICENSE_BANNER = `/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */`;

export default defineConfig({
  base: '/webapp/',
  plugins: [react(), licenseHeaderPlugin()],
  build: {
    outDir: '../src/main/resources/static/webapp',
    emptyOutDir: true,
    sourcemap: false,
  },
  server: {
    port: 5177,
    proxy: {
      '/api': {
        // 本地联调可用 WEBAPP_API_TARGET 指到其他 broker 实例（默认 2024 用户本地端口）
        target: process.env.WEBAPP_API_TARGET ?? 'http://127.0.0.1:2024',
        changeOrigin: true,
      },
    },
  },
});
