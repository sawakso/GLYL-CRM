import baseConfig from './vite.config.base';
import { config } from 'dotenv';
import { mergeConfig } from 'vite';

// 注入本地/开发配置环境变量(先导入的配置优先级高)
config({ path: ['.env.development.local', '.env.development'] });

export default mergeConfig(
  {
    mode: 'development',
    server: {
      port: 3000,
      open: true,
      fs: {
        strict: true,
      },
      proxy: {
        '/sse': {
          target: process.env.VITE_DEV_DOMAIN,
          changeOrigin: true,
        },
        '/front': {
          target: process.env.VITE_DEV_DOMAIN,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/front/, ''),
        },
        '/pic': {
          target: process.env.VITE_DEV_DOMAIN,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/front\/pic/, ''),
        },
      },
    },
    plugins: [
      // 开发时禁用 eslint 实时检查以提升速度,提交前请手动执行 pnpm lint
      // eslint({
      //   cache: false,
      //   include: ['src/**/*.ts', 'src/**/*.tsx', 'src/**/*.vue'],
      //   exclude: ['node_modules'],
      // }),
    ],
  },
  baseConfig
);
