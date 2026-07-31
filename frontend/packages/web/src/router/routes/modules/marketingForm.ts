import { MarketingFormRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const marketingForm: AppRouteRecordRaw = {
  path: '/marketingForm',
  name: MarketingFormRouteEnum.MARKETING_FORM,
  redirect: '/marketingForm/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.marketingForm',
    permissions: ['MARKETING_FORM:READ'],
    icon: 'iconicon_clue',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.marketingForm',
  },
  children: [
    {
      path: 'index',
      name: MarketingFormRouteEnum.MARKETING_FORM_INDEX,
      component: () => import('@/views/marketingForm/index.vue'),
      meta: {
        locale: 'menu.marketingForm',
        isTopMenu: true,
        permissions: ['MARKETING_FORM:READ'],
      },
    },
  ],
};

export default marketingForm;
