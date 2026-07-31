<template>
  <CrmPageWrapper :title="formTitle" hide-back>
    <!-- 加载中 -->
    <div v-if="loading && !fieldList.length" class="flex flex-1 items-center justify-center">
      <van-loading size="32px">{{ t('common.loading') }}</van-loading>
    </div>

    <!-- 加载失败 -->
    <div
      v-else-if="loadFailed"
      class="flex flex-1 flex-col items-center justify-center gap-[16px] px-[32px] text-center"
    >
      <van-icon name="warning-o" size="48px" color="var(--text-n4)" />
      <div class="text-[14px] text-[var(--text-n3)]">{{ failMessage }}</div>
    </div>

    <!-- 提交成功 -->
    <div
      v-else-if="submitted"
      class="flex flex-1 flex-col items-center justify-center gap-[16px] px-[32px] text-center"
    >
      <van-icon name="passed" size="56px" color="var(--primary-6)" />
      <div class="text-[16px] font-medium text-[var(--text-n1)]">{{ t('pubForm.submitSuccess') }}</div>
      <van-button type="primary" plain class="!mt-[8px]" @click="handleFillAgain">
        {{ t('pubForm.fillAgain') }}
      </van-button>
    </div>

    <!-- 表单填写 -->
    <div v-else class="flex flex-1 flex-col">
      <van-form ref="formRef" class="crm-form" required="auto">
        <van-cell-group inset>
          <template v-for="item in mobileFieldList" :key="item.id">
            <component
              :is="getItemComponent(item.type)"
              v-if="item.show !== false"
              :id="item.id"
              v-model:value="formDetail[item.id]"
              :field-config="item"
              :origin-form-detail="originFormDetail"
              :form-detail="formDetail"
              :need-init-detail="false"
              @change="($event: any) => handleFieldChange($event, item)"
            />
          </template>
        </van-cell-group>
      </van-form>
    </div>

    <template #footer>
      <van-button
        v-if="!loading && !loadFailed && !submitted"
        type="primary"
        class="!rounded-[var(--border-radius-small)] !text-[16px]"
        :loading="submitting"
        block
        @click="handleSubmit"
      >
        {{ submitting ? t('pubForm.submitting') : t('pubForm.submit') }}
      </van-button>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { showFailToast, showSuccessToast, FormInstance } from 'vant';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { setLocalStorage } from '@lib/shared/method/local-storage';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { getPublicMarketingForm, submitPublicMarketingForm } from '@/api/modules';

  import { rules } from '@cordys/web/src/components/business/crm-form-create/config';
  import type { FormCreateField, FormCreateFieldRule } from '@cordys/web/src/components/business/crm-form-create/types';

  const route = useRoute();
  const { t } = useI18n();

  const token = route.params.token as string;

  const formRef = ref<FormInstance>();
  const loading = ref(false);
  const submitting = ref(false);
  const loadFailed = ref(false);
  const failMessage = ref('');
  const submitted = ref(false);
  const formTitle = ref(t('pubForm.title'));

  const { fieldList, formDetail, originFormDetail, initFormConfig, initFormShowControl } = useFormCreateApi({
    formKey: FormDesignKeyEnum.MARKETING_FORM,
    sourceId: ref(token),
    needInitDetail: false,
  });

  const mobileFieldList = computed(() => {
    return fieldList.value.filter((item) => item.mobile !== false);
  });

  function getItemComponent(type: FieldTypeEnum) {
    if (type === FieldTypeEnum.INPUT) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.datePicker;
    }
    if (type === FieldTypeEnum.RADIO) {
      return CrmFormCreateComponents.basicComponents.radio;
    }
    if (type === FieldTypeEnum.CHECKBOX) {
      return CrmFormCreateComponents.basicComponents.checkbox;
    }
    if (type === FieldTypeEnum.SELECT) {
      return CrmFormCreateComponents.basicComponents.pick;
    }
    if (type === FieldTypeEnum.SELECT_MULTIPLE) {
      return CrmFormCreateComponents.basicComponents.multiplePick;
    }
    if (type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
    }
    if (type === FieldTypeEnum.LOCATION) {
      return CrmFormCreateComponents.advancedComponents.location;
    }
    if (type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
    if (type === FieldTypeEnum.SERIAL_NUMBER) {
      return CrmFormCreateComponents.advancedComponents.serialNumber;
    }
    if (
      [
        FieldTypeEnum.MEMBER,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DEPARTMENT,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
      ].includes(type)
    ) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (type === FieldTypeEnum.PICTURE) {
      return CrmFormCreateComponents.advancedComponents.upload;
    }
    if (type === FieldTypeEnum.LINK) {
      return CrmFormCreateComponents.advancedComponents.link;
    }
    if (type === FieldTypeEnum.ATTACHMENT) {
      return CrmFormCreateComponents.advancedComponents.file;
    }
    if (type === FieldTypeEnum.INDUSTRY) {
      return CrmFormCreateComponents.advancedComponents.industry;
    }
  }

  function handleFieldChange(_value: any, item: FormCreateField) {
    if (item.showControlRules?.length) {
      initFormShowControl();
    }
  }

  function getRuleType(item: FormCreateField) {
    if (
      item.type === FieldTypeEnum.SELECT_MULTIPLE ||
      item.type === FieldTypeEnum.CHECKBOX ||
      item.type === FieldTypeEnum.INPUT_MULTIPLE ||
      item.type === FieldTypeEnum.MEMBER_MULTIPLE ||
      item.type === FieldTypeEnum.DEPARTMENT_MULTIPLE ||
      [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type) ||
      item.type === FieldTypeEnum.PICTURE ||
      item.type === FieldTypeEnum.ATTACHMENT
    ) {
      return 'array';
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return 'date';
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return 'number';
    }
    return 'string';
  }

  function createValidatorRule(item: FormCreateField, rule: FormCreateFieldRule): FormCreateFieldRule | null {
    const staticRule: any = cloneDeep(rules.find((e) => e.key === rule.key));
    if (!staticRule) return null;

    // 公开页面不做唯一性校验 (无登录态)
    if (staticRule.key === 'unique') {
      return null;
    }

    staticRule.type = getRuleType(item);
    return staticRule;
  }

  function buildValidatorRules(item: FormCreateField) {
    const result: any[] = [];
    item.rules?.forEach((rule) => {
      const r = createValidatorRule(item, rule);
      if (r) {
        result.push(r);
      }
    });
    return result;
  }

  function buildValidatorMessages(item: FormCreateField) {
    const messages: Record<string, string> = {};
    item.rules?.forEach((rule) => {
      if (rule.key === 'required' && item.name) {
        messages.required = t('common.notNull', { value: item.name });
      }
    });
    return messages;
  }

  async function handleSubmit() {
    try {
      // 手动校验必填项 (van-form validate)
      await formRef.value?.validate();

      submitting.value = true;

      // 构建 moduleFields (复用 saveForm 的字段收集逻辑)
      const result = cloneDeep(formDetail.value);
      mobileFieldList.value.forEach((item) => {
        if (item.type === FieldTypeEnum.DATA_SOURCE && Array.isArray(result[item.id])) {
          result[item.id] = result[item.id]?.[0];
        }
        if (item.type === FieldTypeEnum.PHONE) {
          result[item.id] = result[item.id]?.replace(/[\s\uFEFF\xA0]+/g, '');
        }
      });

      const moduleFields: { fieldId: string; fieldValue: any }[] = [];
      fieldList.value.forEach((item) => {
        moduleFields.push({
          fieldId: item.id,
          fieldValue: result[item.id],
        });
      });

      await submitPublicMarketingForm(token, { moduleFields });
      submitted.value = true;
      showSuccessToast(t('pubForm.submitSuccess'));
    } catch (error: any) {
      console.log(error);
      if (error?.message) {
        showFailToast(error.message);
      }
    } finally {
      submitting.value = false;
    }
  }

  function handleFillAgain() {
    submitted.value = false;
    // 重置表单
    formDetail.value = {};
    Object.keys(formDetail.value).forEach((key) => {
      formDetail.value[key] = '';
    });
  }

  async function loadForm() {
    try {
      loading.value = true;
      loadFailed.value = false;
      await initFormConfig();
      // 保存 orgId 到 localStorage, 供后续数据源请求 (产品/客户等下拉选项) 的 Organization-Id 请求头使用
      try {
        const config = await getPublicMarketingForm(token);
        if (config?.organizationId) {
          setLocalStorage('app', { orgId: config.organizationId });
        }
      } catch {
        // 忽略 orgId 获取失败
      }
      // 设置表单标题
      if (fieldList.value.length === 0) {
        loadFailed.value = true;
        failMessage.value = t('pubForm.loadFailed');
      }
    } catch (error) {
      console.log(error);
      loadFailed.value = true;
      failMessage.value = t('pubForm.loadFailed');
    } finally {
      loading.value = false;
    }
  }

  onBeforeMount(() => {
    if (!token) {
      loadFailed.value = true;
      failMessage.value = t('pubForm.loadFailed');
      return;
    }
    loadForm();
  });
</script>

<style lang="less" scoped>
  .crm-form {
    :deep(.van-cell-group--inset) {
      margin: 12px;
    }
  }
</style>
