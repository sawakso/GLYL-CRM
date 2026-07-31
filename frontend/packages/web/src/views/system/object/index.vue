<template>
  <CrmCard no-content-padding hide-footer>
    <div class="h-full p-[24px]">
      <div class="mb-[16px] flex items-center justify-between">
        <div class="text-[16px] font-medium text-[var(--text-n1)]">{{ t('objectSetting.title') }}</div>
        <n-input
          v-model:value="keyword"
          :placeholder="t('objectSetting.searchPlaceholder')"
          clearable
          class="w-[240px]"
          @input="handleSearch"
        />
      </div>
      <n-data-table
        :columns="columns"
        :data="filteredList"
        :loading="loading"
        :bordered="false"
        :row-key="(row: ObjectConfigItem) => row.key"
        striped
      />
    </div>
  </CrmCard>
  <!-- 重命名弹窗 -->
  <n-modal
    v-model:show="renameVisible"
    preset="dialog"
    :title="t('objectSetting.rename')"
    :positive-text="t('common.confirm')"
    :negative-text="t('common.cancel')"
    @positive-click="handleRenameConfirm"
  >
    <n-form>
      <n-form-item :label="t('objectSetting.objectName')">
        <n-input v-model:value="renameValue" :placeholder="t('objectSetting.renamePlaceholder')" />
      </n-form-item>
    </n-form>
  </n-modal>
</template>

<script setup lang="ts">
  import { NDataTable, NForm, NFormItem, NInput, NModal, NSwitch, NTag, useMessage } from 'naive-ui';

  import { type ObjectConfigItem } from '@lib/shared/api/modules/system/objectConfig';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import { getObjectConfigList, renameObjectConfig, switchObjectConfig } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  import type { DataTableColumns } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();

  const loading = ref(false);
  const objectList = ref<ObjectConfigItem[]>([]);
  const keyword = ref('');

  const filteredList = computed(() => {
    if (!keyword.value) return objectList.value;
    const kw = keyword.value.toLowerCase();
    return objectList.value.filter(
      (item) =>
        item.name.toLowerCase().includes(kw) ||
        item.defaultName.toLowerCase().includes(kw) ||
        item.key.toLowerCase().includes(kw)
    );
  });

  async function loadList() {
    try {
      loading.value = true;
      objectList.value = await getObjectConfigList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    // filteredList is computed, auto-updates
  }

  // 重命名
  const renameVisible = ref(false);
  const renameValue = ref('');
  const currentRenameItem = ref<ObjectConfigItem | null>(null);

  function handleRename(item: ObjectConfigItem) {
    currentRenameItem.value = item;
    renameValue.value = item.name;
    renameVisible.value = true;
  }

  async function handleRenameConfirm() {
    if (!currentRenameItem.value) return false;
    try {
      await renameObjectConfig({
        key: currentRenameItem.value.key,
        name: renameValue.value || undefined,
      });
      Message.success(t('common.operationSuccess'));
      await loadList();
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      return false;
    }
  }

  // 启用/禁用
  async function handleSwitch(item: ObjectConfigItem) {
    try {
      await switchObjectConfig(item.key);
      Message.success(t('common.operationSuccess'));
      await loadList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  // 表格列定义
  const columns: DataTableColumns<ObjectConfigItem> = [
    {
      title: t('objectSetting.objectName'),
      key: 'name',
      width: 200,
      render(row) {
        return h('div', { class: 'flex items-center gap-[8px]' }, [
          h('span', {}, row.name),
          row.name !== row.defaultName
            ? h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => t('objectSetting.renamed') })
            : null,
        ]);
      },
    },
    {
      title: t('objectSetting.defaultName'),
      key: 'defaultName',
      width: 150,
    },
    {
      title: t('objectSetting.objectKey'),
      key: 'key',
      width: 180,
    },
    {
      title: t('objectSetting.objectType'),
      key: 'type',
      width: 120,
      render(row) {
        return h(
          NTag,
          { size: 'small', type: row.type === 'PRESET' ? 'default' : 'success', bordered: false },
          { default: () => (row.type === 'PRESET' ? t('objectSetting.preset') : t('objectSetting.custom')) }
        );
      },
    },
    {
      title: t('objectSetting.status'),
      key: 'enable',
      width: 120,
      render(row) {
        return h(NSwitch, {
          value: row.enable,
          size: 'small',
          rubberBand: false,
          disabled: !hasAnyPermission(['MODULE_SETTING:UPDATE']),
          onUpdateValue: () => handleSwitch(row),
        });
      },
    },
    {
      title: t('common.operation'),
      key: 'actions',
      width: 120,
      render(row) {
        if (!hasAnyPermission(['MODULE_SETTING:UPDATE'])) return null;
        return h(
          'a',
          {
            class: 'text-[var(--primary-8)] cursor-pointer',
            onClick: () => handleRename(row),
          },
          t('objectSetting.rename')
        );
      },
    },
  ];

  onBeforeMount(() => {
    loadList();
  });
</script>

<style scoped></style>
