<template>
  <div class="assign-rule-config">
    <div class="mb-[8px] flex items-center justify-between">
      <span class="text-[13px] text-[var(--text-n5)]">{{ t('module.clue.assignRuleTip') }}</span>
      <n-space>
        <n-button size="small" :loading="triggering" :disabled="!form.id" @click="handleTrigger">
          {{ t('module.clue.triggerAssign') }}
        </n-button>
        <n-button size="small" type="primary" @click="addRule">
          {{ t('module.clue.addRule') }}
        </n-button>
      </n-space>
    </div>

    <n-empty v-if="rules.length === 0" :description="t('module.clue.noAssignRules')" class="my-[24px]" />

    <div v-for="(rule, index) in rules" :key="index" class="rule-card">
      <div class="mb-[12px] flex items-center gap-[12px]">
        <span class="shrink-0 text-[12px] font-medium">{{ t('module.clue.priority', { number: index + 1 }) }}</span>
        <n-input v-model:value="rule.ruleName" :placeholder="t('module.clue.ruleName')" class="flex-1" size="small" />
        <n-switch v-model:value="rule.enable" size="small" />
        <n-button size="tiny" quaternary :disabled="index === 0" @click="moveRule(index, -1)">
          {{ t('module.clue.moveUp') }}
        </n-button>
        <n-button size="tiny" quaternary :disabled="index === rules.length - 1" @click="moveRule(index, 1)">
          {{ t('module.clue.moveDown') }}
        </n-button>
        <n-button size="tiny" quaternary @click="duplicateRule(index)">
          {{ t('module.clue.copyRule') }}
        </n-button>
        <n-button size="small" quaternary type="error" @click="removeRule(index)">
          {{ t('common.button.delete') }}
        </n-button>
      </div>

      <!-- 匹配条件 -->
      <div class="mb-[12px]">
        <div class="mb-[4px] text-[12px] text-[var(--text-n5)]">{{ t('module.clue.matchCondition') }}</div>
        <div
          v-for="(cond, cIdx) in rule.conditionList"
          :key="cIdx"
          class="mb-[8px] flex flex-wrap items-center gap-[8px]"
        >
          <n-select v-model:value="cond.conditionType" :options="conditionTypeOptions" class="w-[92px]" size="small" />
          <!-- 内容字段条件 -->
          <template v-if="cond.conditionType !== 'TIME'">
            <n-select
              v-model:value="cond.fieldId"
              :options="fieldOptions"
              :placeholder="t('module.clue.fieldName')"
              class="w-[180px]"
              size="small"
              filterable
              @update:value="(v: string) => onFieldChange(cond, v)"
            />
            <n-select
              v-model:value="cond.operator"
              :options="operatorOptions"
              :placeholder="t('module.clue.operator')"
              class="w-[96px]"
              size="small"
            />
            <n-select
              v-if="isLocationCond(cond)"
              :value="
                cond.value
                  ? String(cond.value)
                      .split(/[,，;；|]/)
                      .filter(Boolean)
                  : []
              "
              :options="provinceOptions"
              :placeholder="t('module.clue.province')"
              multiple
              filterable
              clearable
              class="w-[220px]"
              size="small"
              @update:value="(v: string[]) => (cond.value = (v || []).join(','))"
            />
            <n-select
              v-else-if="currentFieldIsDataSource(cond)"
              :value="
                cond.value
                  ? String(cond.value)
                      .split(/[,，;；|]/)
                      .filter(Boolean)
                  : []
              "
              :options="currentFieldDataSourceOptions(cond)"
              :placeholder="t('module.clue.matchValue')"
              multiple
              filterable
              clearable
              class="w-[220px]"
              size="small"
              @update:value="(v: string[]) => (cond.value = (v || []).join(','))"
            />
            <n-input
              v-else
              v-model:value="cond.value"
              :placeholder="t('module.clue.matchValueMulti')"
              class="w-[220px]"
              size="small"
            />
          </template>
          <!-- 时间条件 -->
          <template v-else>
            <n-select
              v-model:value="cond.fieldId"
              :options="timeFieldOptions"
              :placeholder="t('module.clue.timeField')"
              class="w-[180px]"
              size="small"
              filterable
            />
            <n-select
              v-model:value="cond.operator"
              :options="timeOperatorOptions"
              :placeholder="t('module.clue.operator')"
              class="w-[96px]"
              size="small"
            />
            <n-date-picker
              v-if="cond.operator !== 'BETWEEN'"
              :value="cond.value ? Number(cond.value) : null"
              type="datetime"
              clearable
              size="small"
              class="w-[220px]"
              @update:value="(v: number | null) => (cond.value = v ? String(v) : '')"
            />
            <n-date-picker
              v-else
              v-model:value="cond.timeRange"
              type="datetimerange"
              clearable
              size="small"
              class="w-[280px]"
              @update:value="onTimeRange(cond, $event)"
            />
          </template>
          <n-button size="small" quaternary type="error" @click="removeCondition(rule, cIdx)"> × </n-button>
        </div>
        <n-button size="small" dashed @click="addCondition(rule)"> + {{ t('module.clue.addCondition') }} </n-button>
      </div>

      <!-- 分配方式 -->
      <div class="flex items-center gap-[12px]">
        <span class="text-[12px] text-[var(--text-n5)]">{{ t('module.clue.assignType') }}</span>
        <n-radio-group v-model:value="rule.assignType" size="small">
          <n-radio value="SINGLE">{{ t('module.clue.assignSingle') }}</n-radio>
          <n-radio value="ROUND_ROBIN">{{ t('module.clue.assignRoundRobin') }}</n-radio>
        </n-radio-group>
      </div>

      <!-- 目标类型 -->
      <div class="mt-[12px] flex items-center gap-[12px]">
        <span class="text-[12px] text-[var(--text-n5)]">{{ t('module.clue.assignTargetType') }}</span>
        <n-radio-group
          v-model:value="rule.assignTargetType"
          size="small"
          @update:value="onTargetTypeChange(rule, $event)"
        >
          <n-radio value="USER">{{ t('module.clue.assignTargetUser') }}</n-radio>
          <n-radio value="DEPT">{{ t('module.clue.assignTargetDept') }}</n-radio>
        </n-radio-group>
      </div>

      <!-- 目标: 指定人员 -->
      <div v-if="rule.assignTargetType !== 'DEPT'" class="mt-[8px]">
        <CrmUserTagSelector v-model:selected-list="rule.targetUserNames" />
      </div>

      <!-- 目标: 部门/区域成员 -->
      <div v-else class="mt-[8px]">
        <div class="mb-[8px] flex items-center gap-[12px]">
          <CrmUserTagSelector
            v-model:selected-list="rule.targetDeptNames"
            :member-types="deptMemberTypes"
            :disabled-node-types="deptDisabledNodeTypes"
            drawer-title="选择部门/区域"
            :placeholder="t('module.clue.selectDept')"
          />
          <n-checkbox v-model:checked="rule.includeChildDept">{{ t('module.clue.includeChildDept') }}</n-checkbox>
        </div>
        <p class="text-[12px] text-[var(--text-n5)]">{{ t('module.clue.assignTargetDeptTip') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import {
    NButton,
    NCheckbox,
    NEmpty,
    NInput,
    NRadio,
    NRadioGroup,
    NSelect,
    NSpace,
    NSwitch,
    useMessage,
  } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AssignRuleCondition,
    CluePoolAssignRuleParams,
    SelectedUsersItem,
  } from '@lib/shared/models/system/module';

  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { triggerCluePoolAssign } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    poolId?: string;
    modelValue: CluePoolAssignRuleParams[];
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', val: CluePoolAssignRuleParams[]): void;
  }>();

  const form = ref<{ id?: string }>({});
  const rules = ref<CluePoolAssignRuleParams[]>([]);
  const triggering = ref(false);

  // 部门选择配置:仅允许选择部门/区域节点
  const deptMemberTypes = [{ value: MemberSelectTypeEnum.ONLY_ORG, label: t('module.clue.department') }];
  const deptDisabledNodeTypes = [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE];

  // 字段选项:从线索表单配置获取
  const formKey = ref(FormDesignKeyEnum.CLUE);
  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey,
  });

  const fieldOptions = computed(() => {
    const matchableTypes = [
      FieldTypeEnum.INPUT,
      FieldTypeEnum.TEXTAREA,
      FieldTypeEnum.DATA_SOURCE,
      FieldTypeEnum.SELECT,
      FieldTypeEnum.INPUT_MULTIPLE,
      FieldTypeEnum.LOCATION,
      FieldTypeEnum.SERIAL_NUMBER,
    ];
    return fieldList.value
      .filter((f) => matchableTypes.includes(f.type) && f.businessKey !== 'owner' && f.businessKey !== 'name')
      .map((f) => ({ label: f.name, value: f.id, fieldType: f.type, dataSource: (f as any).dataSource }));
  });

  // 条件类型: 内容字段 / 时间
  const conditionTypeOptions = computed(() => [
    { label: t('module.clue.conditionTypeField'), value: 'FIELD' },
    { label: t('module.clue.conditionTypeTime'), value: 'TIME' },
  ]);

  const operatorOptions = computed(() => [
    { label: t('module.clue.operatorEquals'), value: 'EQUALS' },
    { label: t('module.clue.operatorNotEquals'), value: 'NOT_EQUALS' },
    { label: t('module.clue.operatorContains'), value: 'CONTAINS' },
  ]);

  const timeOperatorOptions = computed(() => [
    { label: t('module.clue.operatorBefore'), value: 'BEFORE' },
    { label: t('module.clue.operatorAfter'), value: 'AFTER' },
    { label: t('module.clue.operatorBetween'), value: 'BETWEEN' },
  ]);

  // 时间字段: 系统创建时间 + 线索表单中的日期类字段
  const timeFieldOptions = computed(() => {
    const dateTypes = [FieldTypeEnum.DATE_TIME, FieldTypeEnum.TIME_RANGE_PICKER];
    const formDateFields = fieldList.value
      .filter((f) => dateTypes.includes(f.type))
      .map((f) => ({ label: f.name, value: f.id }));
    return [{ label: t('module.clue.clueCreateTime'), value: 'CLUE_CREATE_TIME' }, ...formDateFields];
  });

  // 省份下拉(适用于地区/省份类字段)
  const LOCATION_KEYWORDS = ['省', '省份', '地区', '区域', '城市', '市'];
  function isLocationCond(cond: AssignRuleCondition): boolean {
    const field = fieldOptions.value.find((f) => f.value === cond.fieldId);
    if (!field) return false;
    return LOCATION_KEYWORDS.some((kw) => String(field.label).includes(kw));
  }

  function currentFieldIsDataSource(cond: AssignRuleCondition): boolean {
    const field = fieldOptions.value.find((f) => f.value === cond.fieldId);
    return !!field && field.fieldType === FieldTypeEnum.DATA_SOURCE && !!field.dataSource;
  }

  function currentFieldDataSourceOptions(cond: AssignRuleCondition) {
    const field = fieldOptions.value.find((f) => f.value === cond.fieldId);
    const ds = (field?.dataSource as any) || {};
    const options = ds.options || ds.dataSourceOptions || [];
    return (options as any[]).map((o) => ({ label: o.label ?? o.name ?? o.value, value: String(o.value ?? o.id) }));
  }

  // 中国省份下拉
  const PROVINCES = [
    '北京市',
    '天津市',
    '上海市',
    '重庆市',
    '河北省',
    '山西省',
    '辽宁省',
    '吉林省',
    '黑龙江省',
    '江苏省',
    '浙江省',
    '安徽省',
    '福建省',
    '江西省',
    '山东省',
    '河南省',
    '湖北省',
    '湖南省',
    '广东省',
    '海南省',
    '四川省',
    '贵州省',
    '云南省',
    '陕西省',
    '甘肃省',
    '青海省',
    '台湾省',
    '内蒙古自治区',
    '广西壮族自治区',
    '西藏自治区',
    '宁夏回族自治区',
    '新疆维吾尔自治区',
    '香港特别行政区',
    '澳门特别行政区',
  ];
  const provinceOptions = computed(() => PROVINCES.map((p) => ({ label: p, value: p })));

  function onFieldChange(cond: AssignRuleCondition, _value: string) {
    // 切换字段时清空已填值,避免类型不匹配
    cond.value = '';
    cond.value2 = '';
    cond.timeRange = null;
  }

  function onTargetTypeChange(rule: CluePoolAssignRuleParams, type: string) {
    if (type === 'DEPT') {
      rule.targetUserNames = [];
      if (!Array.isArray(rule.targetDeptNames)) rule.targetDeptNames = [];
    } else {
      rule.targetDeptNames = [];
      rule.includeChildDept = false;
      rule.targetDeptIds = [];
    }
  }

  function onTimeRange(cond: AssignRuleCondition, event: number[] | null) {
    if (!event || event.length < 2) {
      cond.value = '';
      cond.value2 = '';
      return;
    }
    cond.value = String(event[0]);
    cond.value2 = String(event[1]);
  }

  function rulesEqual(left?: CluePoolAssignRuleParams[], right?: CluePoolAssignRuleParams[]) {
    return JSON.stringify(left ?? []) === JSON.stringify(right ?? []);
  }

  function cloneRules(value?: CluePoolAssignRuleParams[]) {
    return JSON.parse(JSON.stringify(value ?? [])) as CluePoolAssignRuleParams[];
  }

  // 同步 props -> 内部
  watch(
    () => props.modelValue,
    (val) => {
      if (!rulesEqual(val, rules.value)) {
        rules.value = cloneRules(val);
      }
    },
    { immediate: true, deep: true }
  );

  // 同步内部 -> props (含 DEPT 目标字段转换)
  watch(
    rules,
    (val) => {
      if (!rulesEqual(val, props.modelValue)) {
        const cloned = cloneRules(val);
        cloned.forEach((rule) => {
          if (rule.assignTargetType === 'DEPT' && Array.isArray((rule as any).targetDeptNames)) {
            rule.targetDeptIds = ((rule as any).targetDeptNames as SelectedUsersItem[]).map((d) => d.id);
          }
        });
        emit('update:modelValue', cloned);
      }
    },
    { deep: true }
  );

  watch(
    () => props.poolId,
    (val) => {
      form.value.id = val;
    },
    { immediate: true }
  );

  async function loadFields() {
    await initFormConfig();
  }

  watch(
    () => props.poolId,
    async (val) => {
      if (val) {
        await loadFields();
      }
    },
    { immediate: true }
  );

  function addRule() {
    rules.value.push({
      ruleName: '',
      conditionList: [],
      assignType: 'SINGLE',
      assignTargetType: 'USER',
      targetUserNames: [],
      targetDeptNames: [],
      includeChildDept: false,
      enable: true,
      pos: rules.value.length + 1,
    } as CluePoolAssignRuleParams);
  }

  function removeRule(index: number) {
    rules.value.splice(index, 1);
    rules.value.forEach((rule, ruleIndex) => {
      rule.pos = ruleIndex + 1;
    });
  }

  function moveRule(index: number, offset: number) {
    const target = index + offset;
    if (target < 0 || target >= rules.value.length) return;
    const [rule] = rules.value.splice(index, 1);
    if (!rule) return;
    rules.value.splice(target, 0, rule);
    rules.value.forEach((item, ruleIndex) => {
      item.pos = ruleIndex + 1;
    });
  }

  function duplicateRule(index: number) {
    const source = rules.value[index];
    if (!source) return;
    const copy = cloneRules([source])[0];
    if (!copy) return;
    delete copy.id;
    copy.targetDeptNames = cloneRules([(source as any).targetDeptNames]).flat() as unknown as SelectedUsersItem[];
    rules.value.splice(index + 1, 0, copy);
    rules.value.forEach((item, ruleIndex) => {
      item.pos = ruleIndex + 1;
    });
  }

  function addCondition(rule: CluePoolAssignRuleParams) {
    if (!rule.conditionList) {
      rule.conditionList = [];
    }
    rule.conditionList.push({
      fieldId: '',
      operator: 'EQUALS',
      value: '',
      conditionType: 'FIELD',
    } as AssignRuleCondition);
  }

  function removeCondition(rule: CluePoolAssignRuleParams, index: number) {
    rule.conditionList?.splice(index, 1);
  }

  async function handleTrigger() {
    if (!props.poolId) return;
    triggering.value = true;
    try {
      const count = await triggerCluePoolAssign(props.poolId);
      Message.success(t('module.clue.triggerAssignSuccess', { count }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    } finally {
      triggering.value = false;
    }
  }
</script>

<style scoped lang="less">
  .assign-rule-config {
    .rule-card {
      margin-bottom: 12px;
      padding: 16px;
      border: 1px solid var(--divider-n8);
      border-radius: 8px;
      background: var(--color-fill-1);
    }
  }
</style>
