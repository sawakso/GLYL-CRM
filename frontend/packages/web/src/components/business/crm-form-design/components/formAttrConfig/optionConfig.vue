<template>
  <n-radio-group v-model:value="fieldConfig.optionSource" name="radiogroup" class="flex" :disabled="props.disabled">
    <n-radio-button value="ref" class="flex-1 text-center">
      {{ t('crmFormDesign.quotingData') }}
    </n-radio-button>
    <n-radio-button value="custom" class="flex-1 text-center">
      {{ t('crmFormDesign.custom') }}
    </n-radio-button>
  </n-radio-group>
  <n-cascader
    v-if="fieldConfig.optionSource === 'ref'"
    v-model:value="fieldConfig.refId"
    :disabled="props.disabled"
    :options="refOptions"
    check-strategy="child"
    :render-label="renderCascaderLabel"
    :menu-props="{ class: 'form-design-cascader' }"
    remote
    :on-load="handleLoadRef"
    @update:value="updateFormKeyFromCascader"
  />
  <!-- 引用其他表单：选中数据源表单后，列出该表单全部数据记录，由用户勾选要加入表单的选项 -->
  <div
    v-if="fieldConfig.optionSource === 'ref' && refRecords.length"
    class="mt-[12px] flex flex-col gap-[8px]"
  >
    <div class="text-[13px] text-[var(--text-n4)]">
      选择要加入表单的「{{ selectedFormLabel }}」（共 {{ refRecords.length }} 条）
    </div>
    <div class="flex max-h-[260px] flex-col gap-[8px] overflow-auto pr-[4px]">
      <div v-for="rec in refRecords" :key="rec.value" class="flex items-center gap-[8px]">
        <n-checkbox
          :checked="isOptionSelected(rec.value)"
          :disabled="props.disabled"
          @update:checked="(c) => toggleRefOption(rec, c)"
        />
        <span class="text-[14px]">{{ rec.label }}</span>
      </div>
    </div>
  </div>
  <component
    :is="getComponent"
    v-else-if="fieldConfig.optionSource === 'custom' && fieldConfig.customOptions"
    :value="fieldConfig.defaultValue"
    :disabled="props.disabled"
  >
    <!-- 通过draggable属性控制带.draggable类的元素可拖拽，实现部分元素不允许拖拽 -->
    <VueDraggable
      v-model="fieldConfig.customOptions"
      :animation="150"
      draggable=".draggable"
      handle=".handle"
      class="flex flex-col gap-[8px]"
    >
      <div
        v-for="(item, i) in fieldConfig.customOptions"
        :key="item.value"
        class="flex flex-wrap items-center gap-[8px]"
        :class="item.value === 'other' ? '' : 'draggable'"
      >
        <n-tooltip
          :delay="300"
          :show-arrow="false"
          :disabled="item.value === 'other'"
          class="crm-form-design--composition-item-tools-tip"
        >
          <template #trigger>
            <CrmIcon
              type="iconicon_move"
              class="handle cursor-move"
              :class="item.value === 'other' ? 'cursor-not-allowed text-[var(--text-n6)]' : ''"
            />
          </template>
          {{ t('common.sort') }}
        </n-tooltip>
        <n-checkbox v-if="isMultiple" :value="item.value" @click="handleCheckBoxOptionClick(item.value)" />
        <n-radio
          v-else
          :value="item.value"
          :default-checked="fieldConfig.defaultValue === item.value"
          class="flex items-center"
          :disabled="props.disabled"
          @click="handleRadioOptionClick(item.value)"
        />
        <n-input
          v-model:value="item.label"
          :maxlength="50"
          :disabled="props.disabled"
          :status="
            fieldConfig.customOptions.some((e) => e.value !== item.value && e.label === item.label)
              ? 'error'
              : undefined
          "
          class="flex-1"
          clearable
        ></n-input>
        <n-tooltip :delay="300" :show-arrow="false" class="crm-form-design--composition-item-tools-tip">
          <template #trigger>
            <n-button
              quaternary
              type="error"
              size="small"
              :disabled="
                props.disabled || fieldConfig.customOptions?.length === 1 || fieldConfig.defaultValue === item.value
              "
              class="text-btn-error p-[4px] text-[var(--text-n1)]"
              @click="handleOptionDelete(i)"
            >
              <CrmIcon type="iconicon_delete" :size="14" />
            </n-button>
          </template>
          {{ t('common.delete') }}
        </n-tooltip>
        <div
          v-if="fieldConfig.customOptions.some((e) => e.value !== item.value && e.label === item.label)"
          class="ml-[48px] w-full text-[12px] text-[var(--error-red)]"
        >
          {{ t('crmFormDesign.repeatOptionName') }}
        </div>
      </div>
    </VueDraggable>
  </component>
  <div v-if="fieldConfig.optionSource === 'custom'" class="flex items-center justify-center gap-[8px]">
    <div
      class="cursor-pointer text-[var(--primary-8)]"
      :class="props.disabled ? '!text-[var(--primary-4)]' : ''"
      @click="handleAddOption"
    >
      {{ t('crmFormDesign.addOption') }}
    </div>
    <n-divider vertical class="!m-0" />
    <div
      :class="
        props.disabled || fieldConfig.customOptions?.some((item) => item.value === 'other')
          ? 'cursor-not-allowed text-[var(--primary-4)]'
          : 'cursor-pointer text-[var(--primary-8)]'
      "
      @click="handleAddOtherOption"
    >
      {{ t('crmFormDesign.addOptionOther') }}
    </div>
    <n-divider vertical class="!m-0" />
    <div
      class="cursor-pointer text-[var(--primary-8)]"
      :class="props.disabled ? '!text-[var(--primary-4)]' : ''"
      @click="handleShowBatchEditModal"
    >
      {{ t('crmFormDesign.batchEdit') }}
    </div>
  </div>
  <CrmModal
    v-model:show="showModal"
    :title="t('crmFormDesign.batchEdit')"
    :positive-text="t('common.save')"
    @confirm="handleBatchEditConfirm"
  >
    <n-input
      v-model:value="batchEditValue"
      type="textarea"
      :autosize="{
        minRows: 3,
        maxRows: 10,
      }"
      clearable
    ></n-input>
    <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">{{ t('crmFormDesign.batchEditTip') }}</div>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    CascaderOption,
    NButton,
    NCascader,
    NCheckbox,
    NCheckboxGroup,
    NDivider,
    NInput,
    NRadio,
    NRadioButton,
    NRadioGroup,
    NTooltip,
  } from 'naive-ui';
  import { cloneDeep, debounce } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { findNodePathByKey, getGenerateId } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { fullFormSettingList } from '@/components/business/crm-form-create/config';
  import { FormCreateField, FormCreateFieldOption } from '@/components/business/crm-form-create/types';
  import { sourceApi } from '@/components/business/crm-data-source-select/config';

  import { getFormDesignConfig } from '@/api/modules';

  const props = defineProps<{
    disabled?: boolean;
  }>();

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const isMultiple = computed(() =>
    [
      FieldTypeEnum.CHECKBOX,
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
    ].includes(fieldConfig.value.type)
  );

  const defaultRefOptions = fullFormSettingList
    .filter((item) => !!item.formKey)
    .map((i) => ({ ...i, value: i.formKey, isLeaf: !!i.dataSource }));
  const refOptions = ref<CascaderOption[]>(cloneDeep(defaultRefOptions));

  // 选中"数据来源表单"后，拉取到的该表单全部数据记录（勾选池）
  const refRecords = ref<FormCreateFieldOption[]>([]);
  // 当前选中的数据来源表单名称，用于展示"勾选提示"
  const selectedFormLabel = ref<string>('');

  // 各模块记录对应的"名称"字段，用于把数据记录转成选项 label
  const formNameFieldMap: Record<string, string> = {
    [FormDesignKeyEnum.PRODUCT]: 'productName',
    [FormDesignKeyEnum.CUSTOMER]: 'customerName',
    [FormDesignKeyEnum.CONTACT]: 'contactName',
    [FormDesignKeyEnum.CLUE]: 'clueName',
    [FormDesignKeyEnum.BUSINESS]: 'opportunityName',
    [FormDesignKeyEnum.PRICE]: 'priceName',
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: 'quotationName',
    [FormDesignKeyEnum.ORDER]: 'orderName',
    [FormDesignKeyEnum.CONTRACT]: 'contractName',
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: 'contractPaymentPlanName',
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: 'contractPaymentRecordName',
    [FormDesignKeyEnum.INVOICE]: 'invoiceName',
    [FormDesignKeyEnum.BUSINESS_TITLE]: 'name',
    [FormDesignKeyEnum.CUSTOM_FORM]: 'customFormDataName',
  };

  // 拉取某个"数据来源表单"的全部数据记录，转成 { label, value } 选项
  // 后端限制：pageNum 必须 > 0，pageSize 不能超过 500，故分页拉取
  async function getRefRecords(rootNode: any): Promise<FormCreateFieldOption[]> {
    const dataSource = rootNode?.dataSource as FieldDataSourceTypeEnum | undefined;
    if (!dataSource) return [];
    const api = sourceApi[dataSource];
    if (!api) return [];
    const nameKey = formNameFieldMap[rootNode.formKey as FormDesignKeyEnum];
    const PAGE_SIZE = 500;
    const MAX_PAGES = 100; // 安全上限，防止后端异常导致死循环
    const result: FormCreateFieldOption[] = [];
    try {
      for (let current = 1; current <= MAX_PAGES; current++) {
        const res: any = await api({ current, pageSize: PAGE_SIZE });
        const list: any[] = res?.list || [];
        if (list.length === 0) break;
        for (const record of list) {
          result.push({
            label: record[nameKey] || record.name || record.id,
            value: record.id,
          });
        }
        // 返回条数小于页大小，说明已是最后一页
        if (list.length < PAGE_SIZE) break;
      }
      return result;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('[optionConfig] getRefRecords failed', error);
      return [];
    }
  }

  // 获取第二层的数据
  const getRefData = async (key: string) => {
    const res = await getFormDesignConfig(key as FormDesignKeyEnum);
    return res.fields
      .filter((field) => field.optionSource === 'custom' && field.type === fieldConfig.value.type)
      .map((field) => ({ label: field.name, value: field.id, isLeaf: true, options: field.options }));
  };

  async function handleLoadRef(option: CascaderOption) {
    if (option.children) return;
    option.loading = true;
    let key = option.formKey as FormDesignKeyEnum;
    // 跟进记录和计划 key 特殊处理，因为各模块 key 不一致但是表单配置一致
    if (key.includes('record')) {
      key = 'record' as FormDesignKeyEnum;
    } else if (key.includes('plan')) {
      key = 'plan' as FormDesignKeyEnum;
    }
    option.children = await getRefData(key);
    // 没数据
    if (!option.children || option.children.length === 0) {
      option.children = [
        {
          label: t('common.noData'),
          value: 'no-data',
          disabled: true,
          isLeaf: true,
        },
      ];
    }
    option.loading = false;
  }

  function renderCascaderLabel(option: CascaderOption, checked: boolean) {
    if (option.value === 'no-data') {
      return h('span', { class: 'no-data' }, option.label);
    }
    return option.label;
  }

  // 更新第一层父节点的值，传入后端，方便后期回显
  async function updateFormKeyFromCascader(childValue: string) {
    if (!childValue) {
      fieldConfig.value.refFormKey = '';
      fieldConfig.value.options = [];
      refRecords.value = [];
      selectedFormLabel.value = '';
      return;
    }
    const path = findNodePathByKey(refOptions.value, childValue, undefined, 'value');
    if (!path) {
      fieldConfig.value.refFormKey = '';
      fieldConfig.value.options = [];
      return;
    }
    fieldConfig.value.refFormKey = path.treePath[0].value as string;
    if (path.treePath.length === 1 && path.treePath[0].dataSource) {
      // 选中了"数据来源表单"根节点 → 拉取该表单的全部数据记录作为"勾选池"，由用户自行挑选
      const rootNode = path.treePath[0];
      selectedFormLabel.value = (rootNode.label as string) || '';
      refRecords.value = await getRefRecords(rootNode);
      // 仅保留仍存在于勾选池中的已选项，避免脏数据
      const validValues = new Set(refRecords.value.map((r) => r.value));
      fieldConfig.value.options = (fieldConfig.value.options || []).filter((o) => validValues.has(o.value));
    } else if (path.treePath.length === 1) {
      // 普通表单根节点（非数据来源，理论上不可直接选中）
      fieldConfig.value.options = [];
    } else {
      fieldConfig.value.options = path.options;
    }
  }

  // 判断某条记录是否已被选入表单选项
  function isOptionSelected(value: string | number) {
    return (fieldConfig.value.options || []).some((o) => o.value === value);
  }

  // 勾选/取消：把记录加入或移出 fieldConfig.options
  function toggleRefOption(rec: FormCreateFieldOption, checked: boolean) {
    const opts = fieldConfig.value.options || (fieldConfig.value.options = []);
    const idx = opts.findIndex((o) => o.value === rec.value);
    if (checked && idx === -1) {
      opts.push({ label: rec.label, value: rec.value });
    } else if (!checked && idx > -1) {
      opts.splice(idx, 1);
    }
  }

  // 初始化回显
  async function initEchoByPath(rootValue: string) {
    const rootOpt = refOptions.value.find((opt) => opt.value === rootValue);
    if (!rootOpt) return;
    if (rootOpt.dataSource) {
      // 数据来源表单：回填级联选中值，并补拉全部数据记录作为勾选池（options 已随字段保存）
      fieldConfig.value.refId = rootValue;
      selectedFormLabel.value = (rootOpt.label as string) || '';
      refRecords.value = await getRefRecords(rootOpt);
      const validValues = new Set(refRecords.value.map((r) => r.value));
      if (!fieldConfig.value.options) fieldConfig.value.options = [];
      fieldConfig.value.options = fieldConfig.value.options.filter((o) => validValues.has(o.value));
    } else {
      rootOpt.children = await getRefData(rootValue);
    }
  }

  watch(
    () => fieldConfig.value.id,
    async () => {
      refOptions.value = cloneDeep(defaultRefOptions);
      if (fieldConfig.value.optionSource === 'ref' && fieldConfig.value.refFormKey) {
        initEchoByPath(fieldConfig.value.refFormKey);
      }
    },
    { immediate: true }
  );

  watch(
    () => fieldConfig.value.optionSource,
    async (val) => {
      fieldConfig.value.defaultValue = isMultiple.value ? [] : '';
      if (val === 'ref') {
        if (fieldConfig.value.refFormKey && fieldConfig.value.refId) {
          await initEchoByPath(fieldConfig.value.refFormKey);
          updateFormKeyFromCascader(fieldConfig.value.refId);
        } else {
          fieldConfig.value.options = [];
        }
      } else {
        fieldConfig.value.options = fieldConfig.value.customOptions || fieldConfig.value.options;
      }
    }
  );

  const getComponent = computed(() => {
    if (isMultiple.value) {
      return NCheckboxGroup;
    }
    return NRadioGroup;
  });

  const handleRadioOptionClick = debounce((val: string | number) => {
    if (fieldConfig.value.defaultValue === val) {
      fieldConfig.value.defaultValue = '';
    } else {
      fieldConfig.value.defaultValue = val;
    }
  });

  const handleCheckBoxOptionClick = debounce((val: string | number) => {
    if (!Array.isArray(fieldConfig.value.defaultValue)) {
      fieldConfig.value.defaultValue = [];
    }
    const index = fieldConfig.value.defaultValue.indexOf(val);
    if (index > -1) {
      fieldConfig.value.defaultValue.splice(index, 1);
    } else {
      fieldConfig.value.defaultValue.push(val);
    }
  });

  function handleAddOption() {
    if (props.disabled) {
      return;
    }
    if (!fieldConfig.value.customOptions?.some((e) => e.value === 'other')) {
      fieldConfig.value.customOptions?.push({
        label: t('crmFormDesign.option', { i: fieldConfig.value.customOptions.length + 1 }),
        value: getGenerateId(),
      });
    } else {
      fieldConfig.value.customOptions?.splice(fieldConfig.value.customOptions.length - 1, 0, {
        label: t('crmFormDesign.option', { i: fieldConfig.value.customOptions.length }),
        value: getGenerateId(),
      });
    }
    fieldConfig.value.options = [...(fieldConfig.value.customOptions || [])];
  }

  function handleAddOtherOption() {
    if (props.disabled || fieldConfig.value.customOptions?.some((e) => e.value === 'other')) {
      return;
    }
    fieldConfig.value.customOptions?.push({
      label: t('crmFormDesign.optionOther'),
      value: 'other',
    });
    fieldConfig.value.options = [...(fieldConfig.value.customOptions || [])];
  }

  function setDefaultValue() {
    if (isMultiple.value) {
      fieldConfig.value.defaultValue = fieldConfig.value.defaultValue?.filter((e: any) =>
        fieldConfig.value.customOptions?.some((item) => item.value === e)
      );
    } else if (fieldConfig.value.customOptions?.every((e) => e.value !== fieldConfig.value.defaultValue)) {
      fieldConfig.value.defaultValue = '';
    }
  }

  function handleOptionDelete(i: number) {
    fieldConfig.value.customOptions?.splice(i, 1);
    fieldConfig.value.options = [...(fieldConfig.value.customOptions || [])];
    setDefaultValue();
  }

  const showModal = ref(false);
  const batchEditValue = ref('');

  function handleShowBatchEditModal() {
    if (props.disabled) {
      return;
    }
    showModal.value = true;
    batchEditValue.value = fieldConfig.value.customOptions?.map((e) => e.label).join('\n') || '';
  }

  function handleBatchEditConfirm() {
    const resArr = Array.from(new Set(batchEditValue.value.split('\n')));
    if (resArr.length === 0) {
      fieldConfig.value.customOptions = [
        {
          label: t('crmFormDesign.option', { i: 1 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 2 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 3 }),
          value: getGenerateId(),
        },
      ];
    } else {
      const newOptions = resArr
        .map((e) => e.trim())
        .filter((e) => e)
        .map((e) => ({
          label: e.slice(0, 50),
          value: fieldConfig.value.customOptions?.find((item) => item.label === e)?.value || getGenerateId(),
        }));
      fieldConfig.value.customOptions = newOptions;
    }
    fieldConfig.value.options = [...(fieldConfig.value.customOptions || [])];
    setDefaultValue();
    showModal.value = false;
  }
</script>

<style lang="less">
  // 没数据的样式
  .form-design-cascader .n-cascader-option.n-cascader-option--disabled:has(.no-data) {
    .n-cascader-option__label {
      text-align: center;
    }
    &:hover {
      background: transparent !important;
    }
  }
</style>
