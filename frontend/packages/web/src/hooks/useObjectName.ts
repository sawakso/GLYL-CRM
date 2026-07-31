import { ref } from 'vue';

import { getObjectNameMap } from '@/api/modules';

const objectNameMap = ref<Record<string, string>>({});
let initialized = false;
let initPromise: Promise<void> | null = null;

/**
 * 路由名称 → FormKey 映射
 * 用于导航菜单显示动态对象名称
 */
export const routeNameToFormKey: Record<string, string> = {
  leadManagementLead: 'clue',
  accountIndex: 'customer',
  accountContact: 'contact',
  opportunityOpt: 'opportunity',
  opportunityQuotation: 'quotation',
  contractIndex: 'contract',
  contractInvoice: 'invoice',
  contractPaymentPlan: 'contractPaymentPlan',
  contractPaymentRecord: 'contractPaymentRecord',
  productPro: 'product',
  productPrice: 'price',
  orderIndex: 'order',
};

/**
 * 获取动态对象名称(支持自定义重命名)
 * 首次调用时会自动从后端加载所有对象名称映射并缓存
 */
export default function useObjectName() {
  /**
   * 初始化对象名称映射(仅执行一次)
   */
  async function initObjectNameMap() {
    if (initialized) return;
    if (initPromise) return initPromise;

    initPromise = (async () => {
      try {
        const res = await getObjectNameMap();
        objectNameMap.value = res || ({} as Record<string, string>);
        initialized = true;
      } catch (e) {
        // 加载失败不阻塞应用，下次调用会重试
        initPromise = null;
      }
    })();
    return initPromise;
  }

  /**
   * 根据 FormKey 获取对象显示名称
   * 如果用户自定义了名称则返回自定义名称，否则返回默认名称
   * @param formKey 对象标识(如 "clue", "customer" 等)
   * @param fallback 兜底名称(当映射中找不到时使用)
   */
  function getObjectName(formKey: string, fallback?: string): string {
    return objectNameMap.value[formKey] || fallback || formKey;
  }

  /**
   * 根据路由名称获取动态显示名称
   * @param routeName 路由名称(如 "leadManagementLead")
   * @param fallback i18n 兜底名称
   */
  function getObjectNameByRoute(routeName: string, fallback?: string): string {
    const formKey = routeNameToFormKey[routeName];
    if (formKey && objectNameMap.value[formKey]) {
      return objectNameMap.value[formKey];
    }
    return fallback || routeName;
  }

  /**
   * 强制刷新对象名称映射(重命名后调用)
   */
  async function refreshObjectNameMap() {
    initialized = false;
    initPromise = null;
    await initObjectNameMap();
  }

  return {
    objectNameMap,
    initObjectNameMap,
    getObjectName,
    getObjectNameByRoute,
    refreshObjectNameMap,
  };
}
