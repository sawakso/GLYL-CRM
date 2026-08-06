import baseConfig from './vite.config.base';
import { config } from 'dotenv';
import { mergeConfig } from 'vite';
import eslint from 'vite-plugin-eslint';

// 注入本地/开发配置环境变量(先导入的配置优先级高)
config({ path: ['.env.development.local', '.env.development'] });

// Host 白名单：由环境变量 VITE_ALLOWED_HOSTS 控制（逗号分隔，支持 .域 子域名通配；填 * 表示允许所有）
// - 本机开发：留空，回退到 ['localhost', '127.0.0.1']
// - 公网穿透/局域网：在 .env.development.local 里配置，如 VITE_ALLOWED_HOSTS='crm.sawakso.com,.sawakso.com'
const allowedHosts = (process.env.VITE_ALLOWED_HOSTS ?? '')
  .split(',')
  .map((host) => host.trim())
  .filter(Boolean);

let serverAllowedHosts: string[] | true = ['localhost', '127.0.0.1'];
if (allowedHosts.length > 0) {
  serverAllowedHosts = allowedHosts.includes('*') ? true : allowedHosts;
}

export default mergeConfig(
  {
    mode: 'development',
    server: {
      host: true,
      port: 5174,
      strictPort: true,
      allowedHosts: serverAllowedHosts,
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
