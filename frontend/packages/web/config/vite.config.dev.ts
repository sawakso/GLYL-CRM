import baseConfig from './vite.config.base';
import { config } from 'dotenv';
import { mergeConfig } from 'vite';
import eslint from 'vite-plugin-eslint';

// 注入本地/开发配置环境变量(先导入的配置优先级高)
config({ path: ['.env.development.local', '.env.development'] });

const allowedHosts = (process.env.VITE_ALLOWED_HOSTS ?? '')
  .split(',')
  .map((host) => host.trim())
  .filter(Boolean);

export default mergeConfig(
  {
    mode: 'development',
    server: {
      host: true,
      port: 5174,
      strictPort: true,
      allowedHosts,
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
        '/attachment': {
          target: process.env.VITE_DEV_DOMAIN,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/front\/attachment/, ''),
        },
        '/ui': {
          target: process.env.VITE_DEV_DOMAIN,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/front\/ui/, ''),
        },
      },
    },
    plugins: [
      // 开发时禁用 eslint 实时检查以提升速度,提交前请手动执行 pnpm lint
      // eslint({
      //   overrideConfigFile: 'eslint.config.cjs',
      //   cache: false,
      //   include: ['src/**/*.ts', 'src/**/*.tsx', 'src/**/*.vue'],
      //   exclude: ['node_modules'],
      // }),
    ],
  },
  baseConfig
);
