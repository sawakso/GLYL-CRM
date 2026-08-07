<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
    :show-label="!props.isSubTableRender && !props.isDescriptionRender"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-date-picker
      v-model:value="value"
      :type="pickerType"
      :placeholder="props.fieldConfig.placeholder || t('common.pleaseInput')"
      :disabled="props.fieldConfig.editable === false || props.disabled || !!props.fieldConfig.resourceFieldId"
      class="w-full"
      :status="props.feedback ? 'error' : undefined"
      @update-value="($event) => emit('change', $event)"
    >
    </n-date-picker>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDatePicker, NDivider, NFormItem } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    disabled?: boolean;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    isDescriptionRender?: boolean; // 是否是描述渲染
    feedback?: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: null | number | (string | number)[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<null | number | [number, number]>('value', {
    default: null,
  });

  // n-date-picker 只支持 date/datetime/daterange/datetimerange；字段配置 dateType 可能为
  // undefined 或 month 等无效值，需兜底为 datetime，否则 naive-ui 报 type wrong 并崩溃
  const pickerType = computed<'date' | 'datetime' | 'daterange' | 'datetimerange'>(() => {
    const t = props.fieldConfig.dateType as string;
    return ['date', 'datetime', 'daterange', 'datetimerange'].includes(t)
      ? (t as 'date' | 'datetime' | 'daterange' | 'datetimerange')
      : 'datetime';
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = Number.isNaN(Number(val)) || val === '' || val === null ? value.value : Number(val);
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.fieldConfig.dateDefaultType,
    (val) => {
      if (props.needInitDetail) {
        return;
      }

      if (val === 'current') {
        value.value = new Date().getTime();
      } else if (val === 'custom' && props.fieldConfig.defaultValue === null && !value.value) {
        value.value = null;
      }
      emit('change', value.value);
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
