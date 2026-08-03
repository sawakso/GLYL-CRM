import usePermission from '@/hooks/usePermission';
import useAppStore from '@/store/modules/app';

import { featureRouteMap, NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '../constants';
import NProgress from 'nprogress';
import type { Router } from 'vue-router';

export default function setupPermissionGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const Permission = usePermission();
    const appStore = useAppStore();

    // 模块配置可能来自 localStorage 旧数据, 若未初始化(为空)则先从后端刷新, 避免用过期配置误判无资源权限
    if (!appStore.moduleConfigList || appStore.moduleConfigList.length === 0) {
      try {
        await appStore.initModuleConfig();
      } catch (e) {
        // 忽略刷新失败, 继续走默认判断
      }
    }

    const permissionsAllow = Permission.accessRouter(to);

    const currentMenuConfig: string[] = appStore.moduleConfigList.filter((e) => e.enable).map((e) => e.moduleKey);
    const moduleId = Object.keys(featureRouteMap).find((key) => (to.name as string)?.includes(key));

    // 调试日志
    console.log('[PermissionGuard] to.name=', to.name,
      '| moduleId=', moduleId,
      '| moduleEnabledInConfig=', moduleId ? currentMenuConfig.includes(featureRouteMap[moduleId]) : 'N/A',
      '| permissionsAllow=', permissionsAllow,
      '| moduleConfigLen=', appStore.moduleConfigList.length);

    if (moduleId && featureRouteMap[moduleId] && !currentMenuConfig.includes(featureRouteMap[moduleId])) {
      console.log('[PermissionGuard] >>> BLOCKED by module config (no-resource)');
      next({ name: NO_RESOURCE_ROUTE_NAME });
      NProgress.done();
      return;
    }

    const exist = WHITE_LIST.find((el) => el.name === to.name);
    if (exist || permissionsAllow) {
      next();
    } else {
      console.log('[PermissionGuard] >>> BLOCKED by permission (no-resource)');
      next({ name: NO_RESOURCE_ROUTE_NAME });
    }
    NProgress.done();
  });
}
