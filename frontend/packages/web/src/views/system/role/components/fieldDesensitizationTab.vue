<template>
  <div class="relative flex h-full flex-col">
    <n-scrollbar
      x-scrollable
      class="flex-1 px-[24px] pb-[24px]"
      :content-style="{ 'min-width': '600px', 'width': '100%' }"
    >
      <div class="group-title">{{ t('role.fieldDesensitization') }}</div>
      <div class="mb-[16px] text-[12px] text-[var(--text-n5)]">{{ t('role.fieldDesensitizationTip') }}</div>

      <!-- 模块选择 -->
      <div class="mb-[16px] flex items-center gap-[8px]">
        <span class="text-[14px] text-[var(--text-n7)]">{{ t('role.selectModule') }}</span>
        <n-select
          v-model:value="currentModule"
          :options="moduleOptions"
          style="width: 200px"
          @update-value="handleModuleChange"
        />
      </div>

      <!-- 字段列表 -->
      <n-data-table :columns="columns" :data="fieldList" :loading="loading" :row-key="rowKey" :bordered="false" />

      <n-empty v-if="!loading && fieldList.length === 0" :description="t('role.noFields')" class="mt-[40px]" />
    </n-scrollbar>

    <!-- 底部保存按钮 -->
    <div class="flex justify-end border-t border-[var(--divider-n8)] px-[24px] py-[12px]">
      <n-button type="primary" :loading="saving" :disabled="isDisabled" @click="handleSave">
        {{ t('common.button.save') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, ref, watch } from 'vue';
  import { NButton, NCheckbox, NDataTable, NEmpty, NScrollbar, NSelect, useMessage } from 'naive-ui';

  import type { FieldMaskConfigItem, FieldMaskFieldItem } from '@lib/shared/api/modules/system/fieldMask';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { getFieldMaskConfig, getFieldMaskFields, saveFieldMaskConfig } from '@/api/modules';

  interface FieldItem extends FieldMaskFieldItem {
    checked: boolean;
  }

  const props = defineProps<{
    activeRoleId: string;
    isNew?: boolean;
  }>();

  const { t } = useI18n();
  const message = useMessage();

  const currentModule = ref('customer');
  const fieldList = ref<FieldItem[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  // 已保存的配置(所有模块)
  const savedConfigs = ref<FieldMaskConfigItem[]>([]);

  const isDisabled = computed(() => props.isNew);

  const moduleOptions = [
    { label: t('role.moduleCustomer'), value: 'customer' },
    { label: t('role.moduleOpportunity'), value: 'opportunity' },
    { label: t('role.moduleOrder'), value: 'order' },
    { label: t('role.moduleContact'), value: 'contact' },
  ];

  function rowKey(row: FieldItem) {
    return row.id;
  }

  // 联系人模块的 phone 是实体固定列，不在表单字段里，这里手动加入
  const contactBuiltinFields: FieldMaskFieldItem[] = [
    { id: 'phone_builtin', name: '手机号(phone)', type: 'PHONE', internalKey: 'phone' },
  ];

  const columns = computed(() => [
    {
      title: t('role.fieldName'),
      key: 'name',
      width: 200,
    },
    {
      title: t('role.fieldType'),
      key: 'type',
      width: 150,
    },
    {
      title: t('role.maskFields'),
      key: 'checked',
      width: 100,
      render: (row: FieldItem) =>
        h(NCheckbox, {
          'checked': row.checked,
          'onUpdate:checked': (val: boolean) => {
            row.checked = val;
          },
        }),
    },
  ]);

  // 加载模块字段列表
  async function loadFields() {
    loading.value = true;
    try {
      let fields = await getFieldMaskFields(currentModule.value);
      // 联系人模块补充 phone 固定列
      if (currentModule.value === 'contact') {
        fields = [...contactBuiltinFields, ...fields];
      }
      // 标记已勾选的(从 savedConfigs 中匹配当前模块)
      const moduleConfigs = savedConfigs.value.filter((c) => c.moduleKey === currentModule.value);
      const checkedIds = new Set<string>();
      const checkedKeys = new Set<string>();
      moduleConfigs.forEach((c) => {
        if (c.fieldId) checkedIds.add(c.fieldId);
        if (c.fieldKey) checkedKeys.add(c.fieldKey);
      });
      fieldList.value = fields.map((f) => ({
        ...f,
        checked: f.id === 'phone_builtin' ? checkedKeys.has('phone') : checkedIds.has(f.id),
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  // 加载角色已保存的脱敏配置
  async function loadConfig() {
    if (!props.activeRoleId || props.isNew) {
      savedConfigs.value = [];
      return;
    }
    try {
      savedConfigs.value = await getFieldMaskConfig(props.activeRoleId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      savedConfigs.value = [];
    }
  }

  function handleModuleChange() {
    loadFields();
  }

  // 保存:收集所有模块的勾选状态(不只是当前模块)
  async function handleSave() {
    saving.value = true;
    try {
      // 先把当前模块的勾选同步到 savedConfigs
      const currentChecked: FieldMaskConfigItem[] = [];
      fieldList.value.forEach((f) => {
        if (f.checked) {
          if (f.id === 'phone_builtin') {
            // 固定列:用 fieldKey
            currentChecked.push({
              moduleKey: currentModule.value,
              fieldKey: f.internalKey,
              fieldType: f.type,
            });
          } else {
            currentChecked.push({
              moduleKey: currentModule.value,
              fieldId: f.id,
              fieldType: f.type,
            });
          }
        }
      });
      // 合并:排除当前模块，加上当前模块的最新勾选
      const otherConfigs = savedConfigs.value.filter((c) => c.moduleKey !== currentModule.value);
      const allConfigs = [...otherConfigs, ...currentChecked];

      await saveFieldMaskConfig({ roleId: props.activeRoleId, masks: allConfigs });
      savedConfigs.value = allConfigs;
      message.success(t('role.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      saving.value = false;
    }
  }

  watch(
    () => props.activeRoleId,
    async () => {
      await loadConfig();
      await loadFields();
    },
    { immediate: true }
  );
</script>

<style scoped>
  .group-title {
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-n7);
  }
</style>
