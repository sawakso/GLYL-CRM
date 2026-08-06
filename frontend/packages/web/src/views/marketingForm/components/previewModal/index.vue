<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="t('marketingForm.preview')"
    class="w-[1100px]"
    :bordered="false"
    :mask-closable="false"
    style="max-height: 85vh"
  >
    <div class="preview-modal-body">
      <div class="preview-header mb-[16px] flex items-center justify-between">
        <div class="text-[16px] font-medium">{{ formName }}</div>
        <div class="flex items-center gap-[8px]">
          <n-tag size="small" type="info" bordered>
            {{ t('marketingForm.previewTip') }}
          </n-tag>
        </div>
      </div>

      <n-spin :show="loading">
        <n-alert v-if="errorMsg" type="warning" class="mb-[12px]">
          {{ errorMsg }}
        </n-alert>
        <template v-else>
          <n-form
            ref="formRef"
            :model="formDetail"
            :label-placement="formProp?.labelPos || 'top'"
            :require-mark-placement="formProp?.labelPos === 'left' ? 'left' : 'right'"
            label-width="auto"
          >
            <!-- 左右双栏预览: 左 PC / 右 手机 -->
            <div class="preview-split">
              <!-- PC 预览 -->
              <div class="preview-column preview-column--pc">
                <div class="preview-column-title">{{ t('marketingForm.previewPc') }}</div>
                <n-scrollbar class="preview-scroll" style="max-height: calc(85vh - 240px)">
                  <div class="flex w-full flex-wrap content-start">
                    <template v-for="item in fieldList" :key="item.id">
                      <div
                        v-if="item.show !== false && item.readable"
                        class="preview-form-item"
                        :style="{
                          width: item.type === FieldTypeEnum.ATTACHMENT ? '100%' : `${(item.fieldWidth || 1) * 100}%`,
                        }"
                      >
                        <component
                          :is="getItemComponent(item)"
                          :id="item.id"
                          v-model:value="formDetail[item.id]"
                          :field-config="item"
                          :form-detail="formDetail"
                          :origin-form-detail="originFormDetail"
                          :need-init-detail="false"
                          :form-config="formProp"
                          :path="item.id"
                          @change="() => {}"
                        />
                      </div>
                    </template>
                  </div>
                </n-scrollbar>
              </div>

              <!-- 手机预览 -->
              <div class="preview-column preview-column--mobile">
                <div class="preview-column-title">{{ t('marketingForm.previewMobile') }}</div>
                <div class="preview-phone">
                  <n-scrollbar class="preview-scroll" style="max-height: calc(85vh - 300px)">
                    <div class="preview-phone-body">
                      <template v-for="item in fieldList" :key="item.id">
                        <div v-if="item.show !== false && item.readable" class="preview-phone-item">
                          <component
                            :is="getItemComponent(item)"
                            :id="item.id"
                            v-model:value="formDetail[item.id]"
                            :field-config="item"
                            :form-detail="formDetail"
                            :origin-form-detail="originFormDetail"
                            :need-init-detail="false"
                            :form-config="formProp"
                            :path="item.id"
                            @change="() => {}"
                          />
                        </div>
                      </template>
                    </div>
                  </n-scrollbar>
                </div>
              </div>
            </div>
          </n-form>
        </template>
      </n-spin>
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-[8px]">
        <n-button secondary @click="handleClose">
          {{ t('common.close') }}
        </n-button>
        <n-button type="primary" :loading="submitting" @click="handlePreviewSubmit">
          {{ t('marketingForm.previewSubmit') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NModal, NScrollbar, NSpin, NTag, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getMarketingFormDetail } from '@/api/modules';

  const props = defineProps<{
    formId?: string;
    formName?: string;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const loading = ref(false);
  const submitting = ref(false);
  const errorMsg = ref('');
  const formRef = ref<FormInst | null>(null);
  const fieldList = ref<FormCreateField[]>([]);
  const formProp = ref<FormConfig | null>(null);
  const formDetail = ref<Record<string, any>>({});
  const originFormDetail = ref<Record<string, any>>({});

  function getItemComponent(item: FormCreateField) {
    if (item.type === FieldTypeEnum.INPUT || item.resourceFieldId) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (item.type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.dateTime;
    }
    if (item.type === FieldTypeEnum.RADIO) {
      return CrmFormCreateComponents.basicComponents.radio;
    }
    if (item.type === FieldTypeEnum.CHECKBOX) {
      return CrmFormCreateComponents.basicComponents.checkbox;
    }
    if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.basicComponents.select;
    }
    if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if ([FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (item.type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
    }
    if (item.type === FieldTypeEnum.INPUT_MULTIPLE) {
      return CrmFormCreateComponents.basicComponents.tagInput;
    }
    if (item.type === FieldTypeEnum.PICTURE) {
      return CrmFormCreateComponents.advancedComponents.upload;
    }
    if (item.type === FieldTypeEnum.LOCATION) {
      return CrmFormCreateComponents.advancedComponents.location;
    }
    if (item.type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
    if (item.type === FieldTypeEnum.SERIAL_NUMBER) {
      return CrmFormCreateComponents.advancedComponents.serialNumber;
    }
    if (item.type === FieldTypeEnum.LINK) {
      return CrmFormCreateComponents.advancedComponents.link;
    }
    if (item.type === FieldTypeEnum.ATTACHMENT) {
      return CrmFormCreateComponents.advancedComponents.file;
    }
    if (item.type === FieldTypeEnum.INDUSTRY) {
      return CrmFormCreateComponents.advancedComponents.industry;
    }
    if (item.type === FieldTypeEnum.FORMULA) {
      return CrmFormCreateComponents.advancedComponents.formula;
    }
    return CrmFormCreateComponents.basicComponents.singleText;
  }

  function initFieldDefault(item: FormCreateField): any {
    if (item.defaultValue !== undefined && item.defaultValue !== null) {
      return cloneDeep(item.defaultValue);
    }
    switch (item.type) {
      case FieldTypeEnum.DATA_SOURCE:
      case FieldTypeEnum.DATA_SOURCE_MULTIPLE:
      case FieldTypeEnum.MEMBER_MULTIPLE:
      case FieldTypeEnum.DEPARTMENT_MULTIPLE:
      case FieldTypeEnum.SELECT_MULTIPLE:
      case FieldTypeEnum.CHECKBOX:
      case FieldTypeEnum.INPUT_MULTIPLE:
      case FieldTypeEnum.PICTURE:
      case FieldTypeEnum.ATTACHMENT:
        return [];
      case FieldTypeEnum.INPUT_NUMBER:
      case FieldTypeEnum.DATE_TIME:
        return null;
      default:
        return '';
    }
  }

  async function loadForm() {
    if (!props.formId) {
      errorMsg.value = t('marketingForm.loadFailed');
      return;
    }
    try {
      loading.value = true;
      errorMsg.value = '';
      const detail = await getMarketingFormDetail(props.formId);
      fieldList.value = detail.fields || [];
      formProp.value = detail.formProp || null;
      formDetail.value = {};
      fieldList.value.forEach((item) => {
        formDetail.value[item.id] = initFieldDefault(item);
      });
      originFormDetail.value = cloneDeep(formDetail.value);
    } catch (error) {
      console.error('[PreviewModal] load form failed:', error);
      errorMsg.value = t('marketingForm.loadFailed');
    } finally {
      loading.value = false;
    }
  }

  function handlePreviewSubmit() {
    // 预览模式: 仅本地校验必填项, 不真正提交到后端
    formRef.value?.validate((errors) => {
      if (errors) {
        Message.warning(t('marketingForm.previewValidateFailed'));
        return;
      }
      submitting.value = true;
      setTimeout(() => {
        submitting.value = false;
        Message.success(t('marketingForm.previewSuccess'));
      }, 400);
    });
  }

  function handleClose() {
    visible.value = false;
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        loadForm();
      }
    }
  );
</script>

<style lang="less" scoped>
  .preview-modal-body {
    min-height: 300px;
    .preview-split {
      @apply flex;

      gap: 16px;
    }
    .preview-column {
      @apply min-w-0 flex-1;

      padding: 12px;
      border: 1px solid var(--text-n7);
      border-radius: var(--border-radius);
      background-color: var(--text-n9);
      &--pc {
        flex: 1 1 60%;
      }
      &--mobile {
        flex: 0 0 375px;
      }
      .preview-column-title {
        margin-bottom: 12px;
        font-size: 13px;
        color: var(--text-n2);
        @apply text-center font-medium;
      }
    }
    .preview-scroll {
      @apply w-full;
    }
    .preview-form-item {
      @apply relative self-start;

      padding: 0 12px;
    }
    // 手机屏幕样式
    .preview-phone {
      overflow: hidden;
      margin: 0 auto;
      width: 100%;
      max-width: 360px;
      border: 6px solid var(--text-n4);
      border-radius: 24px;
      background-color: #ffffff;
      .preview-phone-body {
        @apply flex flex-col;

        padding: 16px 12px;
        .preview-phone-item {
          margin-bottom: 14px;
          width: 100%;
        }
      }
    }
  }
</style>
