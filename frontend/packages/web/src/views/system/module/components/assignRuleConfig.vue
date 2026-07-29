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
        <div v-for="(cond, cIdx) in rule.conditionList" :key="cIdx" class="mb-[8px] flex items-center gap-[8px]">
          <n-select
            v-model:value="cond.fieldId"
            :options="fieldOptions"
            :placeholder="t('module.clue.fieldName')"
            class="flex-1"
            size="small"
            filterable
          />
          <n-select
            v-model:value="cond.operator"
            :options="operatorOptions"
            :placeholder="t('module.clue.operator')"
            class="w-[120px]"
            size="small"
          />
          <n-input
            v-model:value="cond.value"
            :placeholder="t('module.clue.matchValue')"
            class="w-[200px]"
            size="small"
          />
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

      <!-- 目标人员 -->
      <div class="mt-[8px]">
        <CrmUserTagSelector v-model:selected-list="rule.targetUserNames" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NButton, NEmpty, NInput, NRadio, NRadioGroup, NSelect, NSpace, NSwitch, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
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

  // 字段选项:从线索表单配置获取
  const formKey = ref(FormDesignKeyEnum.CLUE);
  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey,
  });

  const fieldOptions = computed(() => {
    // 过滤出可匹配的字段类型
    const matchableTypes = [
      FieldTypeEnum.INPUT,
      FieldTypeEnum.DATA_SOURCE,
      FieldTypeEnum.SERIAL_NUMBER,
      FieldTypeEnum.INPUT_MULTIPLE,
      FieldTypeEnum.TEXTAREA,
    ];
    return fieldList.value
      .filter((f) => matchableTypes.includes(f.type) && f.businessKey !== 'owner' && f.businessKey !== 'name')
      .map((f) => ({ label: f.name, value: f.id }));
  });

  const operatorOptions = computed(() => [
    { label: t('module.clue.operatorEquals'), value: 'EQUALS' },
    { label: t('module.clue.operatorNotEquals'), value: 'NOT_EQUALS' },
    { label: t('module.clue.operatorContains'), value: 'CONTAINS' },
  ]);

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

  // 同步内部 -> props
  watch(
    rules,
    (val) => {
      if (!rulesEqual(val, props.modelValue)) {
        emit('update:modelValue', cloneRules(val));
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
      targetUserNames: [],
      enable: true,
      pos: rules.value.length + 1,
    });
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
    rules.value.splice(index + 1, 0, copy);
    rules.value.forEach((item, ruleIndex) => {
      item.pos = ruleIndex + 1;
    });
  }

  function addCondition(rule: CluePoolAssignRuleParams) {
    if (!rule.conditionList) {
      rule.conditionList = [];
    }
    rule.conditionList.push({ fieldId: '', operator: 'EQUALS', value: '' } as AssignRuleCondition);
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
