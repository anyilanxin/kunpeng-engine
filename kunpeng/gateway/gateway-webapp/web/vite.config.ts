import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 产物直接落模块 static/webapp（进程内静态挂载 /webapp/**）；hash 路由无服务端 fallback 需要
export default defineConfig({
  base: '/webapp/',
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static/webapp',
    emptyOutDir: true,
    sourcemap: false,
  },
  server: {
    port: 5177,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:2024',
        changeOrigin: true,
      },
    },
  },
});
