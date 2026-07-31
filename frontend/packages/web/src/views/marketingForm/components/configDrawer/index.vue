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
            :options="poolOptions"
            :placeholder="t('marketingForm.selectTargetPool')"
            filterable
            label-field="name"
            value-field="id"
          />
        </n-form-item>
        <n-form-item :label="t('marketingForm.dedupStrategy')" path="dedupStrategy">
          <n-select v-model:value="settingsForm.dedupStrategy" :options="dedupStrategyOptions" />
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
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
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
    NSelect,
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

  import { addMarketingForm, getMarketingFormDetail, getPoolOptions, updateMarketingForm } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

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

  const activeTab = ref<'design' | 'settings'>('design');
  const tabList = computed(() => [
    { name: 'design', tab: t('marketingForm.formDesign') },
    { name: 'settings', tab: t('marketingForm.settings') },
  ]);

  const titleSaving = ref(false);
  const currentSourceId = ref('');
  const formName = ref('');
  const formNameDraft = ref('');
  const formEnabled = ref(true);
  const settingsFormRef = ref<FormInst | null>(null);
  const poolOptions = ref<CluePoolItem[]>([]);

  const settingsForm = ref({
    description: '',
    targetPoolId: '' as string,
    dedupStrategy: 'NONE',
  });

  // fieldMapping stored as JSON string: { [fieldId]: 'clueFieldName' }
  const fieldMapping = ref<Record<string, string>>({});

  const targetPoolRule = {
    required: true,
    message: t('marketingForm.selectTargetPool'),
    trigger: ['change', 'blur'],
  };

  const dedupStrategyOptions = computed(() => [
    { label: t('marketingForm.strategy.none'), value: 'NONE' },
    { label: t('marketingForm.strategy.update'), value: 'UPDATE' },
    { label: t('marketingForm.strategy.skip'), value: 'SKIP' },
    { label: t('marketingForm.strategy.mark'), value: 'MARK' },
  ]);

  // 可映射的 Clue 字段名 (对应后端 applyClueField 的 switch case)
  const clueFieldOptions = computed(() => [
    { label: t('marketingForm.noMapping'), value: '' },
    { label: t('marketingForm.clueField.name'), value: 'name' },
    { label: t('marketingForm.clueField.contact'), value: 'contact' },
    { label: t('marketingForm.clueField.mobile'), value: 'mobile' },
    { label: t('marketingForm.clueField.tel'), value: 'tel' },
    { label: t('marketingForm.clueField.email'), value: 'email' },
    { label: t('marketingForm.clueField.company'), value: 'company' },
    { label: t('marketingForm.clueField.department'), value: 'department' },
    { label: t('marketingForm.clueField.jobTitle'), value: 'jobTitle' },
    { label: t('marketingForm.clueField.address'), value: 'address' },
    { label: t('marketingForm.clueField.url'), value: 'url' },
    { label: t('marketingForm.clueField.source'), value: 'source' },
    { label: t('marketingForm.clueField.leadsStage'), value: 'leadsStage' },
    { label: t('marketingForm.clueField.bizStatus'), value: 'bizStatus' },
    { label: t('marketingForm.clueField.lifeStatus'), value: 'lifeStatus' },
    { label: t('marketingForm.clueField.remark'), value: 'remark' },
  ]);

  const formKey = ref(FormDesignKeyEnum.MARKETING_FORM);
  const { loading, fieldList, formConfig, unsaved, formDesignRef, checkRepeat, buildSavePayload, setFormConfigDetail } =
    useFormDesignConfig({ formKey });

  // 字段映射行: 从 fieldList (design tab 已加载/编辑) 推导
  const mappingRows = computed<MappingRow[]>(() => {
    return fieldList.value
      .filter((f) => f.type !== FieldTypeEnum.DIVIDER)
      .map((f) => ({
        fieldId: f.id,
        fieldName: f.name || '',
        fieldLabel: f.name || f.id,
        mappedTo: fieldMapping.value[f.id] || '',
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
    return {
      id: currentSourceId.value || undefined,
      name,
      description: settingsForm.value.description,
      targetPoolId: settingsForm.value.targetPoolId,
      dedupStrategy: settingsForm.value.dedupStrategy,
      fieldMapping: JSON.stringify(fieldMapping.value),
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
    } catch (error) {
      console.log(error);
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
    } catch (error) {
      console.log(error);
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
    } catch (error) {
      console.log(error);
    } finally {
      titleSaving.value = false;
    }
  }

  function createDefaultMarketingFormFields(): FormCreateField[] {
    const createDivider = (name: string): FormCreateField => ({
      ...dividerDefaultFieldConfig,
      id: getGenerateId(),
      name,
    });
    const createInput = (name: string, options?: Partial<FormCreateField>): FormCreateField => ({
      ...inputDefaultFieldConfig,
      id: getGenerateId(),
      name,
      ...options,
    });

    return [
      createDivider(t('marketingForm.basicInfo')),
      createInput(t('marketingForm.clueField.name'), {
        rules: [{ key: FieldRuleEnum.REQUIRED }],
      }),
      createInput(t('marketingForm.clueField.mobile')),
      createInput(t('marketingForm.clueField.email')),
      createDivider(t('marketingForm.customerInfo')),
      createInput(t('marketingForm.clueField.company')),
      createInput(t('marketingForm.clueField.jobTitle')),
      createInput(t('marketingForm.clueField.address')),
      createDivider(t('marketingForm.customField')),
      createInput(`${t('marketingForm.customField')} 1`),
      createInput(`${t('marketingForm.customField')} 2`),
    ];
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
        targetPoolId: '',
        dedupStrategy: 'NONE',
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
        dedupStrategy: detail.dedupStrategy || 'NONE',
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
      activeTab.value = props.defaultTab || 'design';
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
