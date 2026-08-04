import { clearToken, hasToken, isLoginExpires } from '@lib/shared/method/auth';

import useUser from '@/hooks/useUser';
import useUserStore from '@/store/modules/user';
import { getFirstRouteNameByPermission } from '@/utils/permission';

import NProgress from 'nprogress';
import type { LocationQueryRaw, Router } from 'vue-router';

export default function setupUserLoginInfoGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    NProgress.start();

    const { isWhiteListPage } = useUser();
    // 登录过期清除token
    if (isLoginExpires()) {
      clearToken();
    }

    const tokenExists = hasToken();

    // 未登录访问受限页面重定向登录页
    if (!tokenExists && to.name !== 'login' && !isWhiteListPage()) {
      next({
        name: 'login',
        query: {
          redirect: to.name,
          ...to.query,
        } as LocationQueryRaw,
      });
      NProgress.done();
      return;
    }

    // 已登录访问 login重定向（有权限第一个页面）
    if (to.name === 'login' && tokenExists) {
      const firstRoute = getFirstRouteNameByPermission(router.getRoutes());
      next({ name: firstRoute });
      NProgress.done();
      return;
    }

    // 已登录但用户信息(含 permissionIds) 尚未加载时, 先拉取再放行,
    // 避免后续 permission 守卫拿到空的 permissionIds 误判无权限 (刷新场景必现)
    const userStore = useUserStore();
    if (tokenExists && userStore.userInfo.permissionIds.length === 0) {
      try {
        await userStore.isLogin(true);
      } catch (e) {
        // eslint-disable-next-line no-console
        console.log('[userLoginInfoGuard] isLogin failed:', e);
      }
    }

    // 其他情况（放行：已登录访问正常页面\未登录访问白名单页面）
    next();
    NProgress.done();
  });
}
