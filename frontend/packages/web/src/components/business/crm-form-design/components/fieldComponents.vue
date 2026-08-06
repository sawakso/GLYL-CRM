<template>
  <n-scrollbar class="flex flex-col p-[16px]">
    <!-- 市场表单仅允许使用意向线索字段, 隐藏通用基础/高级字段 -->
    <template v-if="!isMarketingForm">
      <div class="crm-form-design-field-title">{{ t('crmFormDesign.basicField') }}</div>
      <VueDraggable
        v-model="basicFields"
        :animation="150"
        ghost-class="crm-form-design--composition-item-ghost"
        :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
        :clone="clone"
        :sort="false"
        class="crm-form-design-field-wrapper mb-[24px]"
        @move="handleMove"
      >
        <div
          v-for="field of basicFields"
          :key="field.type"
          class="crm-form-design-field-item"
          draggable="true"
          @click="() => handleFieldClick(field)"
        >
          <CrmIcon :type="field.icon" />
          <div>{{ t(field.name) }}</div>
        </div>
      </VueDraggable>
      <div class="crm-form-design-field-title">{{ t('crmFormDesign.advancedField') }}</div>
      <VueDraggable
        v-model="realAdvancedFields"
        :animation="150"
        ghost-class="crm-form-design--composition-item-ghost"
        :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
        :clone="clone"
        :sort="false"
        class="crm-form-design-field-wrapper"
        @move="handleMove"
      >
        <div
          v-for="field of realAdvancedFields"
          :key="field.type"
          class="crm-form-design-field-item"
          :class="getFieldDisable(field) ? 'crm-form-design-field-item--disabled' : ''"
          :draggable="!getFieldDisable(field)"
          @click="() => handleFieldClick(field)"
        >
          <CrmIcon :type="field.icon" />
          <div>{{ t(field.name) }}</div>
        </div>
      </VueDraggable>
    </template>

    <!-- 引用 CRM 业务字段(仅市场表单设计器, 从线索对象引用现成字段) -->
    <template v-if="showRefGroup">
      <div class="crm-form-design-field-title">{{ t('crmFormDesign.refClueField') }}</div>
      <VueDraggable
        v-model="refClueFields"
        :animation="150"
        ghost-class="crm-form-design--composition-item-ghost"
        :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
        :clone="clone"
        :sort="false"
        class="crm-form-design-field-wrapper"
      >
        <div
          v-for="field of refClueFields"
          :key="field.id"
          class="crm-form-design-field-item crm-form-design-field-item--ref"
          draggable="true"
          @click="() => handleRefFieldClick(field)"
        >
          <CrmIcon :type="field.icon || 'iconicon_text'" />
          <div>{{ field.name }}</div>
        </div>
      </VueDraggable>
    </template>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NScrollbar } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { advancedFields, basicFields } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getClueFormConfig } from '@/api/modules';

  const props = defineProps<{
    fieldList: FormCreateField[];
    formKey: FormDesignKeyEnum;
  }>();
  const emit = defineEmits<{
    (e: 'select', field: FormCreateField): void;
  }>();

  const { t } = useI18n();

  // 市场表单仅允许使用意向线索字段, 隐藏通用基础/高级字段
  const isMarketingForm = computed(() => props.formKey === FormDesignKeyEnum.MARKETING_FORM);

  // 引用 CRM 业务字段分组: 仅市场表单设计器显示, 拉取线索对象字段 schema 作为可拖模板
  const showRefGroup = computed(() => props.formKey === FormDesignKeyEnum.MARKETING_FORM);
  const refClueFields = ref<FormCreateField[]>([]);
  if (showRefGroup.value) {
    getClueFormConfig()
      .then((res: any) => {
        // 放开业务Key限制: 线索对象全部字段均可引用 (fieldId + refFieldId 提供映射, 不依赖 businessKey 注册表)
        // 过滤掉已停用(readable=false)的字段, 市场表单只能关联可用的意向线索字段
        refClueFields.value = (res?.fields || [])
          .filter((f: FormCreateField) => f.id && f.type !== FieldTypeEnum.DIVIDER && (f as any).readable !== false)
          .map((f: FormCreateField) => ({ ...cloneDeep(f), isRef: true }));
      })
      .catch(() => {
        refClueFields.value = [];
      });
  }

  // 将引用模板转为实际字段: 保留 businessKey/type/options/rules, 作为映射到线索对应列的依据
  // 注意:
  //  - 不设置 resourceFieldId —— ModuleFormService.saveFields 会剔除带 resourceFieldId 的字段
  //  - 必须删掉线索本身的 internalKey / isSys, 否则会被误判为表单内置静态列/系统字段:
  //    1) internalKey 会让它走业务字段逻辑, 且可能与线索模块的 internalKey 语义冲突
  //    2) isSys 会让属性面板/删除逻辑把它当系统字段锁定, 导致"没法删除"
  //  - refFieldId = 被引用的线索字段ID: 提交后后端据此把表单值映射回线索模块字段落库 (EAV),
  //    即使该字段不在 BusinessModuleField 注册表 (businessKey 为空) 也能精确收集
  function makeRefField(f: FormCreateField): FormCreateField {
    const cloned = cloneDeep(f);
    (cloned as any).refFieldId = f.id; // 先记录被引用线索字段ID, 再生成表单内新id
    cloned.id = getGenerateId();
    delete (cloned as any).resourceFieldId;
    delete (cloned as any).internalKey;
    delete (cloned as any).isSys;
    delete (cloned as any).disabledProps;
    delete (cloned as any).isRef;
    cloned.isNew = true;
    cloned.isRefField = true; // 标记: 用户新增的引用字段, 允许删除/编辑
    return cloned;
  }

  function handleRefFieldClick(field: FormCreateField) {
    emit('select', makeRefField(field));
  }

  const realAdvancedFields: FormCreateField[] = [];
  if (
    [
      FormDesignKeyEnum.PRICE,
      FormDesignKeyEnum.CONTRACT,
      FormDesignKeyEnum.ORDER,
      FormDesignKeyEnum.INVOICE,
      FormDesignKeyEnum.CUSTOM_FORM,
    ].includes(props.formKey)
  ) {
    advancedFields.forEach((field) => {
      if (field.type !== FieldTypeEnum.SUB_PRICE) {
        realAdvancedFields.push(field);
      }
    });
  } else if (props.formKey === FormDesignKeyEnum.OPPORTUNITY_QUOTATION) {
    advancedFields.forEach((field) => {
      if (field.type !== FieldTypeEnum.SUB_PRODUCT) {
        realAdvancedFields.push(field);
      }
    });
  } else {
    advancedFields.forEach((field) => {
      if (![FieldTypeEnum.SUB_PRODUCT, FieldTypeEnum.SUB_PRICE].includes(field.type)) {
        realAdvancedFields.push(field);
      }
    });
  }

  function getFieldDisable(item: FormCreateField) {
    if (item.type === FieldTypeEnum.SERIAL_NUMBER) {
      return props.fieldList.some((e) => e.type === FieldTypeEnum.SERIAL_NUMBER && !e.resourceFieldId);
    }
    if (
      item.type === FieldTypeEnum.SUB_PRODUCT &&
      ![FormDesignKeyEnum.OPPORTUNITY_QUOTATION, FormDesignKeyEnum.CONTRACT, FormDesignKeyEnum.ORDER].includes(
        props.formKey
      )
    ) {
      // 报价单/合同支持多个子表格
      return props.fieldList.some((e) => e.type === FieldTypeEnum.SUB_PRODUCT);
    }
    return false;
  }

  function handleMove(e: any) {
    return (
      !getFieldDisable(e.data) &&
      (e.to.className.includes('crm-form-design-subtable-wrapper') // 子表格支持的组件类型
        ? [
            FieldTypeEnum.INPUT,
            FieldTypeEnum.INPUT_NUMBER,
            FieldTypeEnum.SELECT,
            FieldTypeEnum.SELECT_MULTIPLE,
            FieldTypeEnum.DATA_SOURCE,
            FieldTypeEnum.FORMULA,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.DATE_TIME,
            FieldTypeEnum.MEMBER,
            FieldTypeEnum.MEMBER_MULTIPLE,
            FieldTypeEnum.DEPARTMENT,
            FieldTypeEnum.DEPARTMENT_MULTIPLE,
          ].includes(e.data.type)
        : true)
    );
  }

  function clone(e: FormCreateField) {
    if ((e as any).isRef) {
      // 引用 CRM 业务字段: 携带 businessKey/resourceFieldId, 自动映射到线索对应列
      return makeRefField(e);
    }
    const res: FormCreateField = {
      ...e,
      id: getGenerateId(),
      name: t(e.name),
      isNew: true,
    };
    if (
      [FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO, FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(
        e.type
      ) &&
      e.options?.length === 0
    ) {
      res.options = [
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
      res.customOptions = [...res.options];
    }
    return cloneDeep(res);
  }

  function handleFieldClick(field: FormCreateField) {
    if (!getFieldDisable(field)) {
      emit('select', field);
    }
  }
</script>

<style lang="less" scoped>
  .crm-form-design-field-title {
    @apply font-semibold;

    margin-bottom: 16px;
    color: var(--text-n1);
  }
  .crm-form-design-field-wrapper {
    @apply grid grid-cols-2;

    gap: 12px;
    .crm-form-design-field-item {
      @apply flex cursor-move items-center;

      padding: 6px 12px;
      border: 1px solid transparent;
      border-radius: var(--border-radius-small);
      background-color: var(--text-n9);
      line-height: 22px;
      gap: 8px;
      &:hover {
        border: 1px solid var(--primary-1);
        color: var(--primary-1);
      }
    }
    .crm-form-design-field-item--ref {
      border: 1px dashed var(--primary-1);
      color: var(--primary-1);
      &:hover {
        background-color: rgb(var(--primary-1-rgb) 0.1);
      }
    }
    .crm-form-design-field-item--disabled {
      @apply cursor-not-allowed;

      color: var(--text-n6);
      &:hover {
        border: 1px solid transparent;
        color: var(--text-n6);
      }
    }
  }
</style>
