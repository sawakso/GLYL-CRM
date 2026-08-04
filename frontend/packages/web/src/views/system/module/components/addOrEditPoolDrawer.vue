<template>
  <CrmDrawer
    v-model:show="visible"
    :width="900"
    :title="title"
    :show-continue="!form.id"
    :ok-text="form.id ? t('common.update') : undefined"
    :loading="loading"
    @confirm="confirmHandler(false)"
    @continue="confirmHandler(true)"
    @cancel="cancelHandler"
  >
    <n-alert v-if="form.id" class="mb-[16px]" type="warning">
      {{ t('module.clue.updateConfirmContent') }}
    </n-alert>

    <n-tabs v-model:value="tabName" type="line" animated>
      <!-- Tab 1: 基础信息 -->
      <n-tab-pane :name="'baseInfo'" :tab="t('module.clue.baseInfo')">
        <n-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="left"
          :label-width="110"
          require-mark-placement="left"
        >
          <div class="w-full">
            <n-form-item
              path="name"
              :label="
                props.type === ModuleConfigEnum.CLUE_MANAGEMENT
                  ? t('module.clue.name')
                  : t('module.customer.openSeaName')
              "
            >
              <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
            </n-form-item>
            <n-form-item v-if="isCluePool" path="description" :label="t('module.clue.description')">
              <n-input
                v-model:value="form.description"
                :maxlength="1000"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                :placeholder="t('common.pleaseInput')"
              />
            </n-form-item>
          </div>
          <div class="flex gap-[16px]">
            <div class="flex-1">
              <n-form-item path="adminIds" :label="t('opportunity.admin')">
                <CrmUserTagSelector v-model:selected-list="form.adminIds" />
              </n-form-item>
            </div>
            <div v-if="isCluePool" class="flex-1">
              <n-form-item path="collaboratorIds" :label="t('module.clue.collaboratorAdmin')">
                <CrmUserTagSelector v-model:selected-list="form.collaboratorIds" />
              </n-form-item>
            </div>
          </div>
          <n-form-item path="userIds" :label="t('role.member')">
            <CrmUserTagSelector v-model:selected-list="form.userIds" />
          </n-form-item>
          <!-- 分配领取规则(仅线索池) -->
          <n-form-item v-if="isCluePool" path="pickMode" :label="t('module.clue.pickMode')">
            <n-radio-group v-model:value="form.pickMode" name="pickModeGroup">
              <n-space>
                <n-radio value="VISIBLE_PICKABLE">
                  {{ t('module.clue.pickModeVisible') }}
                </n-radio>
                <n-radio value="ADMIN_ASSIGN_ONLY">
                  {{ t('module.clue.pickModeAdminOnly') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
        </n-form>
      </n-tab-pane>

      <!-- Tab 2: 规则设置 -->
      <n-tab-pane :name="'ruleSetting'" :tab="t('module.clue.ruleSetting')">
        <n-form :model="form" label-placement="left" :label-width="110" require-mark-placement="left">
          <!-- 新线索提醒(仅线索池) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.newLeadRemind') }}</div>
            <n-form-item path="newLeadRemind">
              <n-switch v-model:value="form.newLeadRemind" />
              <span class="ml-[8px] text-[12px] text-[var(--text-n5)]">{{ t('module.clue.newLeadRemindTip') }}</span>
            </n-form-item>
          </template>

          <!-- 线索领取规则 -->
          <div class="crm-module-form-title">
            {{
              props.type === ModuleConfigEnum.CLUE_MANAGEMENT
                ? t('module.clue.clueCollectionRules')
                : t('module.customer.customerCollectionRules')
            }}
          </div>
          <n-form-item path="pickRule.limitOnNumber" :label="t('module.clue.dailyCollection')">
            <n-radio-group v-model:value="form.pickRule.limitOnNumber" name="radiogroup">
              <n-space>
                <n-radio :value="false">
                  {{ t('module.clue.noLimit') }}
                </n-radio>
                <n-radio :value="true">
                  {{ t('module.clue.limit') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          <n-form-item
            v-if="form.pickRule.limitOnNumber"
            path="pickRule.pickNumber"
            :label="t('module.clue.limitQuantity')"
          >
            <CrmInputNumber
              v-model:value="form.pickRule.pickNumber"
              class="crm-reminder-advance-input"
              :placeholder="t('common.pleaseInput')"
              min="1"
              max="10000"
              :precision="0"
            />
          </n-form-item>
          <n-form-item path="pickRule.limitPreOwner" :label="t('module.clue.ownerCollection')">
            <n-radio-group v-model:value="form.pickRule.limitPreOwner" name="radiogroup">
              <n-space>
                <n-radio :value="false">
                  {{ t('module.clue.noLimit') }}
                </n-radio>
                <n-radio :value="true">
                  {{ t('module.clue.limit') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          <n-form-item
            v-if="form.pickRule.limitPreOwner"
            path="pickRule.pickIntervalDays"
            :label="t('module.clue.formerOwner')"
          >
            <CrmInputNumber
              v-model:value="form.pickRule.pickIntervalDays"
              class="crm-reminder-advance-input"
              :placeholder="t('common.pleaseInput')"
              min="1"
              max="10000"
              :precision="0"
            />
            <div class="flex flex-nowrap"> {{ t('module.clue.receiveDay') }}</div>
          </n-form-item>
          <n-form-item path="pickRule.limitNew">
            <template #label>
              <div class="flex items-center gap-[8px]">
                {{ t('module.clue.newDataPick') }}
                <n-tooltip trigger="hover" placement="right">
                  <template #trigger>
                    <CrmIcon
                      type="iconicon_help_circle"
                      :size="16"
                      class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                    />
                  </template>
                  {{
                    props.type === ModuleConfigEnum.CLUE_MANAGEMENT
                      ? t('module.clue.newPoolDataTip')
                      : t('module.clue.newOpenSeaDataTip')
                  }}
                </n-tooltip>
              </div>
            </template>
            <n-radio-group v-model:value="form.pickRule.limitNew" name="radiogroup">
              <n-space>
                <n-radio :value="false">
                  {{ t('module.clue.noLimit') }}
                </n-radio>
                <n-radio :value="true">
                  {{ t('module.clue.limit') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          <n-form-item v-if="form.pickRule.limitNew" path="pickRule.newPickInterval" :label="t('module.clue.newData')">
            <CrmInputNumber
              v-model:value="form.pickRule.newPickInterval"
              class="crm-reminder-advance-input"
              :placeholder="t('common.pleaseInput')"
              min="1"
              max="10000"
              :precision="0"
            />
            <div class="flex flex-nowrap"> {{ t('module.clue.receiveDay') }}</div>
          </n-form-item>

          <!-- 表单回流去重(仅线索池, 市场表单未单独配置时使用的默认策略) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.formDedup') }}</div>
            <div class="mb-[8px] text-[12px] text-[var(--text-n5)]">{{ t('module.clue.formDedupTip') }}</div>
            <n-form-item path="dedupStrategy" :label="t('module.clue.dedupStrategy')">
              <n-select
                v-model:value="form.dedupStrategy"
                :options="dedupStrategyOptions"
                class="w-[360px]"
                size="small"
              />
            </n-form-item>
            <n-form-item path="dedupWindow" :label="t('module.clue.dedupWindow')">
              <div class="flex items-center gap-[8px]">
                <CrmInputNumber
                  v-model:value="form.dedupWindow"
                  class="w-[160px]"
                  min="0"
                  max="10080"
                  :precision="0"
                  :disabled="form.dedupStrategy === 'NONE'"
                />
                <span class="text-[12px] text-[var(--text-n5)]">{{ t('module.clue.dedupWindowTip') }}</span>
              </div>
            </n-form-item>
            <n-form-item path="dedupKey" :label="t('module.clue.dedupKey')">
              <n-select
                v-model:value="form.dedupKey"
                :options="dedupKeyOptions"
                class="w-[360px]"
                size="small"
                :disabled="form.dedupStrategy === 'NONE'"
              />
            </n-form-item>
          </template>

          <!-- 定时自动分配(仅线索池) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.autoAssign') }}</div>
            <n-form-item path="autoAssignEnabled" :label="t('module.clue.autoAssignEnable')">
              <div class="flex items-center gap-[12px]">
                <n-switch v-model:value="form.autoAssignEnabled" />
                <span class="text-[12px] text-[var(--text-n5)]">{{ t('module.clue.autoAssignEnableTip') }}</span>
              </div>
            </n-form-item>
            <n-form-item
              v-if="form.autoAssignEnabled"
              path="autoAssignCron"
              :label="t('module.clue.autoAssignCron')"
            >
              <div class="flex w-full flex-col gap-[8px]">
                <n-input
                  v-model:value="form.autoAssignCron"
                  :placeholder="t('module.clue.autoAssignCronPlaceholder')"
                  class="w-[320px]"
                  size="small"
                />
                <n-space :size="[8, 8]">
                  <n-tag
                    v-for="item in cronQuickOptions"
                    :key="item.value"
                    :bordered="true"
                    class="cursor-pointer"
                    size="small"
                    @click="form.autoAssignCron = item.value"
                  >
                    {{ item.label }}
                  </n-tag>
                </n-space>
              </div>
            </n-form-item>
          </template>

          <!-- 线索池分配规则(仅线索池) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.assignRule') }}</div>
            <assignRuleConfig v-model="form.assignRules" :pool-id="form.id" />
          </template>

          <!-- 超时提醒(仅线索池) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.timeoutRemind') }}</div>
            <n-form-item path="unassignedReminderMinutes" :label="t('module.clue.unassignedReminder')">
              <div class="flex items-center gap-[8px]">
                <CrmInputNumber
                  v-model:value="unassignedReminderHours"
                  class="w-[120px]"
                  min="0"
                  max="8760"
                  :precision="0"
                />
                <span>{{ t('module.clue.hour') }}</span>
                <CrmInputNumber
                  v-model:value="unassignedReminderMinutePart"
                  class="w-[120px]"
                  min="0"
                  max="59"
                  :precision="0"
                />
                <span>{{ t('module.clue.minute') }}</span>
              </div>
            </n-form-item>
            <n-form-item path="unfollowedReminderMinutes" :label="t('module.clue.unfollowedReminder')">
              <div class="flex items-center gap-[8px]">
                <CrmInputNumber
                  v-model:value="unfollowedReminderHours"
                  class="w-[120px]"
                  min="0"
                  max="8760"
                  :precision="0"
                />
                <span>{{ t('module.clue.hour') }}</span>
                <CrmInputNumber
                  v-model:value="unfollowedReminderMinutePart"
                  class="w-[120px]"
                  min="0"
                  max="59"
                  :precision="0"
                />
                <span>{{ t('module.clue.minute') }}</span>
              </div>
            </n-form-item>
            <n-form-item path="notifyPoolAdminOnUnfollowedTimeout">
              <n-checkbox v-model:checked="form.notifyPoolAdminOnUnfollowedTimeout">
                {{ t('module.clue.notifyPoolAdminOnUnfollowedTimeout') }}
              </n-checkbox>
            </n-form-item>
          </template>

          <!-- 回收规则 -->
          <div class="crm-module-form-title">
            {{
              props.type === ModuleConfigEnum.CLUE_MANAGEMENT
                ? t('module.clue.clueRecycleRule')
                : t('module.customer.customerRecycleRule')
            }}
          </div>
          <n-form-item path="auto" :label="t('module.clue.autoRecycle')">
            <n-radio-group v-model:value="form.auto" name="radiogroup">
              <n-space>
                <n-radio :value="true">
                  {{ t('common.yes') }}
                </n-radio>
                <n-radio :value="false">
                  {{ t('common.no') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          <FilterContent
            v-if="form.auto"
            ref="filterContentRef"
            v-model:form-model="recycleFormItemModel as FilterForm"
            keep-one-line
            :config-list="filterConfigList"
          />

          <!-- 转移退回规则(仅线索池) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.transferReturn') }}</div>
            <div class="flex flex-col gap-[12px]">
              <n-checkbox v-model:checked="form.allowTransferAfterPick">
                {{ t('module.clue.allowTransferAfterPick') }}
              </n-checkbox>
              <n-checkbox v-model:checked="form.restrictTransferInToMembers">
                {{ t('module.clue.restrictTransferInToMembers') }}
              </n-checkbox>
              <n-checkbox v-model:checked="form.restrictReturnToMembers">
                {{ t('module.clue.restrictReturnToMembers') }}
              </n-checkbox>
            </div>
          </template>

          <!-- 清空规则(仅线索池) -->
          <template v-if="isCluePool">
            <div class="crm-module-form-title">{{ t('module.clue.clearRule') }}</div>
            <div class="flex flex-col gap-[12px]">
              <n-checkbox v-model:checked="form.clearTeamOnOwnerChange">
                {{ t('module.clue.clearTeamOnOwnerChange') }}
              </n-checkbox>
              <n-checkbox v-model:checked="form.clearExternalOwnerOnOwnerEmpty">
                {{ t('module.clue.clearExternalOwnerOnOwnerEmpty') }}
              </n-checkbox>
              <n-checkbox v-model:checked="form.clearExternalTeamOnExternalOwnerEmpty">
                {{ t('module.clue.clearExternalTeamOnExternalOwnerEmpty') }}
              </n-checkbox>
              <n-checkbox v-model:checked="form.clearOwnerOnPoolTransfer">
                {{ t('module.clue.clearOwnerOnPoolTransfer') }}
              </n-checkbox>
              <n-checkbox v-model:checked="form.clearExternalOwnerOnPoolTransfer">
                {{ t('module.clue.clearExternalOwnerOnPoolTransfer') }}
              </n-checkbox>
            </div>
          </template>
        </n-form>
      </n-tab-pane>

      <!-- Tab 3: 显示设置 -->
      <n-tab-pane :name="'displaySetting'" :tab="t('module.clue.displaySetting')">
        <div class="crm-module-form-title">{{ t('module.clue.fieldPermissionSetting') }}</div>
        <p class="mb-[12px] text-[12px] text-[var(--text-n5)]">{{ t('module.clue.fieldPermissionTip') }}</p>
        <n-transfer
          v-model:value="showFieldIds"
          :options="fieldTransferOptions"
          :source-title="t('module.clue.privateFields')"
          :target-title="t('module.clue.publicFields')"
          source-filterable
          target-filterable
          class="w-full"
        />
        <template v-if="isCluePool">
          <div class="crm-module-form-title">{{ t('module.clue.otherPermissions') }}</div>
          <div class="flex flex-col gap-[12px]">
            <n-checkbox v-model:checked="form.allowViewChangeLogBeforePick">
              {{ t('module.clue.allowViewChangeLogBeforePick') }}
            </n-checkbox>
            <n-checkbox v-model:checked="form.allowEditTeamBeforePick">
              {{ t('module.clue.allowEditTeamBeforePick') }}
            </n-checkbox>
            <n-checkbox v-model:checked="form.allowSendSalesRecordBeforePick">
              {{ t('module.clue.allowSendSalesRecordBeforePick') }}
            </n-checkbox>
            <n-checkbox v-model:checked="form.allowViewSalesRecordBeforePick">
              {{ t('module.clue.allowViewSalesRecordBeforePick') }}
            </n-checkbox>
            <n-checkbox v-model:checked="form.allowViewPoolLog">
              {{ t('module.clue.allowViewPoolLog') }}
            </n-checkbox>
          </div>
        </template>
      </n-tab-pane>

      <!-- Tab 4: 其他(仅编辑模式) -->
      <n-tab-pane v-if="form.id" :name="'otherSetting'" :tab="t('module.clue.otherSetting')">
        <n-form label-placement="left" :label-width="110">
          <n-form-item v-if="isCluePool" :label="t('module.clue.currentClueCount')">
            <span>{{ currentClueCount ?? 0 }}</span>
          </n-form-item>
          <n-form-item :label="t('module.clue.lastUpdateTime')">
            <span>{{ form.updateTime ? formatTimestamp(form.updateTime) : '-' }}</span>
          </n-form-item>
        </n-form>
      </n-tab-pane>
    </n-tabs>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import {
    FormInst,
    FormRules,
    NAlert,
    NCheckbox,
    NForm,
    NFormItem,
    NInput,
    NRadio,
    NRadioGroup,
    NSpace,
    NSwitch,
    NTabPane,
    NTabs,
    NTooltip,
    NTransfer,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    CluePoolForm,
    CluePoolItem,
    CluePoolParams,
    ModuleConditionsItem,
  } from '@lib/shared/models/system/module';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { DYNAMICS, FIXED } from '@/components/pure/crm-advance-filter/index';
  import { AccordBelowType, FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';
  import assignRuleConfig from './assignRuleConfig.vue';

  import { addCluePool, addCustomerPool, updateCluePool, updateCustomerPool } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    type: ModuleConfigEnum;
    quick?: boolean;
    row?: CluePoolItem;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'saved'): void;
  }>();

  const isCluePool = computed(() => props.type === ModuleConfigEnum.CLUE_MANAGEMENT);

  const tabName = ref('baseInfo');
  const formKey = computed(() => {
    return props.type === ModuleConfigEnum.CLUE_MANAGEMENT
      ? FormDesignKeyEnum.CLUE_POOL
      : FormDesignKeyEnum.CUSTOMER_OPEN_SEA;
  });
  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey,
  });
  const showInTableColumns = computed(() => {
    return fieldList.value.filter(
      (item) => ![FieldTypeEnum.DIVIDER, FieldTypeEnum.TEXTAREA].includes(item.type) && item.businessKey !== 'owner'
    );
  });
  const fieldTransferOptions = computed(() =>
    showInTableColumns.value.map((item) => ({
      label: item.name,
      value: item.id,
      disabled: item.businessKey === 'name',
    }))
  );
  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', {
          value: `${
            props.type === ModuleConfigEnum.CLUE_MANAGEMENT ? t('module.clue.name') : t('module.customer.openSeaName')
          }`,
        }),
        trigger: ['input', 'blur'],
      },
    ],
    adminIds: [{ required: true, message: t('common.pleaseSelect') }],
    userIds: [{ required: true, message: t('common.pleaseSelect') }],
    // 仅在对应开关开启时才必填,否则编辑已关闭开关的池会被校验拦截导致无法保存
    [`pickRule.pickIntervalDays`]: [
      {
        validator: (_rule, value) =>
          !form.value.pickRule.limitPreOwner || (typeof value === 'number' && !Number.isNaN(value))
            ? Promise.resolve()
            : Promise.reject(t('common.pleaseInput')),
        trigger: ['input', 'blur'],
      },
    ],
    [`pickRule.pickNumber`]: [
      {
        validator: (_rule, value) =>
          !form.value.pickRule.limitOnNumber || (typeof value === 'number' && !Number.isNaN(value))
            ? Promise.resolve()
            : Promise.reject(t('common.pleaseInput')),
        trigger: ['input', 'blur'],
      },
    ],
    [`pickRule.newPickInterval`]: [
      {
        validator: (_rule, value) =>
          !form.value.pickRule.limitNew || (typeof value === 'number' && !Number.isNaN(value))
            ? Promise.resolve()
            : Promise.reject(t('common.pleaseInput')),
        trigger: ['input', 'blur'],
      },
    ],
  };

  const initForm: CluePoolForm = {
    name: '',
    adminIds: [],
    collaboratorIds: [],
    description: '',
    userIds: [],
    enable: true,
    auto: false,
    pickMode: 'VISIBLE_PICKABLE',
    newLeadRemind: false,
    unassignedReminderMinutes: 1440,
    unfollowedReminderMinutes: 2880,
    notifyPoolAdminOnUnfollowedTimeout: false,
    allowTransferAfterPick: false,
    restrictTransferInToMembers: false,
    restrictReturnToMembers: false,
    clearTeamOnOwnerChange: false,
    clearExternalOwnerOnOwnerEmpty: false,
    clearExternalTeamOnExternalOwnerEmpty: false,
    clearOwnerOnPoolTransfer: false,
    clearExternalOwnerOnPoolTransfer: false,
    allowViewChangeLogBeforePick: false,
    allowEditTeamBeforePick: false,
    allowSendSalesRecordBeforePick: false,
    allowViewSalesRecordBeforePick: false,
    allowViewPoolLog: false,
    pickRule: {
      limitOnNumber: false,
      pickNumber: undefined,
      limitPreOwner: false,
      pickIntervalDays: undefined,
      limitNew: false,
      newPickInterval: undefined,
    },
    recycleRule: {
      operator: 'all',
      conditions: [],
    },
    assignRules: [],
    hiddenFieldIds: [],
    autoAssignEnabled: false,
    autoAssignCron: '',
    dedupStrategy: 'UPDATE',
    dedupWindow: 5,
    dedupKey: 'AUTO',
  };
  const showFieldIds = ref<string[]>([]);
  const form = ref<CluePoolForm>(cloneDeep(initForm));
  const currentClueCount = ref<number>(0);

  type ReminderField = 'unassignedReminderMinutes' | 'unfollowedReminderMinutes';
  type ReminderPart = 'hour' | 'minute';

  function createReminderPartModel(field: ReminderField, part: ReminderPart) {
    return computed({
      get: () => {
        const totalMinutes = Math.max(0, form.value[field] ?? 0);
        return part === 'hour' ? Math.floor(totalMinutes / 60) : totalMinutes % 60;
      },
      set: (value: number) => {
        const normalizedValue = Number.isFinite(Number(value)) ? Math.max(0, Math.floor(Number(value))) : 0;
        const currentTotal = Math.max(0, form.value[field] ?? 0);
        form.value[field] =
          part === 'hour'
            ? normalizedValue * 60 + (currentTotal % 60)
            : Math.floor(currentTotal / 60) * 60 + Math.min(normalizedValue, 59);
      },
    });
  }

  const unassignedReminderHours = createReminderPartModel('unassignedReminderMinutes', 'hour');
  const unassignedReminderMinutePart = createReminderPartModel('unassignedReminderMinutes', 'minute');
  const unfollowedReminderHours = createReminderPartModel('unfollowedReminderMinutes', 'hour');
  const unfollowedReminderMinutePart = createReminderPartModel('unfollowedReminderMinutes', 'minute');

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [
      {
        dataIndex: 'storageTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
        operator: OperatorEnum.DYNAMICS,
        showScope: true,
        scope: ['Created', 'Picked'],
      },
    ],
  };
  const recycleFormItemModel = ref<FilterForm>(cloneDeep(defaultFormModel));

  const cronQuickOptions = [
    { label: t('module.clue.cronEvery30Min'), value: '0 0/30 * * * ?' },
    { label: t('module.clue.cronEveryHour'), value: '0 0 * * * ?' },
    { label: t('module.clue.cronDaily9'), value: '0 0 9 * * ?' },
    { label: t('module.clue.cronDaily12'), value: '0 0 12 * * ?' },
    { label: t('module.clue.cronDaily20'), value: '0 0 20 * * ?' },
    { label: t('module.clue.cronEvery5Min'), value: '0 0/5 * * * ?' },
  ];

  const dedupStrategyOptions = [
    { label: t('module.clue.dedupStrategyNone'), value: 'NONE' },
    { label: t('module.clue.dedupStrategyUpdate'), value: 'UPDATE' },
    { label: t('module.clue.dedupStrategySkip'), value: 'SKIP' },
    { label: t('module.clue.dedupStrategyMark'), value: 'MARK' },
  ];

  const dedupKeyOptions = [
    { label: t('module.clue.dedupKeyAuto'), value: 'AUTO' },
    { label: t('module.clue.dedupKeyPhone'), value: 'PHONE' },
    { label: t('module.clue.dedupKeyDevice'), value: 'DEVICE' },
    { label: t('module.clue.dedupKeyIp'), value: 'IP' },
  ];

  const title = computed(() => {
    if (props.type === ModuleConfigEnum.CLUE_MANAGEMENT) {
      return !form.value.id ? t('module.clue.addCluePool') : t('module.clue.updateCluePool');
    }
    if (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT) {
      return !form.value.id ? t('module.customer.addOpenSea') : t('module.customer.updateOpenSea');
    }
  });

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('module.clue.storageTime'),
        dataIndex: 'storageTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
        operatorOption: [DYNAMICS, FIXED],
        showScope: true,
        scope: ['Created', 'Picked'],
      },
      {
        title: t('module.clue.followUpTime'),
        dataIndex: 'followUpTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
        operatorOption: [DYNAMICS, FIXED],
      },
    ];
  });

  function formatTimestamp(ts: number): string {
    if (!ts) return '-';
    const d = new Date(ts);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(
      2,
      '0'
    )} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  function cancelHandler() {
    form.value = cloneDeep(initForm);
    recycleFormItemModel.value = cloneDeep(defaultFormModel);
    currentClueCount.value = 0;
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      const { userIds, auto, adminIds, collaboratorIds, updateTime: _updateTime, ...otherParams } = form.value;

      const params: CluePoolParams = {
        ...otherParams,
        ownerIds: adminIds.map((e) => e.id),
        collaboratorIds: collaboratorIds.map((e) => e.id),
        scopeIds: userIds.map((e) => e.id),
        auto,
        recycleRule: {
          operator: recycleFormItemModel.value.searchMode as string,
          conditions: [],
        },
        hiddenFieldIds: showInTableColumns.value
          .filter((item) => !showFieldIds.value.includes(item.id))
          .map((item) => item.id),
      };
      if (auto) {
        const conditions: ModuleConditionsItem[] = [];
        recycleFormItemModel.value.list?.forEach((item) => {
          conditions.push({
            column: item.dataIndex || '',
            operator: item.operator || '',
            value: item.value,
            scope: item.scope,
          });
        });
        params.recycleRule.conditions = form.value.auto ? conditions : [];
      }
      if (form.value.id) {
        await (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT
          ? updateCustomerPool(params, props.quick)
          : updateCluePool(params, props.quick));
        Message.success(t('common.updateSuccess'));
        emit('saved');
      } else {
        await (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT ? addCustomerPool(params) : addCluePool(params));
        Message.success(t('common.addSuccess'));
        emit('refresh');
      }
      if (isContinue) {
        form.value = cloneDeep(initForm);
        recycleFormItemModel.value = cloneDeep(defaultFormModel);
      } else {
        cancelHandler();
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || t('common.saveFailed');
      Message.error(msg);
      // eslint-disable-next-line no-console
      console.error(e);
    } finally {
      loading.value = false;
    }
  }

  const filterContentRef = ref<InstanceType<typeof FilterContent>>();
  async function confirmHandler(isContinue: boolean) {
    try {
      // 1. 验证主表单 (baseInfo tab 的 n-form)
      if (formRef.value) {
        const errors = await formRef.value.validate();
        if (errors && Object.keys(errors).length > 0) {
          Message.error(t('module.clue.formValidateError'));
          return;
        }
      }
      // 2. 验证回收规则 FilterContent (仅在自动回收开启时)
      if (filterContentRef.value?.formRef) {
        try {
          await new Promise<void>((resolve, reject) => {
            filterContentRef.value!.formRef!.validate((errs: any) => {
              if (errs) reject(new Error('filter validate failed'));
              else resolve();
            });
          });
        } catch {
          Message.error(t('module.clue.formValidateError'));
          return;
        }
      }
      // 3. 所有验证通过，执行保存
      await handleSave(isContinue);
    } catch (e: any) {
      console.error('[PoolDrawer] confirmHandler error:', e);
      const msg = e?.message || t('common.saveFailed');
      Message.error(msg);
    }
  }

  watch([() => props.row, () => visible.value], () => {
    if (props.row && visible.value) {
      const val = props.row;
      form.value = {
        id: val.id,
        name: val.name,
        enable: val.enable,
        description: val.description ?? '',
        auto: val.auto,
        pickMode: val.pickMode ?? 'VISIBLE_PICKABLE',
        newLeadRemind: val.newLeadRemind ?? false,
        unassignedReminderMinutes: val.unassignedReminderMinutes ?? 1440,
        unfollowedReminderMinutes: val.unfollowedReminderMinutes ?? 2880,
        notifyPoolAdminOnUnfollowedTimeout: val.notifyPoolAdminOnUnfollowedTimeout ?? false,
        allowTransferAfterPick: val.allowTransferAfterPick ?? false,
        restrictTransferInToMembers: val.restrictTransferInToMembers ?? false,
        restrictReturnToMembers: val.restrictReturnToMembers ?? false,
        clearTeamOnOwnerChange: val.clearTeamOnOwnerChange ?? false,
        clearExternalOwnerOnOwnerEmpty: val.clearExternalOwnerOnOwnerEmpty ?? false,
        clearExternalTeamOnExternalOwnerEmpty: val.clearExternalTeamOnExternalOwnerEmpty ?? false,
        clearOwnerOnPoolTransfer: val.clearOwnerOnPoolTransfer ?? false,
        clearExternalOwnerOnPoolTransfer: val.clearExternalOwnerOnPoolTransfer ?? false,
        allowViewChangeLogBeforePick: val.allowViewChangeLogBeforePick ?? false,
        allowEditTeamBeforePick: val.allowEditTeamBeforePick ?? false,
        allowSendSalesRecordBeforePick: val.allowSendSalesRecordBeforePick ?? false,
        allowViewSalesRecordBeforePick: val.allowViewSalesRecordBeforePick ?? false,
        allowViewPoolLog: val.allowViewPoolLog ?? false,
        pickRule: val.pickRule ?? cloneDeep(initForm).pickRule,
        recycleRule: val.recycleRule ?? cloneDeep(initForm).recycleRule,
        assignRules: val.assignRules ?? [],
        autoAssignEnabled: val.autoAssignEnabled ?? false,
        autoAssignCron: val.autoAssignCron ?? '',
        dedupStrategy: val.dedupStrategy ?? 'UPDATE',
        dedupWindow: val.dedupWindow ?? 5,
        dedupKey: val.dedupKey ?? 'AUTO',
        userIds: val.members,
        adminIds: val.owners,
        collaboratorIds: val.collaborators ?? [],
        hiddenFieldIds: val.fieldConfigs?.filter((item) => !item.enable).map((item) => item.fieldId) || [],
        updateTime: val.updateTime,
      };
      currentClueCount.value = val.currentClueCount ?? 0;
      if (val.auto) {
        recycleFormItemModel.value = {
          list: val.recycleRule.conditions?.map((item) => ({
            dataIndex: item.column,
            operator: item.operator,
            showScope: !!item.scope?.length,
            value: item.value,
            scope: item.scope,
            type: FieldTypeEnum.TIME_RANGE_PICKER,
          })) as FilterFormItem[],
          searchMode: val.recycleRule.operator as AccordBelowType,
        };
      } else {
        recycleFormItemModel.value = cloneDeep(defaultFormModel);
      }
    }
  });

  watch(
    () => visible.value,
    async (val) => {
      if (val) {
        tabName.value = 'baseInfo';
        await initFormConfig();
        showFieldIds.value = showInTableColumns.value
          .filter((item) => item.businessKey === 'name' || !form.value.hiddenFieldIds.includes(item.id))
          .map((item) => item.id);
      }
    }
  );
</script>

<style scoped lang="less">
  :deep(.dataIndex-col) {
    width: 100px;
    flex: initial;
  }
</style>
