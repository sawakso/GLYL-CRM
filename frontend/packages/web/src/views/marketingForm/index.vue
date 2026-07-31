<template>
  <CrmCard no-content-padding hide-footer>
    <div class="h-full p-[24px]">
      <div class="mb-[16px] flex items-center justify-between">
        <n-button v-permission="['MARKETING_FORM:ADD']" type="primary" @click="handleAdd">
          {{ t('marketingForm.createForm') }}
        </n-button>
        <n-input v-model:value="keyword" :placeholder="t('common.search')" clearable class="w-[240px]" />
      </div>
      <n-data-table
        :columns="columns"
        :data="filteredList"
        :loading="loading"
        :bordered="false"
        :row-key="(row: MarketingFormListItem) => row.id"
        striped
      />
    </div>

    <ConfigDrawer
      v-model:visible="configDrawerVisible"
      :source-id="currentSourceId"
      :default-tab="defaultTab"
      @saved="handleFormSaved"
    />

    <QrCodeModal v-model:visible="qrVisible" :form-item="currentQrItem" />
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NDataTable, NInput, NTag, useMessage } from 'naive-ui';

  import type { MarketingFormListItem } from '@lib/shared/api/modules/marketingForm';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import ConfigDrawer from './components/configDrawer/index.vue';
  import QrCodeModal from './components/qrCodeModal/index.vue';

  import { deleteMarketingForm, getMarketingFormList, updateMarketingFormStatus } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  import type { DataTableColumns } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const loading = ref(false);
  const formList = ref<MarketingFormListItem[]>([]);
  const keyword = ref('');

  const filteredList = computed(() => {
    if (!keyword.value) return formList.value;
    const kw = keyword.value.toLowerCase();
    return formList.value.filter(
      (item) =>
        item.name?.toLowerCase().includes(kw) ||
        item.description?.toLowerCase().includes(kw) ||
        item.targetPoolName?.toLowerCase().includes(kw)
    );
  });

  async function loadList() {
    try {
      loading.value = true;
      formList.value = (await getMarketingFormList()) || [];
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  // Config drawer
  const configDrawerVisible = ref(false);
  const currentSourceId = ref<string | undefined>(undefined);
  const defaultTab = ref<'design' | 'settings'>('design');

  function handleAdd() {
    currentSourceId.value = undefined;
    defaultTab.value = 'design';
    configDrawerVisible.value = true;
  }

  function handleEdit(row: MarketingFormListItem) {
    currentSourceId.value = row.id;
    defaultTab.value = 'design';
    configDrawerVisible.value = true;
  }

  function handleSettings(row: MarketingFormListItem) {
    currentSourceId.value = row.id;
    defaultTab.value = 'settings';
    configDrawerVisible.value = true;
  }

  function handleFormSaved() {
    loadList();
  }

  // QR code modal
  const qrVisible = ref(false);
  const currentQrItem = ref<MarketingFormListItem | null>(null);

  function handleShowQr(row: MarketingFormListItem) {
    currentQrItem.value = row;
    qrVisible.value = true;
  }

  // Status toggle: DRAFT/CLOSED -> ACTIVE (启用), ACTIVE -> CLOSED (关闭)
  function handleToggleStatus(row: MarketingFormListItem) {
    if (row.status === 'ACTIVE') {
      // 关闭
      openModal({
        type: 'warning',
        title: t('common.tip'),
        content: t('marketingForm.confirmClose'),
        positiveText: t('common.confirm'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            await updateMarketingFormStatus(row.id, 'CLOSED');
            Message.success(t('common.operationSuccess'));
            loadList();
          } catch (error) {
            console.log(error);
          }
        },
      });
    } else {
      // 启用 (DRAFT/CLOSED -> ACTIVE)
      updateMarketingFormStatus(row.id, 'ACTIVE').then(() => {
        Message.success(t('common.operationSuccess'));
        loadList();
      });
    }
  }

  function handleDelete(row: MarketingFormListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: t('marketingForm.confirmDelete'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      positiveButtonProps: { type: 'error', size: 'medium' },
      onPositiveClick: async () => {
        try {
          await deleteMarketingForm(row.id);
          Message.success(t('common.deleteSuccess'));
          loadList();
        } catch (error) {
          console.log(error);
        }
      },
    });
  }

  // 根据表单状态生成操作菜单 (DRAFT/CLOSED → 可启用, ACTIVE → 可关闭)
  function buildGroupList(row: MarketingFormListItem): ActionsItem[] {
    const items: ActionsItem[] = [
      { label: t('common.edit'), key: 'edit', permission: ['MARKETING_FORM:UPDATE'] },
      { label: t('marketingForm.settings'), key: 'settings', permission: ['MARKETING_FORM:UPDATE'] },
      { label: t('marketingForm.qrCode'), key: 'qr', permission: ['MARKETING_FORM:READ'] },
      { label: '', key: '', type: 'divider' },
    ];
    if (row.status === 'ACTIVE') {
      items.push({ label: t('marketingForm.close'), key: 'close', permission: ['MARKETING_FORM:UPDATE'] });
    } else {
      items.push({ label: t('marketingForm.activate'), key: 'activate', permission: ['MARKETING_FORM:UPDATE'] });
    }
    items.push({ label: t('common.delete'), key: 'delete', permission: ['MARKETING_FORM:DELETE'], danger: true });
    return items;
  }

  function handleActionSelect(row: MarketingFormListItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row);
        break;
      case 'settings':
        handleSettings(row);
        break;
      case 'qr':
        handleShowQr(row);
        break;
      case 'activate':
      case 'close':
        handleToggleStatus(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  function statusTagType(status?: string) {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'CLOSED':
        return 'default';
      default:
        return 'warning';
    }
  }

  function statusLabel(status?: string) {
    switch (status) {
      case 'ACTIVE':
        return t('marketingForm.active');
      case 'CLOSED':
        return t('marketingForm.closed');
      default:
        return t('marketingForm.draft');
    }
  }

  function formatTimestamp(ts: number): string {
    const d = new Date(ts);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
      d.getMinutes()
    )}`;
  }

  const columns: DataTableColumns<MarketingFormListItem> = [
    {
      title: t('marketingForm.name'),
      key: 'name',
      width: 220,
      ellipsis: { tooltip: true },
    },
    {
      title: t('marketingForm.description'),
      key: 'description',
      width: 200,
      ellipsis: { tooltip: true },
      render(row) {
        return row.description || '-';
      },
    },
    {
      title: t('marketingForm.targetPool'),
      key: 'targetPoolName',
      width: 160,
      ellipsis: { tooltip: true },
      render(row) {
        return row.targetPoolName || '-';
      },
    },
    {
      title: t('marketingForm.status'),
      key: 'status',
      width: 100,
      render(row) {
        return h(
          NTag,
          { size: 'small', type: statusTagType(row.status), bordered: false },
          { default: () => statusLabel(row.status) }
        );
      },
    },
    {
      title: t('marketingForm.submissionCount'),
      key: 'submissionCount',
      width: 100,
      align: 'center',
      render(row) {
        return String(row.submissionCount ?? 0);
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 170,
      render(row) {
        return row.createTime ? formatTimestamp(row.createTime) : '-';
      },
    },
    {
      title: t('common.operation'),
      key: 'actions',
      width: 80,
      fixed: 'right',
      render(row) {
        if (!hasAnyPermission(['MARKETING_FORM:UPDATE', 'MARKETING_FORM:DELETE', 'MARKETING_FORM:READ'])) {
          return null;
        }
        return h(CrmOperationButton, {
          groupList: buildGroupList(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        });
      },
    },
  ];

  onBeforeMount(() => {
    loadList();
  });
</script>

<style scoped></style>
