import { ClueRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const lead: AppRouteRecordRaw = {
  path: '/lead',
  name: ClueRouteEnum.CLUE_MANAGEMENT,
  redirect: '/lead/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'module.clueManagement',
    permissions: ['CLUE_MANAGEMENT:READ'],
    icon: 'iconicon_clue',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.clue',
  },
  children: [
    {
      path: 'index',
      name: ClueRouteEnum.CLUE_MANAGEMENT_CLUE,
      component: () => import('@/views/clueManagement/clue/index.vue'),
      meta: {
        locale: 'menu.clue',
        isTopMenu: true,
        permissions: ['CLUE_MANAGEMENT:READ'],
      },
    },
  ],
};

export default lead;
