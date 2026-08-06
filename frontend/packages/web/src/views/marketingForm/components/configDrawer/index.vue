<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    width="100%"
    :loading="loading"
    :title="formName"
    :tab-list="tabList"
    :readonly="activeTab === 'settings'"
    :before-change-tab="handleBeforeChangeTab"
    @cancel="handleBack"
  >
    <template #title>
      <div class="process-name-header flex max-w-full flex-1 overflow-hidden">
        <CrmEditableText
          :value="formName"
          :permission="[]"
          click-to-edit
          :emptyTextTip="t('common.notNull', { value: t('marketingForm.name') })"
          @input="formNameDraft = $event"
          @handle-edit="handleEditTitle"
        >
          <n-tooltip trigger="hover" :delay="300" :disabled="!formName">
            <template #trigger>
              <div class="process-name one-line-text">
                {{ formName || '-' }}
              </div>
            </template>
            {{ formName || '-' }}
          </n-tooltip>
        </CrmEditableText>
      </div>
    </template>

    <template #headerActions>
      <n-button v-if="currentSourceId" secondary :loading="loading" @click="handlePreview">
        {{ t('marketingForm.preview') }}
      </n-button>
      <n-tooltip v-if="activeTab === 'design'" trigger="hover" :disabled="formEnabled">
        <template #trigger>
          <span>
            <n-button type="primary" :loading="loading" :disabled="!formEnabled" @click="handleSaveFormDesign">
              {{ t('common.save') }}
            </n-button>
          </span>
        </template>
        {{ t('customForm.formDisabled') }}
      </n-tooltip>
      <n-button v-if="activeTab === 'settings'" type="primary" :loading="loading" @click="handleSaveSettings">
        {{ t('common.save') }}
      </n-button>
    </template>

    <div v-show="activeTab === 'design'" class="h-full">
      <CrmFormDesign
        v-if="visible"
        ref="formDesignRef"
        v-model:form-config="formConfig"
        v-model:field-list="fieldList"
        class="custom-form-design"
        :form-key="formKey"
      />
    </div>

    <div v-show="activeTab === 'settings'" class="h-full overflow-auto p-[24px]">
      <n-form ref="settingsFormRef" :model="settingsForm" label-placement="top" class="max-w-[640px]">
        <n-form-item :label="t('marketingForm.description')" path="description">
          <n-input
            v-model:value="settingsForm.description"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            :placeholder="t('marketingForm.description')"
          />
        </n-form-item>
        <n-form-item :label="t('marketingForm.targetPool')" path="targetPoolId" :rule="targetPoolRule">
          <n-select
            v-model:value="settingsForm.targetPoolId"
            :options="poolOptionList"
            :placeholder="t('marketingForm.selectTargetPool')"
            filterable
          />
        </n-form-item>
        <n-form-item :label="t('marketingForm.dedupStrategy')" path="dedupStrategy">
          <n-select v-model:value="settingsForm.dedupStrategy" :options="dedupStrategyOptions" />
        </n-form-item>
        <n-form-item
          v-if="settingsForm.dedupStrategy !== 'INHERIT'"
          :label="t('marketingForm.dedupWindow')"
          path="dedupWindow"
        >
          <div class="flex w-full items-center gap-[8px]">
            <n-input-number
              v-model:value="settingsForm.dedupWindow"
              class="w-[160px]"
              :min="0"
              :max="10080"
              :precision="0"
              :placeholder="t('common.pleaseInput')"
            />
            <span class="text-[12px] text-[var(--text-n5)]">{{ t('marketingForm.dedupWindowTip') }}</span>
          </div>
        </n-form-item>
        <n-form-item
          v-if="settingsForm.dedupStrategy !== 'INHERIT'"
          :label="t('marketingForm.dedupKey')"
          path="dedupKey"
        >
          <n-select v-model:value="settingsForm.dedupKey" :options="dedupKeyOptions" />
        </n-form-item>
        <n-form-item :label="t('marketingForm.requireName')" path="requireName">
          <n-switch v-model:value="settingsForm.requireName" />
          <template #feedback>{{ t('marketingForm.requireNameTip') }}</template>
        </n-form-item>

        <n-divider>{{ t('marketingForm.fieldMapping') }}</n-divider>
        <n-data-table
          :columns="mappingColumns"
          :data="mappingRows"
          :bordered="false"
          :single-line="false"
          size="small"
        />
      </n-form>
    </div>
  </CrmProcessDrawer>

  <PreviewModal v-model:visible="previewVisible" :form-id="currentSourceId" :form-name="formName" />
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import {
    type DataTableColumns,
    type FormInst,
    type FormRules,
    NButton,
    NDataTable,
    NDivider,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NSelect,
    NSwitch,
    NTooltip,
    useMessage,
  } from 'naive-ui';

  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import type { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import { dividerDefaultFieldConfig, inputDefaultFieldConfig } from '@/components/business/crm-form-create/config';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import {
    createDefaultFormConfig,
    useFormDesignConfig,
  } from '@/components/business/crm-form-design-drawer/useFormDesignConfig';
  import CrmProcessDrawer from '@/components/business/crm-process-drawer/index.vue';
  import PreviewModal from '../previewModal/index.vue';

  import {
    addMarketingForm,
    getClueFormConfig,
    getMarketingFormDetail,
    getPoolOptions,
    updateMarketingForm,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

  // 线索对象姓名字段ID(必填, 不可删除) - 市场表单新建默认引用它
  const NAME_FIELD_REF_ID = '432350589340127233';

  interface MappingRow {
    fieldId: string;
    fieldName: string;
    fieldLabel: string;
    mappedTo: string;
  }

  const props = defineProps<{
    sourceId?: string;
    defaultTab?: 'design' | 'settings';
  }>();

  const emit = defineEmits<{
    (e: 'saved', id: string): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const userStore = useUserStore();
  const isAdmin = computed(() => userStore.isAdmin);

  const activeTab = ref<'design' | 'settings'>('design');
  // 市场设置仅管理员可见/可用；非 admin 只保留「表单设计」
  const tabList = computed(() => {
    const tabs = [{ name: 'design', tab: t('marketingForm.formDesign') }];
    if (isAdmin.value) {
      tabs.push({ name: 'settings', tab: t('marketingForm.settings') });
    }
    return tabs;
  });

  const titleSaving = ref(false);
  const currentSourceId = ref('');
  const formName = ref('');
  const formNameDraft = ref('');
  const formEnabled = ref(true);
  const previewVisible = ref(false);

  function handlePreview() {
    if (!currentSourceId.value) {
      Message.warning(t('marketingForm.saveFormDesignFirst'));
      return;
    }
    previewVisible.value = true;
  }
  const settingsFormRef = ref<FormInst | null>(null);
  const poolOptions = ref<CluePoolItem[]>([]);
  // 转换为 n-select 期望的 { label, value } 结构，避免 name/id 字段映射不生效
  const poolOptionList = computed(() => (poolOptions.value || []).map((p) => ({ label: p.name, value: p.id })));

  const settingsForm = ref({
    description: '',
    targetPoolId: '' as string,
    dedupStrategy: 'INHERIT',
    dedupWindow: null as number | null,
    dedupKey: '' as string,
    requireName: false,
  });

  // fieldMapping stored as JSON string: { [fieldId]: 'clueFieldName' }
  const fieldMapping = ref<Record<string, string>>({});

  const targetPoolRule = {
    required: true,
    message: t('marketingForm.selectTargetPool'),
    trigger: ['change', 'blur'],
  };

  const dedupStrategyOptions = computed(() => [
    { label: t('marketingForm.strategy.inherit'), value: 'INHERIT' },
    { label: t('marketingForm.strategy.none'), value: 'NONE' },
    { label: t('marketingForm.strategy.update'), value: 'UPDATE' },
    { label: t('marketingForm.strategy.skip'), value: 'SKIP' },
    { label: t('marketingForm.strategy.mark'), value: 'MARK' },
  ]);

  const dedupKeyOptions = computed(() => [
    { label: t('marketingForm.dedupKeyAuto'), value: 'AUTO' },
    { label: t('marketingForm.dedupKeyPhone'), value: 'PHONE' },
    { label: t('marketingForm.dedupKeyDevice'), value: 'DEVICE' },
    { label: t('marketingForm.dedupKeyIp'), value: 'IP' },
  ]);

  // 线索模块全量字段列表 (供字段映射下拉选项使用, 包含全部 60+ 字段)
  const clueFieldList = ref<{ id: string; name: string; businessKey?: string; internalKey?: string }[]>([]);
  // 组件初始化时异步加载 (无需等 initMarketingFormConfig)
  getClueFormConfig()
    .then((res: any) => {
      clueFieldList.value = (res?.fields || []).map((f: any) => ({
        id: f.id,
        name: f.name || '',
        businessKey: f.businessKey,
        internalKey: f.internalKey || f.businessKey, // 业务key缺时回退internalKey供applyClueField兼容
      }));
    })
    .catch(() => {
      clueFieldList.value = [];
    });

  // 辅助: 从线索字段列表按 id/businessKey 查找匹配键值 (优先 businessKey→internalKey→id)
  function matchClueFieldKey(fieldId: string): string {
    const cf = clueFieldList.value.find((c) => c.id === fieldId);
    return cf ? cf.businessKey || cf.internalKey || cf.id : '';
  }

  // 可映射的 Clue 字段名 (动态加载线索模块全量字段)
  const clueFieldOptions = computed(() => [
    { label: t('marketingForm.noMapping'), value: '' },
    ...clueFieldList.value.map((f) => ({ label: f.name, value: f.businessKey || f.internalKey || f.id })),
  ]);

  const formKey = ref(FormDesignKeyEnum.MARKETING_FORM);
  const { loading, fieldList, formConfig, unsaved, formDesignRef, checkRepeat, buildSavePayload, setFormConfigDetail } =
    useFormDesignConfig({ formKey });

  // 引用线索字段拖入后, 自动预填字段映射:
  //   优先 businessKey (注册表字段 → 主表列映射)
  //   否则 refFieldId → 线索字段匹配键 (精确映射)
  watch(
    fieldList,
    (list) => {
      list.forEach((f: any) => {
        if (fieldMapping.value[f.id]) return; // 已手动设值, 不覆盖
        if (f.businessKey) {
          fieldMapping.value[f.id] = f.businessKey;
        } else if (f.refFieldId) {
          fieldMapping.value[f.id] = matchClueFieldKey(f.refFieldId);
        }
      });
    },
    { deep: true }
  );

  // 字段映射行: 从 fieldList (design tab 已加载/编辑) 推导
  // 映射值: fieldMapping 记录 > refFieldId 自动匹配
  const mappingRows = computed<MappingRow[]>(() => {
    return fieldList.value
      .filter((f) => f.type !== FieldTypeEnum.DIVIDER)
      .map((f) => ({
        fieldId: f.id,
        fieldName: f.name || '',
        fieldLabel: f.name || f.id,
        mappedTo:
          fieldMapping.value[f.id] || ((f as any).refFieldId ? matchClueFieldKey((f as any).refFieldId) : '') || '',
      }));
  });

  const mappingColumns = computed<DataTableColumns<MappingRow>>(() => [
    {
      title: t('marketingForm.customField'),
      key: 'fieldLabel',
      width: 240,
      ellipsis: { tooltip: true },
    },
    {
      title: t('marketingForm.fieldMapping'),
      key: 'mappedTo',
      render(row) {
        return h(NSelect, {
          value: row.mappedTo,
          options: clueFieldOptions.value,
          onUpdateValue: (val: string) => {
            fieldMapping.value[row.fieldId] = val;
          },
        });
      },
    },
  ]);

  function showUnsavedLeaveTip() {
    openModal({
      type: 'warning',
      title: t('common.unSaveLeaveTitle'),
      content: t('common.editUnsavedLeave'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        visible.value = false;
      },
    });
  }

  function handleBack() {
    if (loading.value || titleSaving.value) {
      return;
    }
    if (unsaved.value) {
      showUnsavedLeaveTip();
      return;
    }
    visible.value = false;
  }

  function buildSaveRequest(name = formName.value) {
    const { fields, formProp } = buildSavePayload();
    // 兜底: 引用线索字段(带 businessKey)自动映射到线索对应列, 避免漏配
    const mapping = { ...fieldMapping.value };
    (fields as any[]).forEach((f) => {
      if (f.businessKey && !mapping[f.id]) {
        mapping[f.id] = f.businessKey;
      }
    });
    return {
      id: currentSourceId.value || undefined,
      name,
      description: settingsForm.value.description,
      targetPoolId: settingsForm.value.targetPoolId,
      dedupStrategy: settingsForm.value.dedupStrategy,
      // INHERIT(跟随线索池)时窗口/身份键不提交(保持 null → 后端跟随池)
      dedupWindow: settingsForm.value.dedupStrategy === 'INHERIT' ? null : settingsForm.value.dedupWindow,
      dedupKey: settingsForm.value.dedupStrategy === 'INHERIT' ? null : settingsForm.value.dedupKey,
      requireName: settingsForm.value.requireName,
      fieldMapping: JSON.stringify(mapping),
      fields,
      formProp,
    };
  }

  async function handleSaveFormDesign() {
    if (!formNameDraft.value.trim().length) {
      return false;
    }
    if (!checkRepeat()) {
      activeTab.value = 'design';
      return false;
    }
    try {
      loading.value = true;
      const params = buildSaveRequest();
      const result = params.id ? await updateMarketingForm(params) : await addMarketingForm(params);
      if (result?.id) {
        currentSourceId.value = result.id;
      }
      nextTick(() => {
        unsaved.value = false;
      });
      emit('saved', currentSourceId.value);
      Message.success(t('common.saveSuccess'));
      return true;
    } catch (error: any) {
      // 之前静默 console.log 导致"没法保存"无提示; 现在把后端异常抛给用户
      console.error('[MarketingForm] save design failed:', error);
      const msg = error?.response?.data?.message || error?.message || t('common.saveFailed');
      Message.error(msg);
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function handleSaveSettings() {
    try {
      await settingsFormRef.value?.validate();
    } catch {
      return;
    }
    try {
      loading.value = true;
      const params = buildSaveRequest();
      await updateMarketingForm(params);
      nextTick(() => {
        unsaved.value = false;
      });
      emit('saved', currentSourceId.value);
      Message.success(t('common.saveSuccess'));
    } catch (error: any) {
      console.error('[MarketingForm] save settings failed:', error);
      const msg = error?.response?.data?.message || error?.message || t('common.saveFailed');
      Message.error(msg);
    } finally {
      loading.value = false;
    }
  }

  function handleBeforeChangeTab(newVal: string | number, oldVal: string | number | null) {
    if (newVal === 'settings' && !currentSourceId.value) {
      Message.warning(t('marketingForm.saveFormDesignFirst'));
      return false;
    }
    if (oldVal !== 'design' || newVal === oldVal || !unsaved.value) {
      return true;
    }
    return new Promise<boolean>((resolve) => {
      let resolved = false;
      const resolveOnce = (value: boolean) => {
        if (resolved) return;
        resolved = true;
        resolve(value);
      };
      openModal({
        type: 'warning',
        title: t('marketingForm.formUnsavedTitle'),
        content: t('marketingForm.formUnsavedSwitchTip'),
        negativeText: t('common.cancel'),
        positiveText: t('common.save'),
        onPositiveClick: async () => {
          const saved = await handleSaveFormDesign();
          resolveOnce(saved);
        },
        onNegativeClick: () => {
          resolveOnce(true);
        },
      });
    });
  }

  async function handleEditTitle(value: string, done?: () => void) {
    const name = value.trim();
    if (name === formName.value) {
      done?.();
      return;
    }
    try {
      titleSaving.value = true;
      if (!currentSourceId.value) {
        formName.value = name;
        formNameDraft.value = name;
        unsaved.value = true;
        done?.();
        return;
      }
      await updateMarketingForm(buildSaveRequest(name));
      formName.value = name;
      formNameDraft.value = name;
      emit('saved', currentSourceId.value);
      Message.success(t('common.saveSuccess'));
      done?.();
    } catch (error: any) {
      console.error('[MarketingForm] save title failed:', error);
      const msg = error?.response?.data?.message || error?.message || t('common.saveFailed');
      Message.error(msg);
    } finally {
      titleSaving.value = false;
    }
  }

  function createDefaultMarketingFormFields(): FormCreateField[] {
    // 新建表单默认仅包含「姓名」必填字段（引用线索姓名字段, 存入线索姓名独立字段EAV, 不占 name 列）
    const nameField: FormCreateField = {
      ...inputDefaultFieldConfig,
      id: getGenerateId(),
      name: t('marketingForm.name'),
      rules: [{ key: FieldRuleEnum.REQUIRED }],
    };
    // 通过 refFieldId 精确映射到线索姓名字段, 提交后姓名写入线索姓名字段(EAV)
    (nameField as any).refFieldId = NAME_FIELD_REF_ID;
    (nameField as any).businessKey = 'clueNameField';
    (nameField as any).isNameField = true; // 标记为姓名必填字段(不可删除)
    return [nameField];
  }

  async function initMarketingFormConfig() {
    currentSourceId.value = props.sourceId || '';
    // Load pool options (settings tab needs them regardless of new/existing)
    try {
      poolOptions.value = (await getPoolOptions()) || [];
    } catch (error) {
      console.log(error);
      poolOptions.value = [];
    }

    if (!currentSourceId.value) {
      formName.value = t('marketingForm.unnamedForm');
      formNameDraft.value = formName.value;
      formEnabled.value = true;
      settingsForm.value = {
        description: '',
        // 新建表单默认进组织默认线索池（isDefault 标记优先，否则第一个启用池）
        targetPoolId: (poolOptions.value.find((p) => p.isDefault) || poolOptions.value[0])?.id || '',
        dedupStrategy: 'INHERIT',
        dedupWindow: null,
        dedupKey: '',
        requireName: false,
      };
      fieldMapping.value = {};
      setFormConfigDetail({
        fields: createDefaultMarketingFormFields(),
        formProp: createDefaultFormConfig(t),
      });
      return;
    }

    try {
      loading.value = true;
      const detail = await getMarketingFormDetail(currentSourceId.value);
      formName.value = detail.name || '';
      formNameDraft.value = formName.value;
      formEnabled.value = detail.status !== 'CLOSED';
      settingsForm.value = {
        description: detail.description || '',
        targetPoolId: detail.targetPoolId || '',
        dedupStrategy: detail.dedupStrategy || 'INHERIT',
        dedupWindow: detail.dedupWindow ?? null,
        dedupKey: detail.dedupKey || '',
        requireName: !!detail.requireName,
      };
      try {
        fieldMapping.value = detail.fieldMapping ? JSON.parse(detail.fieldMapping) : {};
      } catch {
        fieldMapping.value = {};
      }
      setFormConfigDetail({
        fields: detail.fields || [],
        formProp: detail.formProp || createDefaultFormConfig(t),
      });
    } catch (error) {
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => visible.value,
    (value) => {
      if (!value) {
        activeTab.value = 'design';
        return;
      }
      // 非 admin 强制回退到「表单设计」，避免进入无权访问的市场设置
      activeTab.value = props.defaultTab === 'settings' && !isAdmin.value ? 'design' : props.defaultTab || 'design';
      initMarketingFormConfig();
    },
    { immediate: true }
  );
</script>

<style lang="less">
  .process-name-header {
    min-width: 0;
    > * {
      min-width: 0;
      max-width: 100%;
      flex: 1 1 auto;
    }
    .table-row-edit {
      @apply invisible;
    }
    &:hover {
      .table-row-edit {
        color: var(--primary-8);
        @apply visible;
      }
    }
    .process-name {
      overflow: hidden;
      min-width: 0;
      max-width: 100%;
      font-size: 14px;
      font-weight: 400;
      border-bottom: 2px solid var(--text-n6);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
  .custom-form-design {
    .crm-form-design--composition {
      > .n-scrollbar {
        .n-scrollbar-content {
          width: 100% !important;
        }
      }
    }
  }
</style>
